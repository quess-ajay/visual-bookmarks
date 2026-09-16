package com.example.ui.addbookmark

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.outlined.ErrorOutline
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.screenshot.ScreenshotCaptureResult
import com.example.screenshot.ScreenshotGenerator
import com.example.screenshot.ScreenshotManager
import com.example.ui.components.LayoutSizeSelector
import com.example.ui.components.WebpageSnapshotImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookmarkBottomSheet(
    screenshotManager: ScreenshotManager,
    initialUrl: String = "",
    onBookmarkSaved: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var urlInput by remember { mutableStateOf(initialUrl) }
    var titleInput by remember { mutableStateOf("") }
    var folderInput by remember { mutableStateOf("General") }
    var notesInput by remember { mutableStateOf("") }

    var columnSpan by remember { mutableIntStateOf(1) } // 1 or 2
    var rowSpan by remember { mutableIntStateOf(1) }    // 1, 2, or 3

    var isLoadingPreview by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var loadingStatusText by remember { mutableStateOf("Loading webpage snapshot…") }

    var previewResult by remember { mutableStateOf<ScreenshotCaptureResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun fetchPreview() {
        val trimmed = urlInput.trim()
        if (trimmed.isBlank()) {
            errorMessage = "Please enter a valid webpage URL"
            return
        }

        errorMessage = null
        isLoadingPreview = true
        loadingStatusText = "Rendering webpage and capturing snapshot…"

        scope.launch {
            try {
                val normalized = ScreenshotGenerator.normalizeUrl(trimmed)
                urlInput = normalized
                val result = screenshotManager.generatePreviewForUrl(normalized)
                previewResult = result
                if (titleInput.isBlank()) {
                    titleInput = result.title
                }
                if (!result.isSuccess && result.errorMessage != null) {
                    errorMessage = result.errorMessage
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load webpage preview: ${e.localizedMessage}"
            } finally {
                isLoadingPreview = false
            }
        }
    }

    LaunchedEffect(initialUrl) {
        if (initialUrl.isNotBlank()) {
            fetchPreview()
        }
    }

    fun saveBookmark() {
        val trimmedUrl = urlInput.trim()
        if (trimmedUrl.isBlank()) {
            errorMessage = "Please enter a webpage URL"
            return
        }

        isSaving = true
        scope.launch {
            try {
                screenshotManager.saveNewBookmark(
                    url = trimmedUrl,
                    title = titleInput.ifBlank { previewResult?.title ?: ScreenshotGenerator.extractDomain(trimmedUrl) },
                    folder = folderInput.ifBlank { "General" },
                    description = notesInput.ifBlank { null },
                    screenshotPath = previewResult?.screenshotPath,
                    faviconPath = previewResult?.faviconPath,
                    columnSpan = columnSpan,
                    rowSpan = rowSpan
                )
                onBookmarkSaved()
                onDismiss()
            } catch (e: Exception) {
                errorMessage = "Could not save bookmark: ${e.localizedMessage}"
            } finally {
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
                    text = stringResource(R.string.add_visual_bookmark),
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

            Spacer(modifier = Modifier.height(18.dp))

            // URL Input Field
            OutlinedTextField(
                value = urlInput,
                onValueChange = {
                    urlInput = it
                    errorMessage = null
                },
                label = { Text(stringResource(R.string.url_label)) },
                placeholder = { Text(stringResource(R.string.url_placeholder)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (urlInput.isBlank()) {
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clipData = clipboard.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val text = clipData.getItemAt(0).text?.toString() ?: ""
                                if (text.isNotBlank()) {
                                    urlInput = text
                                    fetchPreview()
                                }
                            }
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                        }
                    } else {
                        IconButton(onClick = { urlInput = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(onGo = { fetchPreview() }),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("url_input_field")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Preview Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = { fetchPreview() },
                    enabled = urlInput.isNotBlank() && !isLoadingPreview,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("fetch_preview_button")
                ) {
                    if (isLoadingPreview) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(stringResource(R.string.preview_webpage), fontWeight = FontWeight.SemiBold)
                }
            }

            // Error Message Banner
            AnimatedVisibility(visible = errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Visual Preview Card Display
            if (isLoadingPreview) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = loadingStatusText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (previewResult != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Visual Snapshot Preview",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        WebpageSnapshotImage(
                            screenshotPath = previewResult?.screenshotPath,
                            domain = previewResult?.domain ?: "",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title Field
            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                label = { Text(stringResource(R.string.title_label)) },
                placeholder = { Text("Webpage title") },
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
                    .testTag("title_input_field")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Folder Field
            OutlinedTextField(
                value = folderInput,
                onValueChange = { folderInput = it },
                label = { Text(stringResource(R.string.folder_label)) },
                placeholder = { Text("e.g. Design, Dev, Reading, Tools") },
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
                    .testTag("folder_input_field")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Notes / Description
            OutlinedTextField(
                value = notesInput,
                onValueChange = { notesInput = it },
                label = { Text(stringResource(R.string.notes_label)) },
                placeholder = { Text("Optional notes or tags") },
                maxLines = 3,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notes_input_field")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Flexible Layout & Grid Sizing Selector
            LayoutSizeSelector(
                columnSpan = columnSpan,
                rowSpan = rowSpan,
                onColumnSpanChange = { columnSpan = it },
                onRowSpanChange = { rowSpan = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Action: [ Save Bookmark ]
            Button(
                onClick = { saveBookmark() },
                enabled = urlInput.isNotBlank() && !isSaving,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_bookmark_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Saving Bookmark…")
                } else {
                    Text(
                        text = stringResource(R.string.save_bookmark),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
