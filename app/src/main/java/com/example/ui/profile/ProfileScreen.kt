package com.example.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnonymousPersona
import com.example.data.model.PastelTheme
import com.example.ui.components.ConfessionCard
import com.example.ui.components.PastelThemeSelector
import com.example.ui.components.PersonaCustomizerDialog
import com.example.ui.viewmodel.ConfessionViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.HapticsHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ConfessionViewModel,
    onNavigateToDetail: (String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val myPostsState by viewModel.myPosts.collectAsState()
    val userSavedPostIds by viewModel.userSavedPostIds.collectAsState()
    val userReactions by viewModel.userReactions.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val appPastelTheme by viewModel.appPastelTheme.collectAsState()
    val currentUserId = viewModel.currentUserId

    var selectedTab by remember { mutableIntStateOf(0) }
    var showCustomizer by remember { mutableStateOf(false) }

    val activePersona = remember(userProfile) {
        userProfile?.let {
            AnonymousPersona(it.anonymousHandle, it.anonymousAvatar, it.anonymousColor)
        } ?: AnonymousPersona.generateNumbered(currentUserId)
    }

    val badgeColor = try {
        Color(android.graphics.Color.parseColor(activePersona.badgeColorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Anonymous Profile", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.toggleDarkMode() }) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
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
            // Profile Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(badgeColor.copy(alpha = 0.2f))
                            .border(2.dp, badgeColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = activePersona.avatarEmoji, fontSize = 34.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = activePersona.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = userProfile?.bio ?: "Whispering secrets anonymously...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val postsCount = (myPostsState as? UiState.Success)?.data?.size ?: 0
                        ProfileStatItem(label = "Confessions", value = postsCount.toString())
                        ProfileStatItem(label = "Bookmarks", value = userSavedPostIds.size.toString())
                        ProfileStatItem(label = "Encrypted", value = "100%")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            HapticsHelper.playClick(context)
                            showCustomizer = true
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Customize Persona & Bio")
                    }
                }
            }

            // Tab Row: My Confessions vs Settings
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        HapticsHelper.playLightTick(context)
                        selectedTab = 0
                    },
                    text = { Text("My Confessions", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        HapticsHelper.playLightTick(context)
                        selectedTab = 1
                    },
                    text = { Text("Settings & Privacy", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTab == 0) {
                // My Confessions List
                when (val state = myPostsState) {
                    is UiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is UiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Error: ${state.message}")
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
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "🤫", fontSize = 40.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("You haven't posted any confessions yet.", fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(posts, key = { it.postId }) { post ->
                                    ConfessionCard(
                                        post = post,
                                        isSaved = userSavedPostIds.contains(post.postId),
                                        userReaction = userReactions[post.postId],
                                        currentUserId = currentUserId,
                                        onReactionClick = { type -> viewModel.toggleReaction(post, type) },
                                        onCommentClick = { onNavigateToDetail(post.postId) },
                                        onSaveClick = { viewModel.toggleSavePost(post.postId) },
                                        onExportClick = { viewModel.openExportDialog(post) },
                                        onShareClick = { viewModel.openExportDialog(post) },
                                        onReportClick = { },
                                        onBlockClick = { },
                                        onDeleteClick = { viewModel.deletePost(post.postId) },
                                        modifier = Modifier.clickable { onNavigateToDetail(post.postId) }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Settings & Privacy Page
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            text = "Global App Theme",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        PastelThemeSelector(
                            selectedTheme = appPastelTheme,
                            onThemeSelected = { viewModel.setAppPastelTheme(it) }
                        )
                    }

                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }

                    item {
                        Text(
                            text = "Privacy & Safety",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Identity Encryption", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("UID is never exposed to public feeds", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }

                                HorizontalDivider()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Haptic Feedback Engine", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Vibrations for all interactive actions", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("Active", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = {
                                HapticsHelper.playHeavyClick(context)
                                onSignOut()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Out Session")
                        }
                    }
                }
            }
        }
    }

    if (showCustomizer) {
        PersonaCustomizerDialog(
            initialPersona = activePersona,
            initialBio = userProfile?.bio ?: "",
            onDismiss = { showCustomizer = false },
            onSave = { updatedPersona, updatedBio ->
                viewModel.updatePersona(updatedPersona, updatedBio)
            }
        )
    }
}

@Composable
fun ProfileStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
