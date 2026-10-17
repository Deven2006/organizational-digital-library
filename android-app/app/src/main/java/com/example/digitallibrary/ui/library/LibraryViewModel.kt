package com.example.digitallibrary.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.digitallibrary.data.model.Book
import com.example.digitallibrary.data.model.ReadingProgress
import com.example.digitallibrary.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val continueReadingBook: Book? = null,
    val continueReadingProgress: ReadingProgress? = null,
    val downloadedBookIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = ""
)

class LibraryViewModel(
    private val bookRepository: BookRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    val filteredBooks: StateFlow<List<Book>> = _uiState.combine(_uiState) { state, _ ->
        val query = state.searchQuery.trim().lowercase()
        if (query.isBlank()) {
            state.books
        } else {
            state.books.filter { book ->
                book.title.lowercase().contains(query) ||
                        (book.author?.lowercase()?.contains(query) == true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadBooks()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun loadBooks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = bookRepository.getPublishedBooks()
            if (result.isSuccess) {
                val fetchedBooks = result.getOrDefault(emptyList())

                // 1. Resolve Continue Reading book from local progress
                val allProgress = bookRepository.progressRepository.getAllLocalProgress()
                val recentProgress = allProgress.firstOrNull { it.progressPercent > 0 || it.lastPage > 0 }
                val continueBook = if (recentProgress != null) {
                    fetchedBooks.find { it.id == recentProgress.bookId }
                        ?: bookRepository.getBookById(recentProgress.bookId).getOrNull()
                } else null

                // 2. Resolve downloaded books for status indicators
                val downloadedIds = bookRepository.downloadManager.getDownloadedBooks()
                    .map { it.bookId }
                    .toSet()

                _uiState.update {
                    it.copy(
                        books = fetchedBooks,
                        continueReadingBook = continueBook,
                        continueReadingProgress = recentProgress,
                        downloadedBookIds = downloadedIds,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } else {
                val error = result.exceptionOrNull()?.message ?: "Failed to fetch library books."
                // Also check if offline cached continue reading exists
                val allProgress = bookRepository.progressRepository.getAllLocalProgress()
                val recentProgress = allProgress.firstOrNull { it.progressPercent > 0 || it.lastPage > 0 }
                val continueBook = if (recentProgress != null) {
                    bookRepository.getBookById(recentProgress.bookId).getOrNull()
                } else null

                val downloadedIds = bookRepository.downloadManager.getDownloadedBooks()
                    .map { it.bookId }
                    .toSet()

                _uiState.update {
                    it.copy(
                        continueReadingBook = continueBook,
                        continueReadingProgress = recentProgress,
                        downloadedBookIds = downloadedIds,
                        isLoading = false,
                        errorMessage = error
                    )
                }
            }
        }
    }
}
