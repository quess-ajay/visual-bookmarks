package com.example.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.data.database.AppDatabase
import com.example.data.entities.BookmarkEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class VisualBookmarkWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VisualBookmarkWidget()
}

class VisualBookmarkWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val database = AppDatabase.getDatabase(context)
        val bookmarks = withContext(Dispatchers.IO) {
            database.bookmarkDao().getAllBookmarksSync()
        }

        provideContent {
            GlanceTheme {
                WidgetContent(bookmarks = bookmarks)
            }
        }
    }

    private fun fixedColor(color: Color): ColorProvider = ColorProvider(color)

    @Composable
    private fun WidgetContent(bookmarks: List<BookmarkEntity>) {
        val context = LocalContext.current

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .cornerRadius(20.dp)
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            if (bookmarks.isEmpty()) {
                EmptyWidgetView(context)
            } else {
                val gridRows = groupBookmarksForGrid(bookmarks)
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    WidgetHeader(context, bookmarks.size)
                    Spacer(modifier = GlanceModifier.height(6.dp))

                    // Scrollable LazyColumn mirroring the 2-column app grid & custom spans
                    LazyColumn(
                        modifier = GlanceModifier.fillMaxSize()
                    ) {
                        items(gridRows) { rowItem ->
                            when (rowItem) {
                                is WidgetGridRow.SingleFullSpan -> {
                                    FullWidthBookmarkTile(rowItem.bookmark, context)
                                    Spacer(modifier = GlanceModifier.height(8.dp))
                                }
                                is WidgetGridRow.TwoColumns -> {
                                    TwoColumnBookmarkRow(rowItem.left, rowItem.right, context)
                                    Spacer(modifier = GlanceModifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun WidgetHeader(context: Context, count: Int) {
        val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .clickable(actionStartActivity(mainActivityIntent)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Visual Bookmarks",
                style = TextStyle(
                    color = fixedColor(Color(0xFFF8FAFC)),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Box(
                modifier = GlanceModifier
                    .cornerRadius(6.dp)
                    .background(Color(0xFF3B82F6))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "$count",
                    style = TextStyle(
                        color = fixedColor(Color.White),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Spacer(modifier = GlanceModifier.defaultWeight())
            Text(
                text = "+ Add",
                style = TextStyle(
                    color = fixedColor(Color(0xFF60A5FA)),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    @Composable
    private fun TwoColumnBookmarkRow(
        left: BookmarkEntity,
        right: BookmarkEntity?,
        context: Context
    ) {
        val maxRowSpan = maxOf(left.rowSpan, right?.rowSpan ?: left.rowSpan)
        val rowHeight = when (maxRowSpan) {
            1 -> 115.dp
            2 -> 165.dp
            3 -> 220.dp
            else -> 115.dp
        }

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(rowHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
            ) {
                BookmarkTile(left, context)
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            if (right != null) {
                Box(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                ) {
                    BookmarkTile(right, context)
                }
            } else {
                Spacer(modifier = GlanceModifier.defaultWeight().fillMaxHeight())
            }
        }
    }

    @Composable
    private fun FullWidthBookmarkTile(bookmark: BookmarkEntity, context: Context) {
        val rowHeight = when (bookmark.rowSpan) {
            1 -> 130.dp
            2 -> 190.dp
            3 -> 260.dp
            else -> 130.dp
        }

        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(rowHeight)
        ) {
            BookmarkTile(bookmark, context, isFullWidth = true)
        }
    }

    @Composable
    private fun BookmarkTile(
        bookmark: BookmarkEntity,
        context: Context,
        isFullWidth: Boolean = false
    ) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val targetW = if (isFullWidth) 480 else 320
        val targetH = when (bookmark.rowSpan) {
            1 -> 240
            2 -> 360
            3 -> 480
            else -> 240
        }
        val bitmap = loadThumbnailBitmap(bookmark.screenshotPath, targetW, targetH)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(12.dp)
                .background(Color(0xFF1E293B))
                .clickable(actionStartActivity(browserIntent))
        ) {
            if (bitmap != null) {
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = bookmark.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(12.dp)
                )
            } else {
                FallbackCardGraphic(bookmark.domain.ifBlank { bookmark.displayTitle })
            }

            // Top Domain Badge
            Row(
                modifier = GlanceModifier
                    .padding(6.dp)
                    .background(Color(0xCC0B0F17))
                    .cornerRadius(6.dp)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = bookmark.domain.ifBlank { "web" },
                    maxLines = 1,
                    style = TextStyle(
                        color = fixedColor(Color(0xFFE2E8F0)),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Bottom Title Bar
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(Color(0xDF0B0F17))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = bookmark.displayTitle,
                    maxLines = 1,
                    style = TextStyle(
                        color = fixedColor(Color.White),
                        fontSize = if (isFullWidth) 11.sp else 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }

    @Composable
    private fun FallbackCardGraphic(title: String) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF334155))
                .cornerRadius(12.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title.take(2).uppercase(),
                style = TextStyle(
                    color = fixedColor(Color(0xFF94A3B8)),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    @Composable
    private fun EmptyWidgetView(context: Context) {
        val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .clickable(actionStartActivity(mainActivityIntent))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Visual Bookmarks",
                style = TextStyle(
                    color = fixedColor(Color.White),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = "Tap to open app & add your first bookmark",
                style = TextStyle(
                    color = fixedColor(Color(0xFF94A3B8)),
                    fontSize = 11.sp
                )
            )
        }
    }

    private fun loadThumbnailBitmap(path: String?, targetW: Int, targetH: Int): Bitmap? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (!file.exists() || file.length() == 0L) return null

        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, options)

            var sampleSize = 1
            if (options.outHeight > targetH || options.outWidth > targetW) {
                val halfH = options.outHeight / 2
                val halfW = options.outWidth / 2
                while ((halfH / sampleSize) >= targetH && (halfW / sampleSize) >= targetW) {
                    sampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeFile(path, decodeOptions)
        } catch (e: Exception) {
            null
        }
    }

    sealed class WidgetGridRow {
        data class SingleFullSpan(val bookmark: BookmarkEntity) : WidgetGridRow()
        data class TwoColumns(val left: BookmarkEntity, val right: BookmarkEntity?) : WidgetGridRow()
    }

    private fun groupBookmarksForGrid(bookmarks: List<BookmarkEntity>): List<WidgetGridRow> {
        val rows = mutableListOf<WidgetGridRow>()
        var i = 0
        while (i < bookmarks.size) {
            val current = bookmarks[i]
            if (current.columnSpan >= 2) {
                rows.add(WidgetGridRow.SingleFullSpan(current))
                i++
            } else {
                val next = bookmarks.getOrNull(i + 1)
                if (next != null && next.columnSpan < 2) {
                    rows.add(WidgetGridRow.TwoColumns(current, next))
                    i += 2
                } else {
                    rows.add(WidgetGridRow.TwoColumns(current, null))
                    i++
                }
            }
        }
        return rows
    }
}
