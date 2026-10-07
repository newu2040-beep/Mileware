package com.example.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.auth.signOutSession
import com.example.ui.create.CreateConfessionScreen
import com.example.ui.details.PostDetailScreen
import com.example.ui.explore.ExploreScreen
import com.example.ui.export.CardExportDialog
import com.example.ui.home.HomeScreen
import com.example.ui.notifications.NotificationScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.saved.SavedConfessionsScreen
import com.example.ui.viewmodel.ConfessionViewModel
import com.example.util.HapticsHelper

sealed class Screen(val route: String, val title: String, val activeIcon: ImageVector, val inactiveIcon: ImageVector) {
    data object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Explore : Screen("explore", "Explore", Icons.Filled.Explore, Icons.Outlined.Explore)
    data object Create : Screen("create", "Whisper", Icons.Filled.Add, Icons.Outlined.Add)
    data object Saved : Screen("saved", "Saved", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder)
    data object Profile : Screen("profile", "Profile", Icons.Filled.Person, Icons.Outlined.PersonOutline)
    data object Detail : Screen("detail/{postId}", "Detail", Icons.Filled.Chat, Icons.Outlined.Chat) {
        fun createRoute(postId: String) = "detail/$postId"
    }
    data object Notifications : Screen("notifications", "Notifications", Icons.Filled.Notifications, Icons.Outlined.Notifications)
}

@Composable
fun AppNavigation(
    onSignOutSuccess: () -> Unit,
    viewModel: ConfessionViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val toastMessage by viewModel.toastMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val exportingPost by viewModel.exportingPost.collectAsState()
    val exportTheme by viewModel.exportTheme.collectAsState()
    val exportResolution by viewModel.exportResolution.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()

    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    val bottomNavScreens = listOf(
        Screen.Home,
        Screen.Explore,
        Screen.Create,
        Screen.Saved,
        Screen.Profile
    )

    val showBottomBar = bottomNavScreens.any { it.route == currentRoute }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    bottomNavScreens.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        if (screen == Screen.Create) {
                            // Prominent Center Add Button
                            NavigationBarItem(
                                selected = false,
                                onClick = {
                                    HapticsHelper.playHeavyClick(context)
                                    navController.navigate(Screen.Create.route) {
                                        launchSingleTop = true
                                    }
                                },
                                icon = {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Create Confession",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = "Whisper",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        } else {
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    HapticsHelper.playLightTick(context)
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(Screen.Home.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) screen.activeIcon else screen.inactiveIcon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { postId ->
                        navController.navigate(Screen.Detail.createRoute(postId))
                    },
                    onNavigateToCreate = {
                        navController.navigate(Screen.Create.route)
                    },
                    onNavigateToNotifications = {
                        navController.navigate(Screen.Notifications.route)
                    }
                )
            }

            composable(Screen.Explore.route) {
                ExploreScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { postId ->
                        navController.navigate(Screen.Detail.createRoute(postId))
                    }
                )
            }

            composable(Screen.Create.route) {
                CreateConfessionScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Saved.route) {
                SavedConfessionsScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { postId ->
                        navController.navigate(Screen.Detail.createRoute(postId))
                    }
                )
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { postId ->
                        navController.navigate(Screen.Detail.createRoute(postId))
                    },
                    onSignOut = {
                        val credentialManager = CredentialManager.create(context)
                        signOutSession(context, credentialManager, coroutineScope) {
                            onSignOutSuccess()
                        }
                    }
                )
            }

            composable(Screen.Notifications.route) {
                NotificationScreen(
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToPost = { postId ->
                        navController.navigate(Screen.Detail.createRoute(postId))
                    }
                )
            }

            composable(
                route = Screen.Detail.route,
                arguments = listOf(navArgument("postId") { type = NavType.StringType })
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId") ?: ""
                PostDetailScreen(
                    postId = postId,
                    viewModel = viewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }

    // Global High-Res Export Dialog
    exportingPost?.let { post ->
        CardExportDialog(
            post = post,
            selectedTheme = exportTheme,
            selectedResolution = exportResolution,
            isExporting = isExporting,
            onThemeSelected = { viewModel.setExportTheme(it) },
            onResolutionSelected = { viewModel.setExportResolution(it) },
            onSaveToGallery = {
                viewModel.saveExportCard { }
            },
            onShare = {
                viewModel.shareExportCard { shareIntent ->
                    context.startActivity(shareIntent)
                }
            },
            onDismiss = { viewModel.closeExportDialog() }
        )
    }
}
