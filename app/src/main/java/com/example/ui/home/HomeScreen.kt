package com.example.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnonymousPersona
import com.example.data.model.ConfessionPost
import com.example.data.model.ReactionModel
import com.example.data.model.ReportModel
import com.example.ui.components.AnonymousPersonaHeader
import com.example.ui.components.ConfessionCard
import com.example.ui.components.PersonaCustomizerDialog
import com.example.ui.viewmodel.ConfessionViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.HapticsHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ConfessionViewModel,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val feedState by viewModel.feedPosts.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedFeedTab by viewModel.selectedFeedTab.collectAsState()
    val userReactions by viewModel.userReactions.collectAsState()
    val userSavedPostIds by viewModel.userSavedPostIds.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val currentUserId = viewModel.currentUserId

    var showPersonaCustomizer by remember { mutableStateOf(false) }
    var reportTargetPostId by remember { mutableStateOf<String?>(null) }
    var reportReason by remember { mutableStateOf("Spam") }
    var reportNotes by remember { mutableStateOf("") }

    val activePersona = remember(userProfile) {
        userProfile?.let {
            AnonymousPersona(it.anonymousHandle, it.anonymousAvatar, it.anonymousColor)
        } ?: AnonymousPersona.generateNumbered(currentUserId)
    }

    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MilesAre",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ANONYMOUS",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    // Persona preview button
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                HapticsHelper.playClick(context)
                                showPersonaCustomizer = true
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = activePersona.avatarEmoji, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = activePersona.name.take(12),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Dark/Light Theme Toggle
                    IconButton(
                        onClick = { viewModel.toggleDarkMode() }
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }

                    // Notifications Button
                    IconButton(
                        onClick = {
                            HapticsHelper.playLightTick(context)
                            onNavigateToNotifications()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Category horizontal filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ConfessionPost.CATEGORIES.forEach { category ->
                    val isSelected = category == selectedCategory
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectCategory(category) },
                        label = {
                            Text(
                                text = category,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            // Feed Section Tab Row: For You, Latest, Trending, Popular
            val tabs = listOf(
                "latest" to "✨ Latest",
                "trending" to "🔥 Trending",
                "popular" to "💬 Popular",
                "for_you" to "🎯 For You"
            )

            TabRow(
                selectedTabIndex = tabs.indexOfFirst { it.first == selectedFeedTab }.coerceAtLeast(0),
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
            ) {
                tabs.forEach { (tabKey, tabLabel) ->
                    val isSelected = selectedFeedTab == tabKey
                    Tab(
                        selected = isSelected,
                        onClick = { viewModel.selectFeedTab(tabKey) },
                        text = {
                            Text(
                                text = tabLabel,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Feed Content
            when (val state = feedState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Unable to load confessions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.selectCategory("All") }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                is UiState.Success -> {
                    val posts = state.data
                    if (posts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(text = "🤫", fontSize = 48.sp)
                                Text(
                                    text = "No confessions here yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Be the first to share an anonymous thought or secret with the world.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = onNavigateToCreate,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Post First Secret")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = posts,
                                key = { it.postId }
                            ) { post ->
                                val isSaved = userSavedPostIds.contains(post.postId)
                                val userReaction = userReactions[post.postId]

                                ConfessionCard(
                                    post = post,
                                    isSaved = isSaved,
                                    userReaction = userReaction,
                                    currentUserId = currentUserId,
                                    onReactionClick = { type ->
                                        viewModel.toggleReaction(post, type)
                                    },
                                    onCommentClick = {
                                        onNavigateToDetail(post.postId)
                                    },
                                    onSaveClick = {
                                        viewModel.toggleSavePost(post.postId)
                                    },
                                    onExportClick = {
                                        viewModel.openExportDialog(post)
                                    },
                                    onShareClick = {
                                        viewModel.openExportDialog(post)
                                    },
                                    onReportClick = {
                                        reportTargetPostId = post.postId
                                    },
                                    onBlockClick = {
                                        viewModel.blockUser(post.anonymousId)
                                    },
                                    onDeleteClick = {
                                        viewModel.deletePost(post.postId)
                                    },
                                    modifier = Modifier.clickable {
                                        onNavigateToDetail(post.postId)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Persona Customizer Dialog
    if (showPersonaCustomizer) {
        PersonaCustomizerDialog(
            initialPersona = activePersona,
            initialBio = userProfile?.bio ?: "",
            onDismiss = { showPersonaCustomizer = false },
            onSave = { updatedPersona, updatedBio ->
                viewModel.updatePersona(updatedPersona, updatedBio)
            }
        )
    }

    // Report Dialog
    if (reportTargetPostId != null) {
        AlertDialog(
            onDismissRequest = { reportTargetPostId = null },
            title = { Text("Report Confession") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select a reason for reporting this post:", style = MaterialTheme.typography.bodyMedium)
                    val reasons = listOf("Spam", "Harassment", "Hate Speech", "Sexual Content", "Violence", "Self-harm", "Other")
                    reasons.forEach { reason ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { reportReason = reason }
                        ) {
                            RadioButton(
                                selected = reportReason == reason,
                                onClick = { reportReason = reason }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(reason)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        reportTargetPostId?.let { postId ->
                            viewModel.reportContent("post", postId, reportReason, reportNotes)
                        }
                        reportTargetPostId = null
                    }
                ) {
                    Text("Submit Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { reportTargetPostId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
