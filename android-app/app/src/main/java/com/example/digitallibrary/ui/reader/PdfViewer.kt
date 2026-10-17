package com.example.digitallibrary.ui.reader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfViewer(
    pdfFile: File,
    modifier: Modifier = Modifier,
    initialPage: Int = 0,
    onPageChanged: ((pageIndex: Int, totalPages: Int) -> Unit)? = null
) {
    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    DisposableEffect(pdfFile) {
        try {
            val fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fd)
            fileDescriptor = fd
            renderer = pdfRenderer
            pageCount = pdfRenderer.pageCount
        } catch (e: Exception) {
            errorMsg = "Unable to open PDF document: ${e.message}"
        }

        onDispose {
            renderer?.close()
            fileDescriptor?.close()
        }
    }

    if (errorMsg != null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = errorMsg ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    val currentRenderer = renderer
    if (currentRenderer == null || pageCount == 0) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val safeInitialPage = initialPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
    val listState = androidx.compose.foundation.lazy.rememberLazyListState(
        initialFirstVisibleItemIndex = safeInitialPage
    )

    LaunchedEffect(listState.firstVisibleItemIndex) {
        onPageChanged?.invoke(listState.firstVisibleItemIndex, pageCount)
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE2E8F0))
            .padding(vertical = 12.dp, horizontal = 10.dp)
    ) {
        items(pageCount) { pageIndex ->
            PdfPageItem(
                renderer = currentRenderer,
                pageIndex = pageIndex,
                totalPages = pageCount
            )
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Composable
private fun PdfPageItem(
    renderer: PdfRenderer,
    pageIndex: Int,
    totalPages: Int
) {
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pageIndex) {
        withContext(Dispatchers.IO) {
            try {
                // Synchronize on renderer because PdfRenderer is not thread-safe across concurrent calls
                synchronized(renderer) {
                    val page = renderer.openPage(pageIndex)
                    // Render at high resolution for crisp text (2x page dimensions)
                    val width = page.width * 2
                    val height = page.height * 2
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    // Fill white background before rendering PDF
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    pageBitmap = bitmap
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val bitmap = pageBitmap
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat()),
                    contentScale = ContentScale.FillWidth
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                }
            }

            // Page indicator footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF1F5F9))
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Page ${pageIndex + 1} of $totalPages",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}
