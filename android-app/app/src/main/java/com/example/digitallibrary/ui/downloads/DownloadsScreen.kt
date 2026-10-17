package com.example.digitallibrary.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.digitallibrary.data.model.DownloadedBook
import com.example.digitallibrary.data.repository.BookRepository
import com.example.digitallibrary.ui.common.LanguageManager
import com.example.digitallibrary.ui.common.LibraryStrings
import com.example.digitallibrary.ui.components.BookCoverCard
import com.example.digitallibrary.ui.components.DevotionalEmptyView
import com.example.digitallibrary.ui.components.DevotionalTopBar
import com.example.digitallibrary.ui.theme.FormatDocxColor
import com.example.digitallibrary.ui.theme.FormatPdfColor
import com.example.digitallibrary.ui.theme.FormatTxtColor
import com.example.digitallibrary.ui.theme.MutedGold
import com.example.digitallibrary.ui.theme.ParchmentBackground
import com.example.digitallibrary.ui.theme.ParchmentBorder
import com.example.digitallibrary.ui.theme.ParchmentSurface
import com.example.digitallibrary.ui.theme.SuccessGreen
import com.example.digitallibrary.ui.theme.TerracottaDark
import com.example.digitallibrary.ui.theme.TerracottaPrimary
import kotlinx.coroutines.launch

@Composable
fun DownloadsScreen(
    bookRepository: BookRepository,
    onBackClick: () -> Unit,
    onReadClick: (String) -> Unit
) {
    val currentLang by LanguageManager.currentLanguage.collectAsState()
    val strings = remember(currentLang) { LibraryStrings(currentLang) }

    val downloadManager = bookRepository.downloadManager
    val progressRepository = bookRepository.progressRepository
    val scope = rememberCoroutineScope()

    var downloadedBooks by remember { mutableStateOf<List<DownloadedBook>>(emptyList()) }
    var bookToDelete by remember { mutableStateOf<DownloadedBook?>(null) }
    var isCheckingUpdates by remember { mutableStateOf(false) }

    fun refreshDownloads() {
        downloadedBooks = downloadManager.getDownloadedBooks()
    }

    LaunchedEffect(Unit) {
        refreshDownloads()
        // Check for updates if online
        scope.launch {
            isCheckingUpdates = true
            val remoteResult = bookRepository.getPublishedBooks()
            if (remoteResult.isSuccess) {
                downloadedBooks = downloadManager.checkForUpdates(remoteResult.getOrThrow())
            }
            isCheckingUpdates = false
        }
    }

    Scaffold(
        topBar = {
            DevotionalTopBar(
                title = strings.offlineDownloads,
                subtitle = "${downloadedBooks.size} ${strings.booksAvailableOffline}",
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ParchmentBackground)
        ) {
            if (downloadedBooks.isEmpty()) {
                DevotionalEmptyView(
                    title = strings.noDownloads,
                    description = strings.noDownloadsDesc,
                    icon = Icons.Default.DownloadDone
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(downloadedBooks, key = { it.bookId }) { downloadedBook ->
                        val progress = progressRepository.getLocalProgress(downloadedBook.bookId)
                        val badgeColor = when {
                            downloadedBook.isPdf -> FormatPdfColor
                            downloadedBook.isDocx -> FormatDocxColor
                            downloadedBook.isTxt -> FormatTxtColor
                            else -> TerracottaPrimary
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ParchmentSurface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, ParchmentBorder)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Book miniature thumbnail
                                    BookCoverCard(
                                        title = downloadedBook.title,
                                        author = downloadedBook.author,
                                        fileType = downloadedBook.fileType,
                                        seriesOrder = downloadedBook.seriesOrder,
                                        modifier = Modifier
                                            .width(48.dp)
                                            .height(68.dp)
                                    )

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = downloadedBook.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontFamily = FontFamily.Serif,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Spacer(modifier = Modifier.height(3.dp))

                                        Text(
                                            text = "${downloadedBook.author ?: strings.unknownAuthor} • ${downloadedBook.formattedFileSize}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(badgeColor.copy(alpha = 0.12f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = downloadedBook.fileType.uppercase(),
                                                color = badgeColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { bookToDelete = downloadedBook },
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = strings.remove,
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
                                        )
                                    }
                                }

                                // Update available notice if newer version on server
                                if (downloadedBook.hasUpdateAvailable) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFFEF3C7))
                                            .border(0.8.dp, Color(0xFFF59E0B), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SystemUpdate,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = strings.newVersionAvailable,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF92400E),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Reading Progress Bar
                                if (progress != null && progress.progressPercent > 0) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (progress.isCompleted) strings.completed else strings.readingProgress,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (progress.isCompleted) SuccessGreen else TerracottaDark,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "${progress.progressPercent}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = TerracottaPrimary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { progress.progressPercent / 100f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = TerracottaPrimary,
                                            trackColor = TerracottaPrimary.copy(alpha = 0.15f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Read Offline Button
                                Button(
                                    onClick = { onReadClick(downloadedBook.bookId) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoStories,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = strings.readOffline,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Confirm Delete Dialog
            if (bookToDelete != null) {
                AlertDialog(
                    onDismissRequest = { bookToDelete = null },
                    title = {
                        Text(
                            text = strings.removeDownloadTitle,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = strings.removeDownloadMsg,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                bookToDelete?.let {
                                    downloadManager.removeDownload(it.bookId)
                                    refreshDownloads()
                                }
                                bookToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(strings.remove, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { bookToDelete = null }) {
                            Text(strings.cancel)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = ParchmentSurface
                )
            }
        }
    }
}
