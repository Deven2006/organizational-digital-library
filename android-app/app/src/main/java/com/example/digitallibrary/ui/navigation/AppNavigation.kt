package com.example.digitallibrary.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.digitallibrary.data.repository.AuthRepository
import com.example.digitallibrary.data.repository.BookRepository
import com.example.digitallibrary.ui.auth.LoginScreen
import com.example.digitallibrary.ui.auth.LoginViewModel
import com.example.digitallibrary.ui.common.LanguageManager
import com.example.digitallibrary.ui.detail.BookDetailScreen
import com.example.digitallibrary.ui.downloads.DownloadsScreen
import com.example.digitallibrary.ui.library.LibraryScreen
import com.example.digitallibrary.ui.library.LibraryViewModel
import com.example.digitallibrary.ui.profile.ProfileScreen
import com.example.digitallibrary.ui.reader.ReaderScreen
import com.example.digitallibrary.ui.series.SeriesDetailScreen
import com.example.digitallibrary.ui.series.SeriesListScreen

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Library : Screen("library")
    data object BookDetail : Screen("book_detail/{bookId}") {
        fun createRoute(bookId: String) = "book_detail/$bookId"
    }
    data object Reader : Screen("reader/{bookId}") {
        fun createRoute(bookId: String) = "reader/$bookId"
    }
    data object SeriesList : Screen("series_list")
    data object SeriesDetail : Screen("series_detail/{seriesId}") {
        fun createRoute(seriesId: String) = "series_detail/$seriesId"
    }
    data object Downloads : Screen("downloads")
    data object Profile : Screen("profile")
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val authRepository = AuthRepository(context)
    val bookRepository = BookRepository(context, authRepository)

    LanguageManager.init(context)

    val navController = rememberNavController()
    val isLoggedIn by authRepository.isLoggedIn.collectAsState()

    val startDestination = if (isLoggedIn) Screen.Library.route else Screen.Login.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            val loginViewModel: LoginViewModel = viewModel {
                LoginViewModel(authRepository)
            }
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(Screen.Library.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Library.route) {
            val libraryViewModel: LibraryViewModel = viewModel {
                LibraryViewModel(bookRepository)
            }
            LibraryScreen(
                viewModel = libraryViewModel,
                onBookClick = { bookId ->
                    navController.navigate(Screen.BookDetail.createRoute(bookId))
                },
                onReadClick = { bookId ->
                    navController.navigate(Screen.Reader.createRoute(bookId))
                },
                onSeriesClick = {
                    navController.navigate(Screen.SeriesList.route)
                },
                onDownloadsClick = {
                    navController.navigate(Screen.Downloads.route)
                },
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }

        composable(Screen.SeriesList.route) {
            SeriesListScreen(
                bookRepository = bookRepository,
                onBackClick = { navController.popBackStack() },
                onSeriesClick = { seriesId ->
                    navController.navigate(Screen.SeriesDetail.createRoute(seriesId))
                }
            )
        }

        composable(
            route = Screen.SeriesDetail.route,
            arguments = listOf(navArgument("seriesId") { type = NavType.StringType })
        ) { backStackEntry ->
            val seriesId = backStackEntry.arguments?.getString("seriesId") ?: ""
            SeriesDetailScreen(
                seriesId = seriesId,
                bookRepository = bookRepository,
                onBackClick = { navController.popBackStack() },
                onBookClick = { bookId ->
                    navController.navigate(Screen.BookDetail.createRoute(bookId))
                },
                onReadClick = { bookId ->
                    navController.navigate(Screen.Reader.createRoute(bookId))
                }
            )
        }

        composable(Screen.Downloads.route) {
            DownloadsScreen(
                bookRepository = bookRepository,
                onBackClick = { navController.popBackStack() },
                onReadClick = { bookId ->
                    navController.navigate(Screen.Reader.createRoute(bookId))
                }
            )
        }

        composable(
            route = Screen.BookDetail.route,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            BookDetailScreen(
                bookId = bookId,
                bookRepository = bookRepository,
                onBackClick = { navController.popBackStack() },
                onReadClick = { id ->
                    navController.navigate(Screen.Reader.createRoute(id))
                }
            )
        }

        composable(
            route = Screen.Reader.route,
            arguments = listOf(navArgument("bookId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getString("bookId") ?: ""
            ReaderScreen(
                bookId = bookId,
                bookRepository = bookRepository,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                authRepository = authRepository,
                onBackClick = { navController.popBackStack() },
                onLogoutClick = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
