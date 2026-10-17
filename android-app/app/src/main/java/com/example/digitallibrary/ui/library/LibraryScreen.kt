package com.example.digitallibrary.ui.library

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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.digitallibrary.data.model.Book
import com.example.digitallibrary.ui.theme.FormatDocxColor
import com.example.digitallibrary.ui.theme.FormatPdfColor
import com.example.digitallibrary.ui.theme.FormatTxtColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onBookClick: (String) -> Unit,
    onSeriesClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val filteredBooks by viewModel.filteredBooks.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Organizational Digital Library",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Official Documents & Publications",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSeriesClick) {
                        Icon(imageVector = Icons.Default.Layers, contentDescription = "Series Collections")
                    }
                    IconButton(onClick = onDownloadsClick) {
                        Icon(imageVector = Icons.Default.DownloadDone, contentDescription = "Downloaded Books")
                    }
                    IconButton(onClick = { viewModel.loadBooks() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Catalog")
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "User Profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Quick Navigation Chips (Catalog, Series, Downloads)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = true,
                    onClick = { /* already in catalog */ },
                    label = { Text("All Books") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )

                FilterChip(
                    selected = false,
                    onClick = onSeriesClick,
                    label = { Text("Series Collections") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )

                FilterChip(
                    selected = false,
                    onClick = onDownloadsClick,
                    label = { Text("Offline Downloads") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.DownloadDone, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                )
            }

            // Search Input Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search by book title or author...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Content Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.isLoading && state.books.isEmpty() -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading published library books...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    state.errorMessage != null && state.books.isEmpty() -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WifiOff,
                                    contentDescription = "Offline / Error",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Unable to Reach Library",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = state.errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.loadBooks() },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Retry")
                                    }
                                    Button(
                                        onClick = onDownloadsClick,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Go to Downloads")
                                    }
                                }
                            }
                        }
                    }

                    filteredBooks.isEmpty() -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = "Empty",
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (state.searchQuery.isNotEmpty()) "No books found" else "Library is currently empty",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (state.searchQuery.isNotEmpty())
                                    "No published documents match \"${state.searchQuery}\""
                                else
                                    "No documents have been published by the administrator yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            items(filteredBooks, key = { it.id }) { book ->
                                BookListItem(book = book, onClick = { onBookClick(book.id) })
                            }
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookListItem(
    book: Book,
    onClick: () -> Unit
) {
    val badgeColor = when {
        book.isPdf -> FormatPdfColor
        book.isDocx -> FormatDocxColor
        book.isTxt -> FormatTxtColor
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book Icon with format badge color
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = "Format Icon",
                    tint = badgeColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Book Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = book.author ?: "Unknown Author",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (book.seriesId != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Series Vol. ${book.seriesOrder}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Format Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = book.fileType.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
