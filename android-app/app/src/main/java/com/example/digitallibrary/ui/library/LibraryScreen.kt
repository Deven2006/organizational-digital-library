package com.example.digitallibrary.ui.library

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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.digitallibrary.ui.common.AppLanguage
import com.example.digitallibrary.ui.common.LanguageManager
import com.example.digitallibrary.ui.common.LibraryStrings
import com.example.digitallibrary.ui.components.ContinueReadingCard
import com.example.digitallibrary.ui.components.DevotionalBookItem
import com.example.digitallibrary.ui.components.DevotionalEmptyView
import com.example.digitallibrary.ui.components.DevotionalErrorView
import com.example.digitallibrary.ui.components.DevotionalLoadingView
import com.example.digitallibrary.ui.components.DevotionalTopBar
import com.example.digitallibrary.ui.theme.MutedGold
import com.example.digitallibrary.ui.theme.ParchmentBackground
import com.example.digitallibrary.ui.theme.ParchmentBorder
import com.example.digitallibrary.ui.theme.ParchmentSurface
import com.example.digitallibrary.ui.theme.TerracottaDark
import com.example.digitallibrary.ui.theme.TerracottaPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onBookClick: (String) -> Unit,
    onSeriesClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onReadClick: (String) -> Unit = onBookClick
) {
    val context = LocalContext.current
    val currentLang by LanguageManager.currentLanguage.collectAsState()
    val strings = remember(currentLang) { LibraryStrings(currentLang) }

    val state by viewModel.uiState.collectAsState()
    val filteredBooks by viewModel.filteredBooks.collectAsState()

    Scaffold(
        topBar = {
            DevotionalTopBar(
                title = strings.appTitle,
                subtitle = strings.appSubtitle,
                actions = {
                    // Quick language toggle
                    IconButton(onClick = {
                        val next = if (currentLang == AppLanguage.ENGLISH) AppLanguage.MARATHI else AppLanguage.ENGLISH
                        LanguageManager.setLanguage(context, next)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Switch Language",
                            tint = TerracottaPrimary
                        )
                    }

                    IconButton(onClick = onSeriesClick) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = strings.seriesCollections,
                            tint = TerracottaPrimary
                        )
                    }
                    IconButton(onClick = onDownloadsClick) {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = strings.offlineDownloads,
                            tint = TerracottaPrimary
                        )
                    }
                    IconButton(onClick = { viewModel.loadBooks() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = TerracottaPrimary
                        )
                    }
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = strings.profileAndSettings,
                            tint = TerracottaPrimary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ParchmentBackground)
        ) {
            // Quick Navigation Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = true,
                    onClick = { /* already in all catalog */ },
                    label = { Text(strings.allBooks, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TerracottaPrimary.copy(alpha = 0.15f),
                        selectedLabelColor = TerracottaDark
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = true,
                        borderColor = TerracottaPrimary.copy(alpha = 0.4f)
                    )
                )

                FilterChip(
                    selected = false,
                    onClick = onSeriesClick,
                    label = { Text(strings.seriesCollections) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = TerracottaPrimary
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = ParchmentSurface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = false,
                        borderColor = ParchmentBorder
                    )
                )

                FilterChip(
                    selected = false,
                    onClick = onDownloadsClick,
                    label = { Text(strings.offlineDownloads) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.DownloadDone,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = TerracottaPrimary
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = ParchmentSurface,
                        labelColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = false,
                        borderColor = ParchmentBorder
                    )
                )
            }

            // Search Input Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text(strings.searchPlaceholder, fontSize = 14.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TerracottaPrimary
                    )
                },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = strings.clearSearch,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ParchmentSurface,
                    unfocusedContainerColor = ParchmentSurface,
                    focusedBorderColor = TerracottaPrimary,
                    unfocusedBorderColor = ParchmentBorder,
                    cursorColor = TerracottaPrimary
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
                        DevotionalLoadingView(message = strings.loadingBooks)
                    }

                    state.errorMessage != null && state.books.isEmpty() -> {
                        DevotionalErrorView(
                            title = strings.unableToReach,
                            errorMessage = state.errorMessage ?: "",
                            onRetry = { viewModel.loadBooks() },
                            onSecondaryAction = onDownloadsClick,
                            secondaryActionLabel = strings.goToDownloads
                        )
                    }

                    filteredBooks.isEmpty() -> {
                        DevotionalEmptyView(
                            title = if (state.searchQuery.isNotEmpty()) strings.noBooksFound else strings.appTitle,
                            description = if (state.searchQuery.isNotEmpty()) strings.noBooksFoundDesc else strings.emptyLibraryDesc
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            // Prominent Continue Reading Hero Card if user has an active book
                            if (state.continueReadingBook != null && state.continueReadingProgress != null && state.searchQuery.isBlank()) {
                                item(key = "continue_reading_hero") {
                                    val continueBook = state.continueReadingBook!!
                                    val continueProgress = state.continueReadingProgress!!

                                    ContinueReadingCard(
                                        book = continueBook,
                                        progress = continueProgress,
                                        strings = strings,
                                        onClick = { onReadClick(continueBook.id) },
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                            }

                            // Section header if Continue Reading is present
                            if (state.continueReadingBook != null && state.searchQuery.isBlank()) {
                                item(key = "catalog_section_header") {
                                    Text(
                                        text = strings.allBooks,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }

                            items(filteredBooks, key = { it.id }) { book ->
                                val isDownloaded = book.id in state.downloadedBookIds
                                DevotionalBookItem(
                                    book = book,
                                    strings = strings,
                                    isDownloaded = isDownloaded,
                                    onClick = { onBookClick(book.id) }
                                )
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
