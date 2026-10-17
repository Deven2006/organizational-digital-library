package com.example.digitallibrary.ui.series

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.digitallibrary.data.model.Book
import com.example.digitallibrary.data.model.DownloadState
import com.example.digitallibrary.data.model.Series
import com.example.digitallibrary.data.repository.BatchDownloadProgress
import com.example.digitallibrary.data.repository.BookRepository
import com.example.digitallibrary.ui.theme.FormatDocxColor
import com.example.digitallibrary.ui.theme.FormatPdfColor
import com.example.digitallibrary.ui.theme.FormatTxtColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesDetailScreen(
    seriesId: String,
    bookRepository: BookRepository,
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    onReadClick: (String) -> Unit
) {
    val downloadManager = bookRepository.downloadManager
    val scope = rememberCoroutineScope()

    var series by remember { mutableStateOf<Series?>(null) }
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Multi-select state
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedBookIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Batch download progress state
    var batchProgress by remember { mutableStateOf<BatchDownloadProgress?>(null) }
    var isBatchDownloading by remember { mutableStateOf(false) }

    val downloadStates by downloadManager.downloadStates.collectAsState()

    fun loadSeriesData() {
        scope.launch {
            isLoading = true
            errorMessage = null

            val seriesListResult = bookRepository.getPublishedSeries()
            if (seriesListResult.isSuccess) {
                series = seriesListResult.getOrThrow().find { it.id == seriesId }
            }

            val booksResult = bookRepository.getSeriesBooks(seriesId)
            if (booksResult.isSuccess) {
                // Ensure ordered strictly by series_order
                books = booksResult.getOrThrow().sortedBy { it.seriesOrder }
            } else {
                errorMessage = booksResult.exceptionOrNull()?.message ?: "Failed to load series documents."
            }

            isLoading = false
        }
    }

    LaunchedEffect(seriesId) {
        loadSeriesData()
    }

    fun startBatchDownload(targetBooks: List<Book>) {
        if (targetBooks.isEmpty() || isBatchDownloading) return

        scope.launch {
            isBatchDownloading = true
            downloadManager.downloadMultipleBooks(targetBooks).collect { progress ->
                batchProgress = progress
                if (progress.isFinished) {
                    isBatchDownloading = false
                    isMultiSelectMode = false
                    selectedBookIds = emptySet()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = series?.title ?: "Series Details",
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (books.isNotEmpty() && !isBatchDownloading) {
                        TextButton(onClick = {
                            isMultiSelectMode = !isMultiSelectMode
                            if (!isMultiSelectMode) selectedBookIds = emptySet()
                        }) {
                            Text(if (isMultiSelectMode) "Cancel" else "Select")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                errorMessage != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Card(
                            modifier = Modifier.padding(24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Error Loading Series",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { loadSeriesData() }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Series Header Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Layers,
                                                contentDescription = "Series",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(26.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                text = series?.title ?: "",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${books.size} Books in Series",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }

                                    if (!series?.description.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = series?.description ?: "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                            lineHeight = 22.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Batch Progress bar if active
                                    if (isBatchDownloading && batchProgress != null) {
                                        val bp = batchProgress!!
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "Downloading series (${bp.completedBooks}/${bp.totalBooks})...",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = bp.currentBookTitle,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.width(140.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            LinearProgressIndicator(
                                                progress = {
                                                    if (bp.totalBooks > 0) bp.completedBooks.toFloat() / bp.totalBooks.toFloat() else 0f
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp)),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }

                                    // Action buttons: Download entire series OR Download Selected
                                    if (isMultiSelectMode) {
                                        Button(
                                            onClick = {
                                                val selectedBooks = books.filter { it.id in selectedBookIds }
                                                startBatchDownload(selectedBooks)
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = selectedBookIds.isNotEmpty() && !isBatchDownloading,
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Download, contentDescription = "Download")
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Download Selected (${selectedBookIds.size})")
                                        }
                                    } else {
                                        Button(
                                            onClick = { startBatchDownload(books) },
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = books.isNotEmpty() && !isBatchDownloading,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(imageVector = Icons.Default.Download, contentDescription = "Download Entire Series")
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Download Entire Series (${books.size} Books)")
                                        }
                                    }
                                }
                            }
                        }

                        // Section Title
                        item {
                            Text(
                                text = "Ordered Sequence",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                        }

                        // Ordered Books List
                        items(books, key = { it.id }) { book ->
                            val isDownloaded = downloadManager.isBookDownloaded(book.id)
                            val bookDownloadState = downloadStates[book.id]
                            val isSelected = book.id in selectedBookIds

                            val badgeColor = when {
                                book.isPdf -> FormatPdfColor
                                book.isDocx -> FormatDocxColor
                                book.isTxt -> FormatTxtColor
                                else -> MaterialTheme.colorScheme.primary
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isMultiSelectMode) {
                                            selectedBookIds = if (isSelected) selectedBookIds - book.id else selectedBookIds + book.id
                                        } else {
                                            onBookClick(book.id)
                                        }
                                    },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isMultiSelectMode) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                selectedBookIds = if (checked) selectedBookIds + book.id else selectedBookIds - book.id
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }

                                    // Sequence badge (1, 2, 3...)
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${book.seriesOrder.takeIf { it > 0 } ?: (books.indexOf(book) + 1)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = book.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = book.author ?: "Unknown",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(badgeColor.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = book.fileType.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = badgeColor,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Action buttons for individual book
                                    when {
                                        bookDownloadState is DownloadState.InProgress -> {
                                            CircularProgressIndicator(
                                                progress = { bookDownloadState.progressPercent / 100f },
                                                modifier = Modifier.size(32.dp),
                                                strokeWidth = 3.dp
                                            )
                                        }

                                        isDownloaded -> {
                                            IconButton(onClick = { onReadClick(book.id) }) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoStories,
                                                    contentDescription = "Read",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        else -> {
                                            IconButton(onClick = {
                                                scope.launch {
                                                    downloadManager.downloadBook(book)
                                                }
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.Download,
                                                    contentDescription = "Download Book",
                                                    tint = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
