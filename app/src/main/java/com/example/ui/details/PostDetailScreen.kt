package com.example.ui.details

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
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
import com.example.data.model.CommentModel
import com.example.data.model.ConfessionPost
import com.example.data.model.ReactionModel
import com.example.data.repository.ConfessionRepository
import com.example.ui.components.ConfessionCard
import com.example.ui.viewmodel.ConfessionViewModel
import com.example.util.HapticsHelper
import kotlinx.coroutines.flow.catch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    viewModel: ConfessionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUserId = viewModel.currentUserId
    val userSavedPostIds by viewModel.userSavedPostIds.collectAsState()
    val userReactions by viewModel.userReactions.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val confessionRepo = remember { ConfessionRepository(context) }

    var post by remember { mutableStateOf<ConfessionPost?>(null) }
    var comments by remember { mutableStateOf<List<CommentModel>>(emptyList()) }
    var commentInput by remember { mutableStateOf("") }
    var replyingToComment by remember { mutableStateOf<CommentModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(postId) {
        confessionRepo.observePost(postId)
            .catch { }
            .collect { observedPost ->
                post = observedPost
                isLoading = false
            }
    }

    LaunchedEffect(postId) {
        confessionRepo.observeComments(postId)
            .catch { }
            .collect { observedComments ->
                comments = observedComments
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confession & Replies", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        HapticsHelper.playLightTick(context)
                        onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    post?.let { p ->
                        IconButton(onClick = { viewModel.openExportDialog(p) }) {
                            Icon(Icons.Default.PhotoSizeSelectActual, contentDescription = "Export 4K/8K")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (post?.commentsEnabled == true) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        // Replying to banner
                        if (replyingToComment != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Replying to ${replyingToComment?.anonymousId}...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                IconButton(
                                    onClick = { replyingToComment = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel reply", modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = userProfile?.anonymousAvatar ?: "🎭",
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedTextField(
                                value = commentInput,
                                onValueChange = { if (it.length <= 500) commentInput = it },
                                placeholder = {
                                    Text(
                                        if (replyingToComment != null) "Reply anonymously..." else "Add an anonymous reply..."
                                    )
                                },
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.weight(1f),
                                maxLines = 3
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(
                                onClick = {
                                    if (commentInput.isNotBlank() && post != null) {
                                        viewModel.addComment(
                                            postId = post!!.postId,
                                            postAuthorId = post!!.authorId,
                                            content = commentInput,
                                            parentId = replyingToComment?.commentId ?: "",
                                            onComplete = {
                                                commentInput = ""
                                                replyingToComment = null
                                            }
                                        )
                                    }
                                },
                                enabled = commentInput.isNotBlank(),
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (commentInput.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send Reply",
                                    tint = if (commentInput.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (post == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Confession not found or was removed.")
            }
        } else {
            val currentPost = post!!
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                // Main Confession Card
                item {
                    ConfessionCard(
                        post = currentPost,
                        isSaved = userSavedPostIds.contains(currentPost.postId),
                        userReaction = userReactions[currentPost.postId],
                        currentUserId = currentUserId,
                        onReactionClick = { type -> viewModel.toggleReaction(currentPost, type) },
                        onCommentClick = { },
                        onSaveClick = { viewModel.toggleSavePost(currentPost.postId) },
                        onExportClick = { viewModel.openExportDialog(currentPost) },
                        onShareClick = { viewModel.openExportDialog(currentPost) },
                        onReportClick = { viewModel.reportContent("post", currentPost.postId, "Inappropriate") },
                        onBlockClick = { viewModel.blockUser(currentPost.anonymousId) },
                        onDeleteClick = {
                            viewModel.deletePost(currentPost.postId)
                            onNavigateBack()
                        },
                        isDetailView = true
                    )
                }

                // Comments header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Anonymous Replies (${comments.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (comments.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text(text = "💬", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "No replies yet",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Share your thoughts or support anonymously.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(comments, key = { it.commentId }) { comment ->
                        val isReply = comment.parentId.isNotBlank()
                        CommentItemRow(
                            comment = comment,
                            isReply = isReply,
                            onReplyClick = {
                                HapticsHelper.playLightTick(context)
                                replyingToComment = comment
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommentItemRow(
    comment: CommentModel,
    isReply: Boolean,
    onReplyClick: () -> Unit
) {
    val badgeColor = try {
        Color(android.graphics.Color.parseColor(comment.anonymousColor))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isReply) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (isReply) 28.dp else 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(badgeColor.copy(alpha = 0.2f))
                            .border(1.dp, badgeColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = comment.anonymousAvatar, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = comment.anonymousId,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                val formattedDate = comment.createdAt?.toDate()?.let {
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(it)
                } ?: "Now"
                Text(
                    text = formattedDate,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onReplyClick() }
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Reply",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Reply",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
