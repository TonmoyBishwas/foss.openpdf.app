package app.openpdf.foss.feature.viewer

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import app.openpdf.foss.core.pdf.PdfDocumentSession
import app.openpdf.foss.core.pdf.model.InkStroke
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

/** Per-page overlays: search hits, selection, buffered ink, eraser outlines. */
data class PageOverlays(
    val searchRects: List<NormalizedRect> = emptyList(),
    val currentSearchRects: List<NormalizedRect> = emptyList(),
    val selectionRects: List<NormalizedRect> = emptyList(),
    val inkStrokes: List<app.openpdf.foss.core.pdf.model.InkStroke> = emptyList(),
    val inkColor: Long = 0xFF2C3E50L,
    val annotationOutlines: List<NormalizedRect> = emptyList(),
    val formFieldRects: List<NormalizedRect> = emptyList(),
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
    docVersion: Int = 0,
    annotationTool: AnnotationTool = AnnotationTool.NONE,
    overlaysForPage: (Int) -> PageOverlays = { PageOverlays() },
    onSelectText: (page: Int, sx: Float, sy: Float, ex: Float, ey: Float) -> Unit = { _, _, _, _, _ -> },
    onClearSelection: () -> Unit = {},
    onPageTap: (page: Int, x: Float, y: Float) -> Unit = { _, _, _ -> },
    onInkStroke: (page: Int, stroke: app.openpdf.foss.core.pdf.model.InkStroke) -> Unit = { _, _ -> },
    onShapeDrawn: (page: Int, rect: NormalizedRect) -> Unit = { _, _ -> },
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
            colorFilter, docVersion, annotationTool, overlaysForPage,
            onSelectText, onClearSelection, onPageTap, onInkStroke, onShapeDrawn,
            jumpToPage, onJumpHandled,
        )

        ViewMode.HORIZONTAL -> HorizontalViewport(
            session, pageSizes, initialPage, onPageChanged, modifier,
            colorFilter, docVersion, annotationTool, overlaysForPage,
            onSelectText, onClearSelection, onPageTap, onInkStroke, onShapeDrawn,
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
    docVersion: Int,
    annotationTool: AnnotationTool,
    overlaysForPage: (Int) -> PageOverlays,
    onSelectText: (Int, Float, Float, Float, Float) -> Unit,
    onClearSelection: () -> Unit,
    onPageTap: (Int, Float, Float) -> Unit,
    onInkStroke: (Int, app.openpdf.foss.core.pdf.model.InkStroke) -> Unit,
    onShapeDrawn: (Int, NormalizedRect) -> Unit,
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
                        docVersion = docVersion,
                        annotationTool = annotationTool,
                        overlays = overlaysForPage(index),
                        onSelectText = onSelectText,
                        onPageTap = onPageTap,
                        onInkStroke = onInkStroke,
                        onShapeDrawn = onShapeDrawn,
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
    docVersion: Int,
    annotationTool: AnnotationTool,
    overlaysForPage: (Int) -> PageOverlays,
    onSelectText: (Int, Float, Float, Float, Float) -> Unit,
    onClearSelection: () -> Unit,
    onPageTap: (Int, Float, Float) -> Unit,
    onInkStroke: (Int, app.openpdf.foss.core.pdf.model.InkStroke) -> Unit,
    onShapeDrawn: (Int, NormalizedRect) -> Unit,
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
                    docVersion = docVersion,
                    annotationTool = annotationTool,
                    overlays = overlaysForPage(index),
                    onSelectText = onSelectText,
                    onPageTap = onPageTap,
                    onInkStroke = onInkStroke,
                    onShapeDrawn = onShapeDrawn,
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
    docVersion: Int,
    annotationTool: AnnotationTool,
    overlays: PageOverlays,
    onSelectText: (Int, Float, Float, Float, Float) -> Unit,
    onPageTap: (Int, Float, Float) -> Unit,
    onInkStroke: (Int, InkStroke) -> Unit,
    onShapeDrawn: (Int, NormalizedRect) -> Unit,
) {
    val bitmap by produceState<Bitmap?>(
        initialValue = null, session, pageIndex, renderWidthPx, docVersion,
    ) {
        value = runCatching { PageBitmapCache.getOrRender(session, pageIndex, renderWidthPx) }
            .getOrNull()
    }
    var liveStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }

    var baseModifier = Modifier
        .aspectRatio(if (aspectRatio > 0f) aspectRatio else 0.7071f)
        .background(MaterialTheme.colorScheme.surfaceContainerLowest)

    baseModifier = when (annotationTool) {
        AnnotationTool.INK -> baseModifier.pointerInput(pageIndex, annotationTool) {
            detectDragGestures(
                onDragStart = { offset -> liveStroke = listOf(offset) },
                onDrag = { change, _ -> liveStroke = liveStroke + change.position },
                onDragEnd = {
                    if (liveStroke.size > 1) {
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        onInkStroke(
                            pageIndex,
                            InkStroke(
                                liveStroke.map {
                                    (it.x / w).coerceIn(0f, 1f) to (it.y / h).coerceIn(0f, 1f)
                                }
                            ),
                        )
                    }
                    liveStroke = emptyList()
                },
                onDragCancel = { liveStroke = emptyList() },
            )
        }

        AnnotationTool.SHAPE -> baseModifier.pointerInput(pageIndex, annotationTool) {
            var start: Offset? = null
            detectDragGestures(
                onDragStart = { offset ->
                    start = offset
                    liveStroke = listOf(offset)
                },
                onDrag = { change, _ ->
                    start?.let { liveStroke = listOf(it, change.position) }
                },
                onDragEnd = {
                    val s = start
                    val e = liveStroke.lastOrNull()
                    if (s != null && e != null && s != e) {
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        onShapeDrawn(
                            pageIndex,
                            NormalizedRect(
                                left = (minOf(s.x, e.x) / w).coerceIn(0f, 1f),
                                top = (minOf(s.y, e.y) / h).coerceIn(0f, 1f),
                                right = (maxOf(s.x, e.x) / w).coerceIn(0f, 1f),
                                bottom = (maxOf(s.y, e.y) / h).coerceIn(0f, 1f),
                            ),
                        )
                    }
                    start = null
                    liveStroke = emptyList()
                },
                onDragCancel = { start = null; liveStroke = emptyList() },
            )
        }

        AnnotationTool.NOTE, AnnotationTool.TEXT, AnnotationTool.SIGN, AnnotationTool.ERASE ->
            baseModifier.pointerInput(pageIndex, annotationTool) {
                detectTapGestures { offset ->
                    onPageTap(
                        pageIndex,
                        offset.x / size.width.toFloat(),
                        offset.y / size.height.toFloat(),
                    )
                }
            }

        AnnotationTool.NONE -> baseModifier
            .pointerInput(pageIndex, annotationTool) {
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
            }
            .pointerInput("form-tap", pageIndex) {
                detectTapGestures { offset ->
                    onPageTap(
                        pageIndex,
                        offset.x / size.width.toFloat(),
                        offset.y / size.height.toFloat(),
                    )
                }
            }
    }

    Box(modifier = baseModifier) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter,
            )
        }
        OverlayCanvas(overlays, liveStroke)
    }
}

