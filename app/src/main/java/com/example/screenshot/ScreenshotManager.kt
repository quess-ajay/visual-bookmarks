package com.example.screenshot

import android.content.Context
import com.example.data.entities.BookmarkEntity
import com.example.data.repository.BookmarkRepository
import com.example.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ScreenshotManager(
    private val context: Context,
    private val repository: BookmarkRepository,
    private val generator: ScreenshotGenerator
) {

    suspend fun generatePreviewForUrl(url: String): ScreenshotCaptureResult {
        return generator.captureWebpage(url)
    }

    suspend fun saveNewBookmark(
        url: String,
        title: String,
        folder: String = "General",
        description: String? = null,
        screenshotPath: String? = null,
        faviconPath: String? = null,
        columnSpan: Int = 1,
        rowSpan: Int = 1
    ): Long = withContext(Dispatchers.IO) {
        val normalizedUrl = ScreenshotGenerator.normalizeUrl(url)
        val domain = ScreenshotGenerator.extractDomain(normalizedUrl)

        var finalScreenshotPath = screenshotPath
        var finalFaviconPath = faviconPath
        var finalTitle = title

        if (finalScreenshotPath == null) {
            val captureResult = generator.captureWebpage(normalizedUrl)
            finalScreenshotPath = captureResult.screenshotPath
            finalFaviconPath = captureResult.faviconPath
            if (finalTitle.isBlank()) {
                finalTitle = captureResult.title
            }
        }

        if (finalTitle.isBlank()) {
            finalTitle = domain.ifBlank { normalizedUrl }
        }

        val nextSortOrder = repository.getMaxSortOrder() + 1

        val bookmark = BookmarkEntity(
            url = normalizedUrl,
            title = finalTitle,
            customTitle = null,
            screenshotPath = finalScreenshotPath,
            faviconPath = finalFaviconPath,
            folder = folder.ifBlank { "General" },
            description = description,
            domain = domain,
            columnSpan = columnSpan.coerceIn(1, 2),
            rowSpan = rowSpan.coerceIn(1, 3),
            sortOrder = nextSortOrder,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val id = repository.insertBookmark(bookmark)

        // Trigger widget update
        WidgetUpdater.updateAllWidgets(context)

        id
    }

    suspend fun refreshScreenshot(bookmarkId: Long): Boolean = withContext(Dispatchers.IO) {
        val bookmark = repository.getBookmarkByIdSync(bookmarkId) ?: return@withContext false
        val captureResult = generator.captureWebpage(bookmark.url)

        if (captureResult.screenshotPath != null) {
            // Delete old screenshot file if different
            bookmark.screenshotPath?.let { oldPath ->
                if (oldPath != captureResult.screenshotPath) {
                    try {
                        File(oldPath).delete()
                    } catch (ignored: Exception) {}
                }
            }

            val updated = bookmark.copy(
                screenshotPath = captureResult.screenshotPath,
                faviconPath = captureResult.faviconPath ?: bookmark.faviconPath,
                title = if (bookmark.title.isBlank() || bookmark.title == bookmark.domain) captureResult.title else bookmark.title,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateBookmark(updated)
            WidgetUpdater.updateAllWidgets(context)
            true
        } else {
            false
        }
    }

    suspend fun deleteBookmark(bookmark: BookmarkEntity) = withContext(Dispatchers.IO) {
        bookmark.screenshotPath?.let { path ->
            try {
                File(path).delete()
            } catch (ignored: Exception) {}
        }
        bookmark.faviconPath?.let { path ->
            try {
                File(path).delete()
            } catch (ignored: Exception) {}
        }
        repository.deleteBookmark(bookmark)
        WidgetUpdater.updateAllWidgets(context)
    }

    suspend fun updateBookmarkDetails(
        id: Long,
        newTitle: String,
        newFolder: String,
        newDescription: String?,
        columnSpan: Int? = null,
        rowSpan: Int? = null
    ) = withContext(Dispatchers.IO) {
        val bookmark = repository.getBookmarkByIdSync(id) ?: return@withContext
        val updated = bookmark.copy(
            customTitle = newTitle.trim(),
            folder = newFolder.trim().ifBlank { "General" },
            description = newDescription?.trim(),
            columnSpan = (columnSpan ?: bookmark.columnSpan).coerceIn(1, 2),
            rowSpan = (rowSpan ?: bookmark.rowSpan).coerceIn(1, 3),
            updatedAt = System.currentTimeMillis()
        )
        repository.updateBookmark(updated)
        WidgetUpdater.updateAllWidgets(context)
    }

    suspend fun updateBookmarkLayout(
        id: Long,
        columnSpan: Int,
        rowSpan: Int
    ) = withContext(Dispatchers.IO) {
        val bookmark = repository.getBookmarkByIdSync(id) ?: return@withContext
        val updated = bookmark.copy(
            columnSpan = columnSpan.coerceIn(1, 2),
            rowSpan = rowSpan.coerceIn(1, 3),
            updatedAt = System.currentTimeMillis()
        )
        repository.updateBookmark(updated)
        WidgetUpdater.updateAllWidgets(context)
    }

    suspend fun swapBookmarks(first: BookmarkEntity, second: BookmarkEntity) = withContext(Dispatchers.IO) {
        repository.swapBookmarksOrder(first, second)
        WidgetUpdater.updateAllWidgets(context)
    }

    suspend fun reorderBookmarks(reorderedList: List<BookmarkEntity>) = withContext(Dispatchers.IO) {
        val updated = reorderedList.mapIndexed { index, bookmark ->
            bookmark.copy(sortOrder = index, updatedAt = System.currentTimeMillis())
        }
        repository.updateBookmarks(updated)
        WidgetUpdater.updateAllWidgets(context)
    }
}
