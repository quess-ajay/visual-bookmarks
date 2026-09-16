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
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.unit.ColorProvider
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
        val size = LocalSize.current

        val isSmall = size.width < 180.dp || size.height < 160.dp
        val isMedium = size.width in 180.dp..280.dp || (size.height in 160.dp..240.dp && size.width < 320.dp)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF0F172A))
                .cornerRadius(18.dp)
                .padding(8.dp)
        ) {
            if (bookmarks.isEmpty()) {
                EmptyWidgetView(context)
            } else {
                when {
                    isSmall -> SmallWidgetLayout(bookmarks.first(), context)
                    isMedium -> MediumWidgetLayout(bookmarks.take(2), context)
                    else -> LargeWidgetLayout(bookmarks.take(4), context)
                }
            }
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
                text = "Tap to add your first bookmark snapshot",
                style = TextStyle(
                    color = fixedColor(Color(0xFF94A3B8)),
                    fontSize = 11.sp
                )
            )
        }
    }

    @Composable
    private fun SmallWidgetLayout(bookmark: BookmarkEntity, context: Context) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val bitmap = loadThumbnailBitmap(bookmark.screenshotPath, 360, 480)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(12.dp)
                .clickable(actionStartActivity(browserIntent))
        ) {
            if (bitmap != null) {
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = bookmark.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = GlanceModifier.fillMaxSize().cornerRadius(12.dp)
                )
            } else {
                FallbackCardGraphic(bookmark.domain.ifBlank { bookmark.displayTitle })
            }

            // Bottom gradient overlay with title
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .background(Color(0xCC0B0F17))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = bookmark.displayTitle,
                    maxLines = 1,
                    style = TextStyle(
                        color = fixedColor(Color.White),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }

    @Composable
    private fun MediumWidgetLayout(bookmarks: List<BookmarkEntity>, context: Context) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            HeaderBar(context)
            Spacer(modifier = GlanceModifier.height(6.dp))
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                bookmarks.forEachIndexed { index, bookmark ->
                    if (index > 0) Spacer(modifier = GlanceModifier.width(6.dp))
                    Box(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                    ) {
                        BookmarkThumbnailTile(bookmark, context)
                    }
                }
            }
        }
    }

    @Composable
    private fun LargeWidgetLayout(bookmarks: List<BookmarkEntity>, context: Context) {
        Column(modifier = GlanceModifier.fillMaxSize()) {
            HeaderBar(context)
            Spacer(modifier = GlanceModifier.height(6.dp))

            // Row 1
            Row(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight()
            ) {
                val row1 = bookmarks.take(2)
                row1.forEachIndexed { index, bookmark ->
                    if (index > 0) Spacer(modifier = GlanceModifier.width(6.dp))
                    Box(
                        modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                    ) {
                        BookmarkThumbnailTile(bookmark, context)
                    }
                }
            }

            if (bookmarks.size > 2) {
                Spacer(modifier = GlanceModifier.height(6.dp))
                // Row 2
                Row(
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                ) {
                    val row2 = bookmarks.drop(2).take(2)
                    row2.forEachIndexed { index, bookmark ->
                        if (index > 0) Spacer(modifier = GlanceModifier.width(6.dp))
                        Box(
                            modifier = GlanceModifier.defaultWeight().fillMaxHeight()
                        ) {
                            BookmarkThumbnailTile(bookmark, context)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun HeaderBar(context: Context) {
        val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .clickable(actionStartActivity(mainActivityIntent)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Visual Bookmarks",
                style = TextStyle(
                    color = fixedColor(Color(0xFFE2E8F0)),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }

    @Composable
    private fun BookmarkThumbnailTile(bookmark: BookmarkEntity, context: Context) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(bookmark.url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val bitmap = loadThumbnailBitmap(bookmark.screenshotPath, 320, 240)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(10.dp)
                .background(Color(0xFF1E293B))
                .clickable(actionStartActivity(browserIntent))
        ) {
            if (bitmap != null) {
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = bookmark.displayTitle,
                    contentScale = ContentScale.Crop,
                    modifier = GlanceModifier.fillMaxSize().cornerRadius(10.dp)
                )
            } else {
                FallbackCardGraphic(bookmark.domain.ifBlank { bookmark.displayTitle })
            }

            // Bottom title pill
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(Color(0xD90B0F17))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = bookmark.displayTitle,
                    maxLines = 1,
                    style = TextStyle(
                        color = fixedColor(Color.White),
                        fontSize = 10.sp,
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
                .cornerRadius(10.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title.take(2).uppercase(),
                style = TextStyle(
                    color = fixedColor(Color(0xFFCBD5E1)),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
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
}
