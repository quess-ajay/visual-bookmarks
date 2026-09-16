package com.example.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.VisualBookmarksApp
import com.example.data.entities.BookmarkEntity
import com.example.data.repository.BookmarkRepository
import com.example.screenshot.ScreenshotManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val folders: List<String> = emptyList(),
    val selectedFolder: String? = null,
    val searchQuery: String = "",
    val refreshingBookmarkIds: Set<Long> = emptySet(),
    val isInitialLoading: Boolean = false
)

class HomeViewModel(
    application: Application,
    private val repository: BookmarkRepository,
    val screenshotManager: ScreenshotManager
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder = _selectedFolder.asStateFlow()

    private val _refreshingIds = MutableStateFlow<Set<Long>>(emptySet())
    val refreshingIds = _refreshingIds.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val bookmarksFlow = combine(_searchQuery, _selectedFolder) { query, folder ->
        Pair(query, folder)
    }.flatMapLatest { (query, folder) ->
        if (query.isNotBlank()) {
            repository.searchBookmarks(query.trim())
        } else if (folder != null) {
            repository.getBookmarksByFolder(folder)
        } else {
            repository.getAllBookmarks()
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        bookmarksFlow,
        repository.getAllFolders(),
        _selectedFolder,
        _searchQuery,
        _refreshingIds
    ) { bookmarks, folders, selectedFolder, searchQuery, refreshingIds ->
        HomeUiState(
            bookmarks = bookmarks,
            folders = folders.filter { it.isNotBlank() },
            selectedFolder = selectedFolder,
            searchQuery = searchQuery,
            refreshingBookmarkIds = refreshingIds,
            isInitialLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isInitialLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFolderSelected(folder: String?) {
        _selectedFolder.value = folder
    }

    fun refreshScreenshot(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            _refreshingIds.value = _refreshingIds.value + bookmark.id
            try {
                screenshotManager.refreshScreenshot(bookmark.id)
            } finally {
                _refreshingIds.value = _refreshingIds.value - bookmark.id
            }
        }
    }

    fun deleteBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            screenshotManager.deleteBookmark(bookmark)
        }
    }

    fun addQuickBookmark(url: String) {
        viewModelScope.launch {
            screenshotManager.saveNewBookmark(url = url, title = "")
        }
    }

    fun updateBookmarkLayout(bookmark: BookmarkEntity, columnSpan: Int, rowSpan: Int) {
        viewModelScope.launch {
            screenshotManager.updateBookmarkLayout(bookmark.id, columnSpan, rowSpan)
        }
    }

    fun swapBookmarks(first: BookmarkEntity, second: BookmarkEntity) {
        viewModelScope.launch {
            screenshotManager.swapBookmarks(first, second)
        }
    }

    fun reorderBookmarks(reorderedList: List<BookmarkEntity>) {
        viewModelScope.launch {
            screenshotManager.reorderBookmarks(reorderedList)
        }
    }

    companion object {
        fun factory(app: VisualBookmarksApp): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(
                        application = app,
                        repository = app.repository,
                        screenshotManager = app.screenshotManager
                    ) as T
                }
            }
    }
}
