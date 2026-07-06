package app.openpdf.foss.feature.viewer

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import app.openpdf.foss.core.pdf.PdfDocumentSession
import app.openpdf.foss.core.pdf.model.PageSize
import app.openpdf.foss.ui.theme.Spacing
import androidx.compose.foundation.gestures.detectTapGestures
import kotlin.math.roundToInt

private const val MIN_ZOOM = 1f
private const val MAX_ZOOM = 5f

/**
 * Continuous-scroll PDF viewport: one-finger drag scrolls, two-finger pinch
 * zooms, double-tap toggles 1x/2.5x. Pages re-render at the nearest half-step
 * of the current zoom so bitmaps stay sharp without re-rendering every frame.
 */
@Composable
fun PdfViewport(
    session: PdfDocumentSession,
    pageSizes: List<PageSize>,
    initialPage: Int,
    onPageChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    jumpToPage: Int? = null,
    onJumpHandled: () -> Unit = {},
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

        // Report the page closest to the viewport top.
        LaunchedEffect(listState) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .collect(onPageChanged)
        }

        LaunchedEffect(jumpToPage) {
            jumpToPage?.let { target ->
                listState.scrollToItem(target.coerceIn(0, pageSizes.size - 1))
                onJumpHandled()
            }
        }

        // Render width snaps to half-zoom steps to bound re-render churn.
        val renderZoom = (zoom * 2).roundToInt().coerceAtLeast(2) / 2f
        val renderWidthPx = (viewportWidthPx * renderZoom).roundToInt()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .transformable(transformState, lockRotationOnZoomPan = true)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { zoom = if (zoom > 1.5f) 1f else 2.5f },
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
                    )
                }
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
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, session, pageIndex, renderWidthPx) {
        value = runCatching { PageBitmapCache.getOrRender(session, pageIndex, renderWidthPx) }
            .getOrNull()
    }
    Box(
        modifier = Modifier
            .aspectRatio(if (aspectRatio > 0f) aspectRatio else 0.7071f)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}
