package app.openpdf.foss.feature.viewer

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import app.openpdf.foss.core.pdf.PdfDocumentSession
import app.openpdf.foss.core.pdf.model.NormalizedRect
import app.openpdf.foss.core.pdf.model.PageSize
import app.openpdf.foss.ui.theme.Spacing
import kotlin.math.roundToInt

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 5f

private val NightColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f,
        )
    )
)

private val SepiaColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0.9f, 0f, 0f, 0f, 25f,
            0f, 0.82f, 0f, 0f, 18f,
            0f, 0f, 0.65f, 0f, 5f,
            0f, 0f, 0f, 1f, 0f,
        )
    )
)

/** Per-page overlays: search hits and the active text selection. */
data class PageOverlays(
    val searchRects: List<NormalizedRect> = emptyList(),
    val currentSearchRects: List<NormalizedRect> = emptyList(),
    val selectionRects: List<NormalizedRect> = emptyList(),
)

@Composable
fun PdfViewport(
    session: PdfDocumentSession,
    pageSizes: List<PageSize>,
    initialPage: Int,
    onPageChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    readingMode: ReadingMode = ReadingMode.NORMAL,
    viewMode: ViewMode = ViewMode.VERTICAL,
    overlaysForPage: (Int) -> PageOverlays = { PageOverlays() },
    onSelectText: (page: Int, sx: Float, sy: Float, ex: Float, ey: Float) -> Unit = { _, _, _, _, _ -> },
    onClearSelection: () -> Unit = {},
    jumpToPage: Int? = null,
    onJumpHandled: () -> Unit = {},
) {
    val colorFilter = when (readingMode) {
        ReadingMode.NORMAL -> null
        ReadingMode.NIGHT -> NightColorFilter
        ReadingMode.SEPIA -> SepiaColorFilter
    }

    when (viewMode) {
        ViewMode.VERTICAL -> VerticalViewport(
            session, pageSizes, initialPage, onPageChanged, modifier,
            colorFilter, overlaysForPage, onSelectText, onClearSelection,
            jumpToPage, onJumpHandled,
        )

        ViewMode.HORIZONTAL -> HorizontalViewport(
            session, pageSizes, initialPage, onPageChanged, modifier,
            colorFilter, overlaysForPage, onSelectText, onClearSelection,
            jumpToPage, onJumpHandled,
        )
    }
}

@Composable
private fun VerticalViewport(
    session: PdfDocumentSession,
    pageSizes: List<PageSize>,
    initialPage: Int,
    onPageChanged: (Int) -> Unit,
    modifier: Modifier,
    colorFilter: ColorFilter?,
    overlaysForPage: (Int) -> PageOverlays,
    onSelectText: (Int, Float, Float, Float, Float) -> Unit,
    onClearSelection: () -> Unit,
    jumpToPage: Int?,
    onJumpHandled: () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewportWidthPx = constraints.maxWidth
        val viewportWidthDp = maxWidth
        var zoom by remember { mutableFloatStateOf(1f) }
        val transformState = rememberTransformableState { zoomChange, _, _ ->
            zoom = (zoom * zoomChange).coerceIn(MIN_ZOOM, MAX_ZOOM)
        }
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPage)
        val hScroll = rememberScrollState()

        LaunchedEffect(listState) {
            snapshotFlow { listState.firstVisibleItemIndex }.collect(onPageChanged)
        }
        LaunchedEffect(jumpToPage) {
            jumpToPage?.let { target ->
                listState.scrollToItem(target.coerceIn(0, pageSizes.size - 1))
                onJumpHandled()
            }
        }

        val renderZoom = (zoom * 2).roundToInt().coerceAtLeast(2) / 2f
        val renderWidthPx = (viewportWidthPx * renderZoom).roundToInt()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .transformable(transformState, lockRotationOnZoomPan = true)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { zoom = if (zoom > 1.5f) 1f else 2.5f },
                        onTap = { onClearSelection() },
                    )
                }
                .horizontalScroll(hScroll),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.width(viewportWidthDp * zoom),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                items(count = pageSizes.size, key = { it }) { index ->
                    PdfPageItem(
                        session = session,
                        pageIndex = index,
                        aspectRatio = pageSizes[index].aspectRatio,
                        renderWidthPx = renderWidthPx,
                        colorFilter = colorFilter,
                        overlays = overlaysForPage(index),
                        onSelectText = onSelectText,
                    )
                }
            }
        }
    }
}

