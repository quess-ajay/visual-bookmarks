package com.example

import android.app.Application
import com.example.data.database.AppDatabase
import com.example.data.repository.BookmarkRepository
import com.example.data.repository.BookmarkRepositoryImpl
import com.example.screenshot.ScreenshotGenerator
import com.example.screenshot.ScreenshotManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VisualBookmarksApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: BookmarkRepository
        private set

    lateinit var screenshotGenerator: ScreenshotGenerator
        private set

    lateinit var screenshotManager: ScreenshotManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        repository = BookmarkRepositoryImpl(database.bookmarkDao())
        screenshotGenerator = ScreenshotGenerator(this)
        screenshotManager = ScreenshotManager(this, repository, screenshotGenerator)

        // Seed initial popular demo bookmarks if database is empty on first launch
        CoroutineScope(Dispatchers.IO).launch {
            val existing = repository.getAllBookmarksSync()
            if (existing.isEmpty()) {
                seedInitialBookmarks()
            }
        }
    }

    private suspend fun seedInitialBookmarks() {
        val seeds = listOf(
            Triple("https://github.com", "GitHub", "Dev"),
            Triple("https://figma.com", "Figma", "Design"),
            Triple("https://en.wikipedia.org", "Wikipedia", "Reading"),
            Triple("https://developer.android.com", "Android Developers", "Dev")
        )

        for ((url, title, folder) in seeds) {
            try {
                val domain = ScreenshotGenerator.extractDomain(url)
                val snapshotPath = screenshotGenerator.generateFallbackSnapshot(domain, url)
                screenshotManager.saveNewBookmark(
                    url = url,
                    title = title,
                    folder = folder,
                    screenshotPath = snapshotPath
                )
            } catch (ignored: Exception) {}
        }
    }
}
