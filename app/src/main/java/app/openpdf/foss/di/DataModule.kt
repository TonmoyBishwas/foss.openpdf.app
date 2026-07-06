package app.openpdf.foss.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import app.openpdf.foss.core.data.db.OpenPdfDatabase
import app.openpdf.foss.core.data.db.RecentFilesDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.settingsDataStore

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OpenPdfDatabase =
        Room.databaseBuilder(context, OpenPdfDatabase::class.java, "openpdf.db").build()

    @Provides
    fun provideRecentFilesDao(db: OpenPdfDatabase): RecentFilesDao = db.recentFilesDao()
}
