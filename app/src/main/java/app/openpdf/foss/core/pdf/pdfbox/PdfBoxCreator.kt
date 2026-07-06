package app.openpdf.foss.core.pdf.pdfbox

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import androidx.exifinterface.media.ExifInterface
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PDF creation via PdfBox-Android (Apache-2.0): images/scans to PDF, text to
 * PDF, blank documents. Pure file-to-file transforms — never touches a live
 * MuPDF session.
 */
@Singleton
class PdfBoxCreator @Inject constructor() {

    /**
     * One page per image, page sized to the (EXIF-corrected) image aspect at
     * A4 width. [blackAndWhite] applies a scanner-style desaturate+contrast.
     */
    suspend fun createFromImages(
        imagePaths: List<String>,
        outPath: String,
        blackAndWhite: Boolean = false,
    ): Unit = withContext(Dispatchers.IO) {
        require(imagePaths.isNotEmpty()) { "No images" }
        PDDocument().use { doc ->
            imagePaths.forEach { path ->
                val bitmap = loadCorrected(path, blackAndWhite)
                try {
                    val pageWidth = PDRectangle.A4.width
                    val pageHeight = pageWidth * bitmap.height / bitmap.width
                    val page = PDPage(PDRectangle(pageWidth, pageHeight))
                    doc.addPage(page)
                    val image = JPEGFactory.createFromImage(doc, bitmap, 0.9f)
                    PDPageContentStream(doc, page).use { content ->
                        content.drawImage(image, 0f, 0f, pageWidth, pageHeight)
                    }
                } finally {
                    bitmap.recycle()
                }
            }
            doc.save(outPath)
        }
    }

    suspend fun createFromText(text: String, outPath: String): Unit =
        withContext(Dispatchers.IO) {
            PDDocument().use { doc ->
                val font = PDType1Font.HELVETICA
                val fontSize = 12f
                val leading = fontSize * 1.5f
                val margin = 54f
                val pageSize = PDRectangle.A4
                val maxWidth = pageSize.width - 2 * margin

                val lines = wrapText(text, font, fontSize, maxWidth)
                val linesPerPage = ((pageSize.height - 2 * margin) / leading).toInt()

                lines.chunked(linesPerPage.coerceAtLeast(1)).forEach { pageLines ->
                    val page = PDPage(pageSize)
                    doc.addPage(page)
                    PDPageContentStream(doc, page).use { content ->
                        content.beginText()
                        content.setFont(font, fontSize)
                        content.newLineAtOffset(margin, pageSize.height - margin - fontSize)
                        pageLines.forEach { line ->
                            content.showText(line)
                            content.newLineAtOffset(0f, -leading)
                        }
                        content.endText()
                    }
                }
                doc.save(outPath)
            }
        }

    suspend fun createBlank(pageCount: Int, outPath: String): Unit =
        withContext(Dispatchers.IO) {
            PDDocument().use { doc ->
                repeat(pageCount.coerceIn(1, 100)) { doc.addPage(PDPage(PDRectangle.A4)) }
                doc.save(outPath)
            }
        }

    private fun wrapText(
        text: String,
        font: PDType1Font,
        fontSize: Float,
        maxWidth: Float,
    ): List<String> {
        val result = mutableListOf<String>()
        text.split('\n').forEach { paragraph ->
            // PdfBox WinAnsi encoding can't render some chars; replace them.
            val safe = paragraph.map { ch ->
                runCatching { font.getStringWidth(ch.toString()) }
                    .map { ch }.getOrDefault('?')
            }.joinToString("")
            if (safe.isBlank()) {
                result.add("")
                return@forEach
            }
            var line = StringBuilder()
            safe.split(' ').forEach { word ->
                val candidate = if (line.isEmpty()) word else "$line $word"
                val width = font.getStringWidth(candidate) / 1000 * fontSize
                if (width > maxWidth && line.isNotEmpty()) {
                    result.add(line.toString())
                    line = StringBuilder(word)
                } else {
                    line = StringBuilder(candidate)
                }
            }
            result.add(line.toString())
        }
        return result
    }

    private fun loadCorrected(path: String, blackAndWhite: Boolean): Bitmap {
        val options = BitmapFactory.Options().apply {
            // Cap decode size to keep memory bounded on large camera images.
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)
        val maxDim = 2400
        var sample = 1
        while (options.outWidth / sample > maxDim || options.outHeight / sample > maxDim) {
            sample *= 2
        }
        val decoded = BitmapFactory.decodeFile(
            path,
            BitmapFactory.Options().apply { inSampleSize = sample },
        ) ?: error("Cannot decode image: $path")

        val rotationDegrees = when (
            ExifInterface(path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        var bitmap = decoded
        if (rotationDegrees != 0f) {
            val matrix = Matrix().apply { postRotate(rotationDegrees) }
            val rotated = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true,
            )
            if (rotated != bitmap) bitmap.recycle()
            bitmap = rotated
        }
        if (blackAndWhite) {
            val bw = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bw)
            val paint = Paint().apply {
                // Desaturate then push contrast for a scanned-document look.
                val desaturate = ColorMatrix().apply { setSaturation(0f) }
                val contrast = 1.4f
                val offset = (-0.5f * contrast + 0.5f) * 255f
                desaturate.postConcat(
                    ColorMatrix(
                        floatArrayOf(
                            contrast, 0f, 0f, 0f, offset,
                            0f, contrast, 0f, 0f, offset,
                            0f, 0f, contrast, 0f, offset,
                            0f, 0f, 0f, 1f, 0f,
                        )
                    )
                )
                colorFilter = ColorMatrixColorFilter(desaturate)
            }
            canvas.drawBitmap(bitmap, 0f, 0f, paint)
            bitmap.recycle()
            bitmap = bw
        }
        // JPEGFactory needs ARGB_8888.
        if (bitmap.config != Bitmap.Config.ARGB_8888) {
            val converted = bitmap.copy(Bitmap.Config.ARGB_8888, false)
            bitmap.recycle()
            bitmap = converted
        }
        return bitmap
    }
}
