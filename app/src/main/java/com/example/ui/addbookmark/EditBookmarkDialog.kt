package com.example.ui.addbookmark

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.entities.BookmarkEntity
import com.example.screenshot.ScreenshotManager
import com.example.ui.components.LayoutSizeSelector
import com.example.ui.components.WebpageSnapshotImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBookmarkBottomSheet(
    bookmark: BookmarkEntity,
    screenshotManager: ScreenshotManager,
    onBookmarkUpdated: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var titleInput by remember { mutableStateOf(bookmark.displayTitle) }
    var folderInput by remember { mutableStateOf(bookmark.folder) }
    var notesInput by remember { mutableStateOf(bookmark.description ?: "") }

    var columnSpan by remember { mutableIntStateOf(bookmark.columnSpan) }
    var rowSpan by remember { mutableIntStateOf(bookmark.rowSpan) }

    var isSaving by remember { mutableStateOf(false) }
    var isRefreshingScreenshot by remember { mutableStateOf(false) }
    var currentBookmark by remember { mutableStateOf(bookmark) }

    fun refreshScreenshot() {
        isRefreshingScreenshot = true
        scope.launch {
            try {
                screenshotManager.refreshScreenshot(currentBookmark.id)
                onBookmarkUpdated()
            } catch (ignored: Exception) {} finally {
                isRefreshingScreenshot = false
            }
        }
    }

    fun saveChanges() {
        isSaving = true
        scope.launch {
            try {
                screenshotManager.updateBookmarkDetails(
                    id = currentBookmark.id,
                    newTitle = titleInput,
                    newFolder = folderInput,
                    newDescription = notesInput.ifBlank { null },
                    columnSpan = columnSpan,
                    rowSpan = rowSpan
                )
                onBookmarkUpdated()
                onDismiss()
            } catch (ignored: Exception) {} finally {
                isSaving = false
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.edit_bookmark),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current Snapshot Card Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                WebpageSnapshotImage(
                    screenshotPath = currentBookmark.screenshotPath,
                    domain = currentBookmark.domain,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Refresh Screenshot Button
            OutlinedButton(
                onClick = { refreshScreenshot() },
                enabled = !isRefreshingScreenshot,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("refresh_snapshot_button")
            ) {
                if (isRefreshingScreenshot) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refreshing Webpage Snapshot…")
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.refresh_screenshot))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // URL (Read-only reference)
            OutlinedTextField(
                value = currentBookmark.url,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.url_label)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Title Field
            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                label = { Text(stringResource(R.string.title_label)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Title,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_title_field")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Folder Field
            OutlinedTextField(
                value = folderInput,
                onValueChange = { folderInput = it },
                label = { Text(stringResource(R.string.folder_label)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_folder_field")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Notes / Description Field
            OutlinedTextField(
                value = notesInput,
                onValueChange = { notesInput = it },
                label = { Text(stringResource(R.string.notes_label)) },
                placeholder = { Text("Optional notes or tags") },
                maxLines = 3,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Layout & Sizing selector
            LayoutSizeSelector(
                columnSpan = columnSpan,
                rowSpan = rowSpan,
                onColumnSpanChange = { columnSpan = it },
                onRowSpanChange = { rowSpan = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Action: Save Changes
            Button(
                onClick = { saveChanges() },
                enabled = !isSaving,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_edit_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving…")
                } else {
                    Text(
                        text = stringResource(R.string.save),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
