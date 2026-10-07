package com.example.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
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
import com.example.data.model.ConfessionPost
import com.example.data.model.PastelTheme
import com.example.ui.components.ConfessionCard
import com.example.ui.viewmodel.ConfessionViewModel
import com.example.ui.viewmodel.UiState
import com.example.util.HapticsHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    viewModel: ConfessionViewModel,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val feedState by viewModel.feedPosts.collectAsState()
    val userReactions by viewModel.userReactions.collectAsState()
    val userSavedPostIds by viewModel.userSavedPostIds.collectAsState()
    val currentUserId = viewModel.currentUserId

    var searchInput by remember { mutableStateOf(searchQuery) }

    val trendingTopics = listOf(
        Triple("College Secrets", "🏫", PastelTheme.LAVENDER_MIST),
        Triple("Late Night Thoughts", "🌙", PastelTheme.MIDNIGHT_VELVET),
        Triple("Unsent Love Letters", "💌", PastelTheme.ROSE_QUARTZ),
        Triple("Workplace Drama", "💼", PastelTheme.SKY_CYAN),
        Triple("Deep Life Regrets", "🍂", PastelTheme.PEACH_SUNSET),
        Triple("Ghost Encounters", "👻", PastelTheme.MINT_BREEZE)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Explore & Discover",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
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
            // Search Bar
            OutlinedTextField(
                value = searchInput,
                onValueChange = {
                    searchInput = it
                    viewModel.setSearchQuery(it)
                },
                placeholder = { Text("Search confessions, #tags, topics...") },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchInput.isNotEmpty()) {
                        IconButton(onClick = {
                            searchInput = ""
                            viewModel.setSearchQuery("")
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (searchInput.isBlank()) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Safety pledge banner
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Shield,
                                        contentDescription = "Zero-leak Privacy",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "100% Anonymous & Zero-Leak",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Your identity is encrypted. Real names and accounts are never public.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Trending Topics Header
                    item {
                        Text(
                            text = "🔥 Trending Anonymous Topics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Topic cards
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            trendingTopics.chunked(2).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    rowItems.forEach { (title, emoji, theme) ->
                                        Card(
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.cardColors(containerColor = theme.backgroundColor),
                                            border = androidx.compose.foundation.BorderStroke(1.5.dp, theme.borderColor),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(16.dp))
                                                .clickable {
                                                    HapticsHelper.playClick(context)
                                                    searchInput = title.split(" ").first()
                                                    viewModel.setSearchQuery(searchInput)
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .background(Brush.linearGradient(theme.gradientColors))
                                                    .padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = emoji, fontSize = 24.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = title,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = theme.textColor
                                                    )
                                                    Text(
                                                        text = "Explore #tag",
                                                        fontSize = 10.sp,
                                                        color = theme.accentColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (rowItems.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // Categories Browse
                    item {
                        Text(
                            text = "🏷️ Browse All Categories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ConfessionPost.CATEGORIES.filter { it != "All" }.forEach { cat ->
                                SuggestionChip(
                                    onClick = {
                                        HapticsHelper.playLightTick(context)
                                        viewModel.selectCategory(cat)
                                        searchInput = cat
                                        viewModel.setSearchQuery(cat)
                                    },
                                    label = { Text(cat) }
                                )
                            }
                        }
                    }
                }
            } else {
                // Search Results
                when (val state = feedState) {
                    is UiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    is UiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Error searching: ${state.message}")
                        }
                    }
                    is UiState.Success -> {
                        val results = state.data
                        if (results.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No confessions matched \"$searchInput\"")
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(results, key = { it.postId }) { post ->
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
                                        onReportClick = { viewModel.reportContent("post", post.postId, "Inappropriate") },
                                        onBlockClick = { viewModel.blockUser(post.anonymousId) },
                                        onDeleteClick = { viewModel.deletePost(post.postId) },
                                        modifier = Modifier.clickable { onNavigateToDetail(post.postId) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
