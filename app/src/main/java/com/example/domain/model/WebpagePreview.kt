package com.example.domain.model

import android.graphics.Bitmap
import com.example.data.entities.BookmarkEntity

data class WebpagePreview(
    val url: String,
    val title: String,
    val domain: String,
    val description: String? = null,
    val screenshotBitmap: Bitmap? = null,
    val screenshotPath: String? = null,
    val faviconBitmap: Bitmap? = null,
    val faviconPath: String? = null,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)

fun BookmarkEntity.toDomainPreview(): WebpagePreview {
    return WebpagePreview(
        url = url,
        title = displayTitle,
        domain = domain,
        description = description,
        screenshotPath = screenshotPath,
        faviconPath = faviconPath,
        isSuccess = true
    )
}
