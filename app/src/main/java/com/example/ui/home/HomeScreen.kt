package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.entities.BookmarkEntity
import com.example.ui.addbookmark.AddBookmarkBottomSheet
import com.example.ui.addbookmark.EditBookmarkBottomSheet
import com.example.ui.components.BookmarkCard
import com.example.ui.components.DeleteConfirmDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FolderChipsRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    var isAddSheetOpen by remember { mutableStateOf(false) }
    var initialAddUrl by remember { mutableStateOf("") }
    var editingBookmark by remember { mutableStateOf<BookmarkEntity?>(null) }
    var deletingBookmark by remember { mutableStateOf<BookmarkEntity?>(null) }

    // Drag-to-replace / drag-and-drop swap state
    var draggedBookmark by remember { mutableStateOf<BookmarkEntity?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var hoveredTargetId by remember { mutableStateOf<Long?>(null) }
    val itemBounds = remember { mutableStateMapOf<Long, Rect>() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = { SnackbarHost(snackbarHostState, modifier = Modifier.navigationBarsPadding()) },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    // Title Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Dashboard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            if (uiState.bookmarks.isNotEmpty()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "${uiState.bookmarks.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Add Button in Top Bar
                        IconButton(
                            onClick = {
                                initialAddUrl = ""
                                isAddSheetOpen = true
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .testTag("top_add_bookmark_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Bookmark",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search Input Bar
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotBlank()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("search_bookmarks_field")
                    )
                }
            }
        },
        floatingActionButton = {
            if (uiState.bookmarks.isNotEmpty() && draggedBookmark == null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        initialAddUrl = ""
                        isAddSheetOpen = true
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Bookmark", fontWeight = FontWeight.SemiBold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("fab_add_bookmark")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.bookmarks.isEmpty() && uiState.searchQuery.isBlank() && uiState.selectedFolder == null) {
                EmptyStateView(
                    onAddBookmarkClick = {
                        initialAddUrl = ""
                        isAddSheetOpen = true
                    },
                    onQuickAddSuggestion = { url ->
                        initialAddUrl = url
                        isAddSheetOpen = true
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Fixed 2 columns max with flexible item column spans (1 col or 2 cols full width)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 96.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Folder Chips Header across all 2 columns
                    if (uiState.folders.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            FolderChipsRow(
                                folders = uiState.folders,
                                selectedFolder = uiState.selectedFolder,
                                onSelectFolder = { viewModel.onFolderSelected(it) },
                                onAddCustomFolder = {
                                    initialAddUrl = ""
                                    isAddSheetOpen = true
                                },
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }

                    // Empty Search Results State
                    if (uiState.bookmarks.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No visual bookmarks found",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (uiState.searchQuery.isNotBlank()) "No bookmarks matching '${uiState.searchQuery}'" else "No bookmarks in this folder",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // Visual Bookmark Cards with dynamic column spans, stretch handles, and drag-and-drop swapping
                        items(
                            items = uiState.bookmarks,
                            key = { it.id },
                            span = { bookmark ->
                                val colSpan = if (bookmark.columnSpan >= 2) 2 else 1
                                GridItemSpan(colSpan)
                            }
                        ) { bookmark ->
                            val isRefreshing = uiState.refreshingBookmarkIds.contains(bookmark.id)
                            val isBeingDragged = draggedBookmark?.id == bookmark.id
                            val isHoverTarget = hoveredTargetId == bookmark.id

                            BookmarkCard(
                                bookmark = bookmark,
                                isRefreshing = isRefreshing,
                                isBeingDragged = isBeingDragged,
                                isHoverTarget = isHoverTarget,
                                dragOffset = if (isBeingDragged) dragOffset else Offset.Zero,
                                onEdit = { editingBookmark = it },
                                onRefreshScreenshot = {
                                    viewModel.refreshScreenshot(it)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Refreshing screenshot for ${it.displayTitle}…")
                                    }
                                },
                                onLayoutChange = { item, colSpan, rowSpan ->
                                    viewModel.updateBookmarkLayout(item, colSpan, rowSpan)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Layout updated: ${if (colSpan == 2) "Full Width" else "Half Width"} (${rowSpan}R)")
                                    }
                                },
                                onStartDragReorder = { item, _ ->
                                    draggedBookmark = item
                                    dragOffset = Offset.Zero
                                    hoveredTargetId = null
                                },
                                onDragReorder = { delta ->
                                    val currentDragged = draggedBookmark
                                    if (currentDragged != null) {
                                        dragOffset += delta
                                        val originRect = itemBounds[currentDragged.id]
                                        if (originRect != null) {
                                            val currentCenter = originRect.center + dragOffset
                                            val target = itemBounds.entries.firstOrNull { (id, rect) ->
                                                id != currentDragged.id && rect.contains(currentCenter)
                                            }?.key
                                            hoveredTargetId = target
                                        }
                                    }
                                },
                                onEndDragReorder = {
                                    val currentDragged = draggedBookmark
                                    val targetId = hoveredTargetId
                                    if (currentDragged != null && targetId != null) {
                                        val targetBookmark = uiState.bookmarks.find { it.id == targetId }
                                        if (targetBookmark != null) {
                                            viewModel.swapBookmarks(currentDragged, targetBookmark)
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Swapped positions with ${targetBookmark.displayTitle}")
                                            }
                                        }
                                    }
                                    draggedBookmark = null
                                    dragOffset = Offset.Zero
                                    hoveredTargetId = null
                                },
                                onCancelDragReorder = {
                                    draggedBookmark = null
                                    dragOffset = Offset.Zero
                                    hoveredTargetId = null
                                },
                                onDelete = { deletingBookmark = it },
                                modifier = Modifier.onGloballyPositioned { coords ->
                                    val pos = coords.positionInRoot()
                                    val size = coords.size
                                    itemBounds[bookmark.id] = Rect(
                                        pos,
                                        Size(size.width.toFloat(), size.height.toFloat())
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Bookmark BottomSheet
    if (isAddSheetOpen) {
        AddBookmarkBottomSheet(
            screenshotManager = viewModel.screenshotManager,
            initialUrl = initialAddUrl,
            onBookmarkSaved = {
                scope.launch {
                    snackbarHostState.showSnackbar("Visual bookmark added!")
                }
            },
            onDismiss = { isAddSheetOpen = false }
        )
    }

    // Edit Bookmark BottomSheet
    editingBookmark?.let { bookmark ->
        EditBookmarkBottomSheet(
            bookmark = bookmark,
            screenshotManager = viewModel.screenshotManager,
            onBookmarkUpdated = {
                scope.launch {
                    snackbarHostState.showSnackbar("Bookmark updated!")
                }
            },
            onDismiss = { editingBookmark = null }
        )
    }

    // Delete Confirmation Dialog
    deletingBookmark?.let { bookmark ->
        DeleteConfirmDialog(
            bookmark = bookmark,
            onConfirm = {
                viewModel.deleteBookmark(bookmark)
                deletingBookmark = null
                scope.launch {
                    snackbarHostState.showSnackbar("Bookmark deleted")
                }
            },
            onDismiss = { deletingBookmark = null }
        )
    }
}
