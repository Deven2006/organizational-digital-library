package com.example.digitallibrary.ui.downloads

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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.digitallibrary.data.model.DownloadedBook
import com.example.digitallibrary.data.repository.BookRepository
import com.example.digitallibrary.ui.theme.FormatDocxColor
import com.example.digitallibrary.ui.theme.FormatPdfColor
import com.example.digitallibrary.ui.theme.FormatTxtColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    bookRepository: BookRepository,
    onBackClick: () -> Unit,
    onReadClick: (String) -> Unit
) {
    val downloadManager = bookRepository.downloadManager
    val progressRepository = bookRepository.progressRepository
    val scope = rememberCoroutineScope()

    var downloadedBooks by remember { mutableStateOf<List<DownloadedBook>>(emptyMap<String, String>().let { emptyList() }) }
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
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Downloaded Books",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${downloadedBooks.size} books available offline",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            if (downloadedBooks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = "No downloads",
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Downloaded Books",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Download books while online to read them anywhere, even in airplane mode.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(downloadedBooks, key = { it.bookId }) { downloadedBook ->
                        val progress = progressRepository.getLocalProgress(downloadedBook.bookId)
                        val badgeColor = when {
                            downloadedBook.isPdf -> FormatPdfColor
                            downloadedBook.isDocx -> FormatDocxColor
                            downloadedBook.isTxt -> FormatTxtColor
                            else -> MaterialTheme.colorScheme.primary
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(badgeColor.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = "Format",
                                            tint = badgeColor,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = downloadedBook.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${downloadedBook.author ?: "Unknown Author"} • ${downloadedBook.formattedFileSize}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }

                                    IconButton(
                                        onClick = { bookToDelete = downloadedBook },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Remove download",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                // Update available notice
                                if (downloadedBook.hasUpdateAvailable) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFFEF3C7))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SystemUpdate,
                                            contentDescription = "Update available",
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "New version available on server",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF92400E),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // Reading Progress Bar
                                if (progress != null && progress.progressPercent > 0) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (progress.isCompleted) "Completed" else "Reading Progress",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (progress.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                            )
                                            Text(
                                                text = "${progress.progressPercent}%",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = { progress.progressPercent / 100f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(5.dp)
                                                .clip(RoundedCornerShape(3.dp)),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Read Offline Button
                                Button(
                                    onClick = { onReadClick(downloadedBook.bookId) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoStories,
                                        contentDescription = "Read Offline",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Read Offline",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
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
                    title = { Text("Remove Download?") },
                    text = {
                        Text("This will remove \"${bookToDelete?.title}\" from your offline storage. You can download it again whenever you are online.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                bookToDelete?.let {
                                    downloadManager.removeDownload(it.bookId)
                                    refreshDownloads()
                                }
                                bookToDelete = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Remove")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { bookToDelete = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
