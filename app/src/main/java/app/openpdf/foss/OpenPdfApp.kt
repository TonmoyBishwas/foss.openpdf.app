package app.openpdf.foss

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OpenPdfApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // PdfBox needs its font/resource assets initialized before any use.
        PDFBoxResourceLoader.init(this)
    }
}