@Composable
private fun OverlayCanvas(overlays: PageOverlays, liveStroke: List<Offset>) {
    val hitColor = Color(0x66FFC107)
    val currentColor = Color(0x99FF9800)
    val selectionColor = Color(0x552C3E50)
    val outlineColor = Color(0xFFBA1A1A)
    val inkColor = Color(overlays.inkColor.toInt()).copy(alpha = 1f)
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

        // Interactive form fields get a light hint fill.
        overlays.formFieldRects.forEach { r ->
            drawRect(
                color = Color(0x2A2C6BD6),
                topLeft = Offset(r.left * size.width, r.top * size.height),
                size = Size(
                    (r.right - r.left) * size.width,
                    (r.bottom - r.top) * size.height,
                ),
            )
        }

        // Eraser mode: outline existing annotations so they can be targeted.
        overlays.annotationOutlines.forEach { r ->
            drawRect(
                color = outlineColor,
                topLeft = Offset(r.left * size.width, r.top * size.height),
                size = Size(
                    (r.right - r.left) * size.width,
                    (r.bottom - r.top) * size.height,
                ),
                style = Stroke(width = 3f),
            )
        }

        val strokeStyle = Stroke(width = size.width * 0.004f, cap = StrokeCap.Round)
        fun drawStrokePath(points: List<Offset>) {
            if (points.size < 2) return
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, color = inkColor, style = strokeStyle)
        }
        // Buffered (uncommitted) strokes in normalized coords.
        overlays.inkStrokes.forEach { stroke ->
            drawStrokePath(stroke.points.map { Offset(it.first * size.width, it.second * size.height) })
        }
        // Stroke currently being drawn, in pixel coords.
        drawStrokePath(liveStroke)
    }
}
