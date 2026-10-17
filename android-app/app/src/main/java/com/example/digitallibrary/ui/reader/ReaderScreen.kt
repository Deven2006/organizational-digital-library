package com.example.digitallibrary.ui.reader

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.digitallibrary.data.model.Book
import com.example.digitallibrary.data.repository.BookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookId: String,
    bookRepository: BookRepository,
    onBackClick: () -> Unit
) {
    var book by remember { mutableStateOf<Book?>(null) }
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var textContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadingStatus by remember { mutableStateOf("Loading document...") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isOfflineMode by remember { mutableStateOf(false) }
    var initialPage by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()

    fun loadDocument() {
        scope.launch {
            isLoading = true
            errorMessage = null

            // 1. Check if book is stored locally in persistent offline downloads
            val localPersistentFile = bookRepository.getLocalFileForBook(bookId)
            val savedProgress = bookRepository.progressRepository.getLocalProgress(bookId)
            if (savedProgress != null) {
                initialPage = savedProgress.lastPage
            }

            if (localPersistentFile != null && localPersistentFile.exists()) {
                isOfflineMode = true
                val bookResult = bookRepository.getBookById(bookId)
                val currentBook = bookResult.getOrNull()
                book = currentBook

                when {
                    currentBook?.isPdf == true || localPersistentFile.extension.equals("pdf", ignoreCase = true) -> {
                        pdfFile = localPersistentFile
                    }

                    currentBook?.isDocx == true || localPersistentFile.extension.equals("docx", ignoreCase = true) -> {
                        if (!currentBook?.parsedContent.isNullOrBlank()) {
                            textContent = currentBook?.parsedContent
                        } else {
                            withContext(Dispatchers.IO) {
                                textContent = localPersistentFile.readText()
                            }
                        }
                    }

                    else -> {
                        withContext(Dispatchers.IO) {
                            textContent = localPersistentFile.readText()
                        }
                    }
                }

                isLoading = false
                return@launch
            }

            // 2. Online fetch flow
            loadingStatus = "Retrieving book details..."
            val bookResult = bookRepository.getBookById(bookId)
            if (bookResult.isFailure) {
                errorMessage = bookResult.exceptionOrNull()?.message ?: "Failed to retrieve book metadata."
                isLoading = false
                return@launch
            }

            val currentBook = bookResult.getOrThrow()
            book = currentBook

            when {
                // PDF Document: Download file to local cache and render via PdfRenderer
                currentBook.isPdf -> {
                    loadingStatus = "Downloading PDF from secure storage..."
                    val downloadResult = bookRepository.downloadDocument(currentBook.storagePath)
                    if (downloadResult.isSuccess) {
                        pdfFile = downloadResult.getOrNull()
                    } else {
                        errorMessage = downloadResult.exceptionOrNull()?.message
                            ?: "Failed to download PDF document from server."
                    }
                }

                // DOCX Document: Render extracted readable text stored in the record
                currentBook.isDocx -> {
                    if (!currentBook.parsedContent.isNullOrBlank()) {
                        textContent = currentBook.parsedContent
                    } else {
                        errorMessage = "No extracted text found for this Word document. Please ensure it was parsed during upload."
                    }
                }

                // TXT Document: Render stored parsed text or fetch file
                currentBook.isTxt -> {
                    if (!currentBook.parsedContent.isNullOrBlank()) {
                        textContent = currentBook.parsedContent
                    } else {
                        loadingStatus = "Downloading text document..."
                        val downloadResult = bookRepository.downloadDocument(currentBook.storagePath)
                        if (downloadResult.isSuccess) {
                            val file = downloadResult.getOrThrow()
                            withContext(Dispatchers.IO) {
                                textContent = file.readText()
                            }
                        } else {
                            errorMessage = downloadResult.exceptionOrNull()?.message
                                ?: "Failed to download text document."
                        }
                    }
                }

                else -> {
                    errorMessage = "Unsupported document format: ${currentBook.fileType}"
                }
            }

            isLoading = false
        }
    }

    LaunchedEffect(bookId) {
        loadDocument()
    }

    // Save reading progress when text content is loaded
    LaunchedEffect(textContent) {
        if (textContent != null) {
            bookRepository.progressRepository.saveProgress(
                bookId = bookId,
                lastPage = 0,
                progressPercent = 100,
                isCompleted = true
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = book?.title ?: "Document Reader",
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isOfflineMode) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.OfflinePin,
                                    contentDescription = "Offline",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Reading from Offline Storage",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
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
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = loadingStatus,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                errorMessage != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = "Unable to Open Document",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(onClick = { loadDocument() }) {
                                    Text("Retry Loading")
                                }
                            }
                        }
                    }
                }

                // Render PDF with progress tracking
                pdfFile != null -> {
                    PdfViewer(
                        pdfFile = pdfFile!!,
                        initialPage = initialPage,
                        onPageChanged = { pageIndex, totalPages ->
                            if (totalPages > 0) {
                                val percent = ((pageIndex + 1) * 100) / totalPages
                                val completed = (pageIndex + 1) >= totalPages
                                bookRepository.progressRepository.saveProgress(
                                    bookId = bookId,
                                    lastPage = pageIndex,
                                    progressPercent = percent,
                                    isCompleted = completed
                                )
                            }
                        }
                    )
                }

                // Render Text / DOCX readable content
                textContent != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 24.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(22.dp)) {
                                Text(
                                    text = book?.title ?: "",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                if (!book?.author.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "By ${book?.author}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Text(
                                    text = textContent ?: "",
                                    style = MaterialTheme.typography.bodyLarge,
                                    lineHeight = 26.sp,
                                    letterSpacing = 0.2.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
