package app.openpdf.foss.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import app.openpdf.foss.core.data.db.BookmarksDao
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

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS bookmarks (
                    uri TEXT NOT NULL,
                    pageIndex INTEGER NOT NULL,
                    label TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    PRIMARY KEY(uri, pageIndex)
                )"""
            )
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OpenPdfDatabase =
        Room.databaseBuilder(context, OpenPdfDatabase::class.java, "openpdf.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideRecentFilesDao(db: OpenPdfDatabase): RecentFilesDao = db.recentFilesDao()

    @Provides
    fun provideBookmarksDao(db: OpenPdfDatabase): BookmarksDao = db.bookmarksDao()
}
