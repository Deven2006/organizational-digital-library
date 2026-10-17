package com.example.digitallibrary.ui.series

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.digitallibrary.data.model.Book
import com.example.digitallibrary.data.model.DownloadState
import com.example.digitallibrary.data.model.Series
import com.example.digitallibrary.data.repository.BatchDownloadProgress
import com.example.digitallibrary.data.repository.BookRepository
import com.example.digitallibrary.ui.common.LanguageManager
import com.example.digitallibrary.ui.common.LibraryStrings
import com.example.digitallibrary.ui.components.BookCoverCard
import com.example.digitallibrary.ui.components.DevotionalErrorView
import com.example.digitallibrary.ui.components.DevotionalLoadingView
import com.example.digitallibrary.ui.components.DevotionalTopBar
import com.example.digitallibrary.ui.theme.FormatDocxColor
import com.example.digitallibrary.ui.theme.FormatPdfColor
import com.example.digitallibrary.ui.theme.FormatTxtColor
import com.example.digitallibrary.ui.theme.MutedGold
import com.example.digitallibrary.ui.theme.ParchmentBackground
import com.example.digitallibrary.ui.theme.ParchmentBorder
import com.example.digitallibrary.ui.theme.ParchmentSurface
import com.example.digitallibrary.ui.theme.TerracottaDark
import com.example.digitallibrary.ui.theme.TerracottaPrimary
import kotlinx.coroutines.launch

@Composable
fun SeriesDetailScreen(
    seriesId: String,
    bookRepository: BookRepository,
    onBackClick: () -> Unit,
    onBookClick: (String) -> Unit,
    onReadClick: (String) -> Unit
) {
    val currentLang by LanguageManager.currentLanguage.collectAsState()
    val strings = remember(currentLang) { LibraryStrings(currentLang) }

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
            DevotionalTopBar(
                title = series?.title ?: strings.seriesDetails,
                subtitle = "${books.size} ${strings.booksInSeries}",
                onBackClick = onBackClick,
                actions = {
                    if (books.isNotEmpty() && !isBatchDownloading) {
                        TextButton(
                            onClick = {
                                isMultiSelectMode = !isMultiSelectMode
                                if (!isMultiSelectMode) selectedBookIds = emptySet()
                            }
                        ) {
                            Text(
                                text = if (isMultiSelectMode) strings.cancel else strings.select,
                                color = TerracottaPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ParchmentBackground)
        ) {
            when {
                isLoading -> {
                    DevotionalLoadingView(message = strings.loadingBooks)
                }

                errorMessage != null -> {
                    DevotionalErrorView(
                        title = strings.unableToReach,
                        errorMessage = errorMessage ?: "",
                        onRetry = { loadSeriesData() }
                    )
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
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = ParchmentSurface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MutedGold.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(TerracottaPrimary.copy(alpha = 0.12f))
                                                .border(0.8.dp, TerracottaPrimary.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Layers,
                                                contentDescription = "Series",
                                                tint = TerracottaPrimary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column {
                                            Text(
                                                text = series?.title ?: "",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontFamily = FontFamily.Serif,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${books.size} ${strings.booksInSeries}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TerracottaDark,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    if (!series?.description.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = series?.description ?: "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                                    text = "${strings.downloading} (${bp.completedBooks}/${bp.totalBooks})...",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TerracottaPrimary
                                                )
                                                Text(
                                                    text = bp.currentBookTitle,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                                                color = TerracottaPrimary,
                                                trackColor = TerracottaPrimary.copy(alpha = 0.15f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                    }

                                    // Action buttons: Download entire series OR Download Selected
                                    if (isMultiSelectMode) {
                                        Button(
                                            onClick = {
                                                val selectedBooks = books.filter { it.id in selectedBookIds }
                                                startBatchDownload(selectedBooks)
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp),
                                            enabled = selectedBookIds.isNotEmpty() && !isBatchDownloading,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("${strings.downloadSelected} (${selectedBookIds.size})", fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Button(
                                            onClick = { startBatchDownload(books) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp),
                                            enabled = books.isNotEmpty() && !isBatchDownloading,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Download,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("${strings.downloadEntireSeries} (${books.size})", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // Section Title: Ordered Sequence
                        item {
                            Text(
                                text = strings.orderedSequence,
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
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
                                else -> TerracottaPrimary
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
                                colors = CardDefaults.cardColors(containerColor = ParchmentSurface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, ParchmentBorder)
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
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = TerracottaPrimary
                                            )
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }

                                    // Sequence badge (1, 2, 3...)
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(TerracottaPrimary.copy(alpha = 0.12f))
                                            .border(0.8.dp, TerracottaPrimary.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${book.seriesOrder.takeIf { it > 0 } ?: (books.indexOf(book) + 1)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TerracottaDark
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = book.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontFamily = FontFamily.Serif,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = book.author ?: strings.unknownAuthor,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(badgeColor.copy(alpha = 0.12f))
                                                    .border(0.5.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = book.fileType.uppercase(),
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
                                                strokeWidth = 3.dp,
                                                color = TerracottaPrimary
                                            )
                                        }

                                        isDownloaded -> {
                                            IconButton(onClick = { onReadClick(book.id) }) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoStories,
                                                    contentDescription = strings.readOffline,
                                                    tint = TerracottaPrimary
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
                                                    contentDescription = strings.downloadForOffline,
                                                    tint = TerracottaDark.copy(alpha = 0.8f)
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
