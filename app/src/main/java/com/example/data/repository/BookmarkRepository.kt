package com.example.data.repository

import com.example.data.dao.BookmarkDao
import com.example.data.entities.BookmarkEntity
import kotlinx.coroutines.flow.Flow

interface BookmarkRepository {
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>
    suspend fun getAllBookmarksSync(): List<BookmarkEntity>
    fun getBookmarkById(id: Long): Flow<BookmarkEntity?>
    suspend fun getBookmarkByIdSync(id: Long): BookmarkEntity?
    fun getBookmarksByFolder(folder: String): Flow<List<BookmarkEntity>>
    fun searchBookmarks(query: String): Flow<List<BookmarkEntity>>
    fun getAllFolders(): Flow<List<String>>
    suspend fun getMaxSortOrder(): Int
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long
    suspend fun updateBookmark(bookmark: BookmarkEntity)
    suspend fun updateBookmarks(bookmarks: List<BookmarkEntity>)
    suspend fun deleteBookmark(bookmark: BookmarkEntity)
    suspend fun deleteBookmarkById(id: Long)
    suspend fun swapBookmarksOrder(first: BookmarkEntity, second: BookmarkEntity)
}

class BookmarkRepositoryImpl(private val bookmarkDao: BookmarkDao) : BookmarkRepository {
    override fun getAllBookmarks(): Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    override suspend fun getAllBookmarksSync(): List<BookmarkEntity> = bookmarkDao.getAllBookmarksSync()

    override fun getBookmarkById(id: Long): Flow<BookmarkEntity?> = bookmarkDao.getBookmarkById(id)

    override suspend fun getBookmarkByIdSync(id: Long): BookmarkEntity? = bookmarkDao.getBookmarkByIdSync(id)

    override fun getBookmarksByFolder(folder: String): Flow<List<BookmarkEntity>> =
        bookmarkDao.getBookmarksByFolder(folder)

    override fun searchBookmarks(query: String): Flow<List<BookmarkEntity>> =
        bookmarkDao.searchBookmarks(query)

    override fun getAllFolders(): Flow<List<String>> = bookmarkDao.getAllFolders()

    override suspend fun getMaxSortOrder(): Int = bookmarkDao.getMaxSortOrder()

    override suspend fun insertBookmark(bookmark: BookmarkEntity): Long =
        bookmarkDao.insertBookmark(bookmark)

    override suspend fun updateBookmark(bookmark: BookmarkEntity) =
        bookmarkDao.updateBookmark(bookmark)

    override suspend fun updateBookmarks(bookmarks: List<BookmarkEntity>) =
        bookmarkDao.updateBookmarks(bookmarks)

    override suspend fun deleteBookmark(bookmark: BookmarkEntity) =
        bookmarkDao.deleteBookmark(bookmark)

    override suspend fun deleteBookmarkById(id: Long) =
        bookmarkDao.deleteBookmarkById(id)

    override suspend fun swapBookmarksOrder(first: BookmarkEntity, second: BookmarkEntity) {
        val updatedFirst = first.copy(sortOrder = second.sortOrder, updatedAt = System.currentTimeMillis())
        val updatedSecond = second.copy(sortOrder = first.sortOrder, updatedAt = System.currentTimeMillis())
        bookmarkDao.updateBookmarks(listOf(updatedFirst, updatedSecond))
    }
}
