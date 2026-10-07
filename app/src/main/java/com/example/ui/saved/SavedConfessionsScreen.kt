package com.example.ui.saved

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConfessionPost
import com.example.data.repository.ConfessionRepository
import com.example.ui.components.ConfessionCard
import com.example.ui.viewmodel.ConfessionViewModel
import com.example.ui.viewmodel.UiState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.catch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedConfessionsScreen(
    viewModel: ConfessionViewModel,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val savedIds by viewModel.userSavedPostIds.collectAsState()
    val feedState by viewModel.feedPosts.collectAsState()
    val userReactions by viewModel.userReactions.collectAsState()
    val currentUserId = viewModel.currentUserId

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Private Bookmarks", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        if (savedIds.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "🔖", fontSize = 48.sp)
                    Text(
                        text = "No saved confessions yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap the bookmark icon on any confession to save it privately for later.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val allPosts = (feedState as? UiState.Success)?.data ?: emptyList()
            val savedPosts = allPosts.filter { savedIds.contains(it.postId) }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(savedPosts, key = { it.postId }) { post ->
                    ConfessionCard(
                        post = post,
                        isSaved = true,
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