@Composable
private fun HorizontalViewport(
    session: PdfDocumentSession,
    pageSizes: List<PageSize>,
    initialPage: Int,
    onPageChanged: (Int) -> Unit,
    modifier: Modifier,
    colorFilter: ColorFilter?,
    overlaysForPage: (Int) -> PageOverlays,
    onSelectText: (Int, Float, Float, Float, Float) -> Unit,
    onClearSelection: () -> Unit,
    jumpToPage: Int?,
    onJumpHandled: () -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val viewportWidthPx = constraints.maxWidth
        val pagerState = rememberPagerState(initialPage = initialPage) { pageSizes.size }

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }.collect(onPageChanged)
        }
        LaunchedEffect(jumpToPage) {
            jumpToPage?.let { target ->
                pagerState.scrollToPage(target.coerceIn(0, pageSizes.size - 1))
                onJumpHandled()
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onTap = { onClearSelection() }) },
        ) { index ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                PdfPageItem(
                    session = session,
                    pageIndex = index,
                    aspectRatio = pageSizes[index].aspectRatio,
                    renderWidthPx = viewportWidthPx,
                    colorFilter = colorFilter,
                    overlays = overlaysForPage(index),
                    onSelectText = onSelectText,
                )
            }
        }
    }
}

@Composable
private fun PdfPageItem(
    session: PdfDocumentSession,
    pageIndex: Int,
    aspectRatio: Float,
    renderWidthPx: Int,
    colorFilter: ColorFilter?,
    overlays: PageOverlays,
    onSelectText: (Int, Float, Float, Float, Float) -> Unit,
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, session, pageIndex, renderWidthPx) {
        value = runCatching { PageBitmapCache.getOrRender(session, pageIndex, renderWidthPx) }
            .getOrNull()
    }
    Box(
        modifier = Modifier
            .aspectRatio(if (aspectRatio > 0f) aspectRatio else 0.7071f)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .pointerInput(pageIndex) {
                var start: Offset? = null
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset -> start = offset },
                    onDrag = { change, _ ->
                        val s = start ?: return@detectDragGesturesAfterLongPress
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        onSelectText(
                            pageIndex,
                            (s.x / w).coerceIn(0f, 1f),
                            (s.y / h).coerceIn(0f, 1f),
                            (change.position.x / w).coerceIn(0f, 1f),
                            (change.position.y / h).coerceIn(0f, 1f),
                        )
                    },
                )
            },
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter,
            )
        }
        if (overlays.searchRects.isNotEmpty() ||
            overlays.currentSearchRects.isNotEmpty() ||
            overlays.selectionRects.isNotEmpty()
        ) {
            OverlayCanvas(overlays)
        }
    }
}

@Composable
private fun OverlayCanvas(overlays: PageOverlays) {
    val hitColor = Color(0x66FFC107)
    val currentColor = Color(0x99FF9800)
    val selectionColor = Color(0x552C3E50)
    Canvas(modifier = Modifier.fillMaxSize()) {
        fun drawRects(rects: List<NormalizedRect>, color: Color) {
            rects.forEach { r ->
                drawRect(
                    color = color,
                    topLeft = Offset(r.left * size.width, r.top * size.height),
                    size = Size(
                        (r.right - r.left) * size.width,
                        (r.bottom - r.top) * size.height,
                    ),
                )
            }
        }
        drawRects(overlays.searchRects, hitColor)
        drawRects(overlays.currentSearchRects, currentColor)
        drawRects(overlays.selectionRects, selectionColor)
    }
}
