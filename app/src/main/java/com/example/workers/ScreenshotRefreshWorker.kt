package com.example.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.database.AppDatabase
import com.example.data.repository.BookmarkRepositoryImpl
import com.example.screenshot.ScreenshotGenerator
import com.example.screenshot.ScreenshotManager

class ScreenshotRefreshWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = BookmarkRepositoryImpl(database.bookmarkDao())
        val generator = ScreenshotGenerator(applicationContext)
        val manager = ScreenshotManager(applicationContext, repository, generator)

        val bookmarkId = inputData.getLong("BOOKMARK_ID", -1L)

        return try {
            if (bookmarkId != -1L) {
                manager.refreshScreenshot(bookmarkId)
            } else {
                val allBookmarks = repository.getAllBookmarksSync()
                for (bookmark in allBookmarks) {
                    manager.refreshScreenshot(bookmark.id)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
