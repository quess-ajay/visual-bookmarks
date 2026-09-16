package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File

@Composable
fun WebpageSnapshotImage(
    screenshotPath: String?,
    domain: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current
    val hasValidFile = remember(screenshotPath) {
        !screenshotPath.isNullOrBlank() && File(screenshotPath).exists() && File(screenshotPath).length() > 0
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (hasValidFile) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(File(screenshotPath!!))
                    .crossfade(true)
                    .build(),
                contentDescription = "Webpage screenshot for $domain",
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Elegant Monogram Fallback
            val gradientColors = remember(domain) {
                val hash = domain.hashCode()
                val color1 = Color(
                    red = ((hash and 0xFF0000 shr 16) % 150 + 60) / 255f,
                    green = ((hash and 0x00FF00 shr 8) % 150 + 60) / 255f,
                    blue = ((hash and 0x0000FF) % 150 + 90) / 255f
                )
                val color2 = Color(
                    red = ((hash * 31 and 0xFF0000 shr 16) % 140 + 40) / 255f,
                    green = ((hash * 31 and 0x00FF00 shr 8) % 140 + 40) / 255f,
                    blue = ((hash * 31 and 0x0000FF) % 140 + 80) / 255f
                )
                listOf(color1, color2)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = domain.trimStart().firstOrNull()?.uppercaseChar()?.toString() ?: "W",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
