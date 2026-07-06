package app.openpdf.foss.di

import app.openpdf.foss.core.pdf.PdfEngine
import app.openpdf.foss.core.pdf.mupdf.MuPdfEngine
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PdfModule {

    @Binds
    @Singleton
    abstract fun bindPdfEngine(impl: MuPdfEngine): PdfEngine
}
