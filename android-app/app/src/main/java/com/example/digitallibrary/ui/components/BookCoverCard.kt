package com.example.digitallibrary.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.digitallibrary.ui.theme.DeepMaroon
import com.example.digitallibrary.ui.theme.DeepMaroonDark
import com.example.digitallibrary.ui.theme.FormatDocxColor
import com.example.digitallibrary.ui.theme.FormatPdfColor
import com.example.digitallibrary.ui.theme.FormatTxtColor
import com.example.digitallibrary.ui.theme.MutedGold
import com.example.digitallibrary.ui.theme.MutedGoldLight
import com.example.digitallibrary.ui.theme.TerracottaDark
import com.example.digitallibrary.ui.theme.TerracottaPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

private val imageHttpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

/**
 * Renders high-quality book cover artwork.
 * If a valid coverImageUrl is provided, downloads and displays the remote image.
 * Otherwise, renders an authentic traditional Indian scripture / pothi-style bound book cover
 * with deep maroon/terracotta leather gradients, golden margins, Sanskrit script motifs,
 * book title, author, and format badge.
 */
@Composable
fun BookCoverCard(
    title: String,
    author: String?,
    fileType: String,
    modifier: Modifier = Modifier,
    coverImageUrl: String? = null,
    isLarge: Boolean = false,
    seriesOrder: Int = 0
) {
    var loadedBitmap by remember(coverImageUrl) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(coverImageUrl) {
        if (!coverImageUrl.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val request = Request.Builder().url(coverImageUrl).build()
                    imageHttpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val bytes = response.body?.bytes()
                            if (bytes != null) {
                                loadedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Fall back to stylized scripture cover
                }
            }
        }
    }

    val badgeColor = when {
        fileType.equals("pdf", ignoreCase = true) -> FormatPdfColor
        fileType.equals("docx", ignoreCase = true) -> FormatDocxColor
        fileType.equals("txt", ignoreCase = true) -> FormatTxtColor
        else -> TerracottaPrimary
    }

    val cornerRadius = if (isLarge) 16.dp else 10.dp
    val elevation = if (isLarge) 6.dp else 3.dp

    Box(
        modifier = modifier
            .shadow(elevation, RoundedCornerShape(cornerRadius))
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DeepMaroon,
                        DeepMaroonDark,
                        Color(0xFF240A02)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = MutedGold.copy(alpha = 0.5f),
                shape = RoundedCornerShape(cornerRadius)
            )
    ) {
        if (loadedBitmap != null) {
            Image(
                bitmap = loadedBitmap!!.asImageBitmap(),
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Traditional Devotional Scripture Cover Design
            // Left Spine Accent (Pothi fold / sacred binding)
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .width(if (isLarge) 8.dp else 5.dp)
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MutedGoldLight.copy(alpha = 0.7f),
                                    MutedGold.copy(alpha = 0.9f),
                                    TerracottaDark
                                )
                            )
                        )
                )

                // Main Cover Body
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(if (isLarge) 14.dp else 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Sacred Header
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "।। ॐ ।।",
                            color = MutedGold,
                            fontSize = if (isLarge) 11.sp else 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                    }

                    // Center Motif & Title
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = if (isLarge) 8.dp else 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isLarge) 44.dp else 26.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MutedGold.copy(alpha = 0.15f))
                                .border(0.5.dp, MutedGold.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = null,
                                tint = MutedGoldLight,
                                modifier = Modifier.size(if (isLarge) 24.dp else 16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(if (isLarge) 8.dp else 4.dp))

                        Text(
                            text = title,
                            color = Color(0xFFFBF4EB),
                            style = if (isLarge) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            textAlign = TextAlign.Center,
                            maxLines = if (isLarge) 3 else 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = if (isLarge) 20.sp else 13.sp
                        )

                        if (!author.isNullOrBlank() && isLarge) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = author,
                                color = MutedGoldLight.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Bottom Vol or Floral flourish
                    if (seriesOrder > 0) {
                        Text(
                            text = "भाग $seriesOrder • Vol. $seriesOrder",
                            color = MutedGold,
                            fontSize = if (isLarge) 10.sp else 8.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "• • •",
                            color = MutedGold.copy(alpha = 0.5f),
                            fontSize = if (isLarge) 10.sp else 7.sp
                        )
                    }
                }
            }
        }
    }
}
