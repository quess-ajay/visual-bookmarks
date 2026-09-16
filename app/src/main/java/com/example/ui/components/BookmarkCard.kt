package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.entities.BookmarkEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun BookmarkCard(
    bookmark: BookmarkEntity,
    isRefreshing: Boolean,
    onEdit: (BookmarkEntity) -> Unit,
    onRefreshScreenshot: (BookmarkEntity) -> Unit,
    onDelete: (BookmarkEntity) -> Unit,
    onLayoutChange: ((BookmarkEntity, Int, Int) -> Unit)? = null,
    onStartDragReorder: ((BookmarkEntity, Offset) -> Unit)? = null,
    onDragReorder: ((Offset) -> Unit)? = null,
    onEndDragReorder: (() -> Unit)? = null,
    onCancelDragReorder: (() -> Unit)? = null,
    isBeingDragged: Boolean = false,
    isHoverTarget: Boolean = false,
    dragOffset: Offset = Offset.Zero,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    var menuExpanded by remember { mutableStateOf(false) }

    // Resize dragging state (width & height stretching)
    var isResizeDragging by remember { mutableStateOf(false) }
    var totalResizeDragX by remember { mutableFloatStateOf(0f) }
    var totalResizeDragY by remember { mutableFloatStateOf(0f) }

    // Live preview layout targets while resizing
    var previewColSpan by remember(bookmark.columnSpan) { mutableIntStateOf(bookmark.columnSpan) }
    var previewRowSpan by remember(bookmark.rowSpan) { mutableIntStateOf(bookmark.rowSpan) }

    // Thresholds for resize gestures
    val widthThresholdPx = with(density) { 60.dp.toPx() }
    val heightThresholdPx = with(density) { 70.dp.toPx() }

    // Dynamic aspect ratio calculation
    val effectiveColSpan = if (isResizeDragging) previewColSpan else bookmark.columnSpan
    val effectiveRowSpan = if (isResizeDragging) previewRowSpan else bookmark.rowSpan

    val targetAspectRatio = remember(effectiveColSpan, effectiveRowSpan) {
        when (effectiveColSpan) {
            2 -> when (effectiveRowSpan) {
                1 -> 2.1f
                2 -> 1.25f
                3 -> 0.85f
                else -> 1.4f
            }
            else -> when (effectiveRowSpan) {
                1 -> 1.25f
                2 -> 0.72f
                3 -> 0.48f
                else -> 1.25f
            }
        }
    }

    val animatedAspectRatio by animateFloatAsState(
        targetValue = targetAspectRatio,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "aspect_ratio_anim"
    )

    val cardScale by animateFloatAsState(
        targetValue = when {
            isBeingDragged -> 1.06f
            isHoverTarget -> 0.96f
            isResizeDragging -> 1.02f
            else -> 1.0f
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "card_scale_anim"
    )

    // Pulse animation when hovered as swap target
    val infiniteTransition = rememberInfiniteTransition(label = "hover_pulse")
    val hoverPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isBeingDragged -> MaterialTheme.colorScheme.primary
            isHoverTarget -> MaterialTheme.colorScheme.tertiary
            isResizeDragging -> MaterialTheme.colorScheme.primary
            else -> Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "border_color_anim"
    )

    val currentOffset = if (isBeingDragged) {
        IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt())
    } else {
        IntOffset.Zero
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = when {
            isHoverTarget -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f)
            else -> MaterialTheme.colorScheme.surface
        },
        tonalElevation = when {
            isBeingDragged -> 16.dp
            isHoverTarget -> 6.dp
            isResizeDragging -> 8.dp
            else -> 2.dp
        },
        shadowElevation = when {
            isBeingDragged -> 20.dp
            isHoverTarget -> 8.dp
            isResizeDragging -> 10.dp
            else -> 3.dp
        },
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (isBeingDragged) 99f else if (isHoverTarget) 5f else 1f)
            .offset { currentOffset }
            .scale(cardScale)
            .alpha(if (isBeingDragged) 0.92f else 1f)
            .border(
                width = if (isBeingDragged || isHoverTarget) 3.dp else if (isResizeDragging) 2.dp else 0.dp,
                color = if (isHoverTarget) borderColor.copy(alpha = hoverPulseAlpha) else borderColor,
                shape = RoundedCornerShape(22.dp)
            )
            .testTag("bookmark_card_${bookmark.id}")
            .clip(RoundedCornerShape(22.dp))
            // Long-press to initiate Home Screen-style Drag to Swap/Reorder
            .pointerInput(bookmark.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        onStartDragReorder?.invoke(bookmark, offset)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragReorder?.invoke(dragAmount)
                    },
                    onDragEnd = {
                        onEndDragReorder?.invoke()
                    },
                    onDragCancel = {
                        onCancelDragReorder?.invoke()
                    }
                )
            }
            .clickable(enabled = !isBeingDragged && !isResizeDragging) {
                openInBrowser(context, bookmark.url)
            }
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Webpage Screenshot Area with dynamic aspect ratio
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(animatedAspectRatio)
                ) {
                    WebpageSnapshotImage(
                        screenshotPath = bookmark.screenshotPath,
                        domain = bookmark.domain,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Domain badge & Favicon at top left
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xCC0B0F17))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!bookmark.faviconPath.isNullOrBlank() && File(bookmark.faviconPath).exists()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(File(bookmark.faviconPath))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Favicon",
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                        }
                        Text(
                            text = bookmark.domain.ifBlank { "webpage" },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Direct Drag Handle at top center (Tap & Drag to swap like Home Screen widgets)
                    if (onStartDragReorder != null) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xCC0B0F17),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                                .size(width = 36.dp, height = 24.dp)
                                .pointerInput(bookmark.id) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            onStartDragReorder(bookmark, offset)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            onDragReorder?.invoke(dragAmount)
                                        },
                                        onDragEnd = {
                                            onEndDragReorder?.invoke()
                                        },
                                        onDragCancel = {
                                            onCancelDragReorder?.invoke()
                                        }
                                    )
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DragIndicator,
                                    contentDescription = "Drag to swap bookmark",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Layout Size Badges (shown if non-default)
                    if (bookmark.columnSpan == 2 || bookmark.rowSpan > 1) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xD90F172A))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${if (bookmark.columnSpan == 2) "2 Cols (Full)" else "1 Col"} • ${bookmark.rowSpan}R",
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Options Menu Button at Top Right
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xAA0B0F17))
                                .testTag("menu_button_${bookmark.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Open in Browser") },
                                leadingIcon = {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    openInBrowser(context, bookmark.url)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Bookmark") },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onEdit(bookmark)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Refresh Screenshot") },
                                leadingIcon = {
                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onRefreshScreenshot(bookmark)
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            DropdownMenuItem(
                                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDelete(bookmark)
                                }
                            )
                        }
                    }

                    // Loading overlay when screenshot is actively refreshing
                    if (isRefreshing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x99000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp
                            )
                        }
                    }
                }

                // Bottom Title & Details Info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = bookmark.displayTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (bookmark.columnSpan == 2 && bookmark.rowSpan >= 2) 3 else 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (!bookmark.description.isNullOrBlank() && bookmark.columnSpan == 2 && bookmark.rowSpan >= 2) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = bookmark.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Folder pill tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = bookmark.folder,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Date / relative timestamp
                        val formattedDate = remember(bookmark.createdAt) {
                            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                            sdf.format(Date(bookmark.createdAt))
                        }
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Interactive Direct Stretch/Resize Handles (Corner + Bottom Bar)
            if (onLayoutChange != null && !isBeingDragged) {
                // Bottom Center Stretch Bar (Vertical Height Drag)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 2.dp)
                        .size(width = 48.dp, height = 24.dp)
                        .pointerInput(bookmark.id, bookmark.rowSpan) {
                            detectDragGestures(
                                onDragStart = {
                                    isResizeDragging = true
                                    totalResizeDragY = 0f
                                    previewRowSpan = bookmark.rowSpan
                                },
                                onDragEnd = {
                                    isResizeDragging = false
                                    if (previewRowSpan != bookmark.rowSpan) {
                                        onLayoutChange(bookmark, bookmark.columnSpan, previewRowSpan)
                                    }
                                    totalResizeDragY = 0f
                                },
                                onDragCancel = {
                                    isResizeDragging = false
                                    previewRowSpan = bookmark.rowSpan
                                    totalResizeDragY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    totalResizeDragY += dragAmount.y

                                    // Downward drag increases rowSpan (1 -> 2 -> 3), Upward decreases (3 -> 2 -> 1)
                                    val rowDelta = (totalResizeDragY / heightThresholdPx).toInt()
                                    val newTarget = (bookmark.rowSpan + rowDelta).coerceIn(1, 3)
                                    previewRowSpan = newTarget
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 32.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                    )
                }

                // Bottom Right Corner Handle (Horizontal Width + Vertical Height Drag)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(44.dp)
                        .testTag("drag_resize_handle_${bookmark.id}")
                        .pointerInput(bookmark.id, bookmark.columnSpan, bookmark.rowSpan) {
                            detectDragGestures(
                                onDragStart = {
                                    isResizeDragging = true
                                    totalResizeDragX = 0f
                                    totalResizeDragY = 0f
                                    previewColSpan = bookmark.columnSpan
                                    previewRowSpan = bookmark.rowSpan
                                },
                                onDragEnd = {
                                    isResizeDragging = false
                                    if (previewColSpan != bookmark.columnSpan || previewRowSpan != bookmark.rowSpan) {
                                        onLayoutChange(bookmark, previewColSpan, previewRowSpan)
                                    }
                                    totalResizeDragX = 0f
                                    totalResizeDragY = 0f
                                },
                                onDragCancel = {
                                    isResizeDragging = false
                                    previewColSpan = bookmark.columnSpan
                                    previewRowSpan = bookmark.rowSpan
                                    totalResizeDragX = 0f
                                    totalResizeDragY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    totalResizeDragX += dragAmount.x
                                    totalResizeDragY += dragAmount.y

                                    // Horizontal Width Stretch:
                                    val newCol = when {
                                        bookmark.columnSpan == 1 && totalResizeDragX > widthThresholdPx -> 2
                                        bookmark.columnSpan == 2 && totalResizeDragX < -widthThresholdPx -> 1
                                        else -> bookmark.columnSpan
                                    }
                                    previewColSpan = newCol

                                    // Vertical Height Stretch:
                                    val rowDelta = (totalResizeDragY / heightThresholdPx).toInt()
                                    val newRow = (bookmark.rowSpan + rowDelta).coerceIn(1, 3)
                                    previewRowSpan = newRow
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isResizeDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                        tonalElevation = 2.dp,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInFull,
                            contentDescription = "Drag to stretch layout",
                            tint = if (isResizeDragging) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(5.dp)
                                .size(18.dp)
                        )
                    }
                }
            }

            // Live Drag Overlay Indicator Badge when Resizing
            AnimatedVisibility(
                visible = isResizeDragging,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xF00F172A),
                    tonalElevation = 8.dp,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.OpenWith,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Release to set size:",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${if (previewColSpan == 2) "2 Columns (Full Width)" else "1 Column (Half Width)"} • ${previewRowSpan} Row${if (previewRowSpan > 1) "s" else ""}",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Live Drop Target Overlay (when another card is dragged over this card to swap)
            AnimatedVisibility(
                visible = isHoverTarget,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    tonalElevation = 10.dp,
                    modifier = Modifier.padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Release to swap position",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun openInBrowser(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open browser for URL", Toast.LENGTH_SHORT).show()
    }
}
