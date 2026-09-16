package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entities.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY sortOrder ASC, createdAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks ORDER BY sortOrder ASC, createdAt DESC")
    suspend fun getAllBookmarksSync(): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE id = :id")
    fun getBookmarkById(id: Long): Flow<BookmarkEntity?>

    @Query("SELECT * FROM bookmarks WHERE id = :id LIMIT 1")
    suspend fun getBookmarkByIdSync(id: Long): BookmarkEntity?

    @Query("SELECT * FROM bookmarks WHERE folder = :folder ORDER BY sortOrder ASC, createdAt DESC")
    fun getBookmarksByFolder(folder: String): Flow<List<BookmarkEntity>>

    @Query("""
        SELECT * FROM bookmarks 
        WHERE title LIKE '%' || :query || '%' 
           OR customTitle LIKE '%' || :query || '%' 
           OR url LIKE '%' || :query || '%' 
           OR domain LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%'
        ORDER BY sortOrder ASC, createdAt DESC
    """)
    fun searchBookmarks(query: String): Flow<List<BookmarkEntity>>

    @Query("SELECT DISTINCT folder FROM bookmarks WHERE folder IS NOT NULL AND folder != ''")
    fun getAllFolders(): Flow<List<String>>

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM bookmarks")
    suspend fun getMaxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Update
    suspend fun updateBookmark(bookmark: BookmarkEntity)

    @Update
    suspend fun updateBookmarks(bookmarks: List<BookmarkEntity>)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)
}
