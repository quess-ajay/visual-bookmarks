package com.example.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val title: String,
    val customTitle: String? = null,
    val screenshotPath: String? = null,
    val faviconPath: String? = null,
    val folder: String = "General",
    val description: String? = null,
    val domain: String = "",
    val columnSpan: Int = 1, // 1 (half width) or 2 (full width spanning 2 columns)
    val rowSpan: Int = 1,    // 1 (standard height), 2 (tall / 2 rows), or 3 (extra tall / 3 rows)
    val sortOrder: Int = 0,  // Reordering position index in the grid
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
) {
    val displayTitle: String
        get() = if (!customTitle.isNullOrBlank()) customTitle else if (title.isNotBlank()) title else domain.ifBlank { url }
}
