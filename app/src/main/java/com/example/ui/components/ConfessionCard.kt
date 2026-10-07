package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConfessionPost
import com.example.data.model.PastelTheme
import com.example.data.model.ReactionModel
import com.example.util.HapticsHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConfessionCard(
    post: ConfessionPost,
    isSaved: Boolean,
    userReaction: String?,
    currentUserId: String,
    onReactionClick: (ReactionModel.Type) -> Unit,
    onCommentClick: () -> Unit,
    onSaveClick: () -> Unit,
    onExportClick: () -> Unit,
    onShareClick: () -> Unit,
    onReportClick: () -> Unit,
    onBlockClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDetailView: Boolean = false
) {
    val context = LocalContext.current
    val theme = PastelTheme.fromId(post.pastelTheme)
    var showMenu by remember { mutableStateOf(false) }
    var isWarningDismissed by remember { mutableStateOf(post.contentWarning.isBlank()) }
    var isPlayingAudio by remember { mutableStateOf(false) }
    var audioProgress by remember { mutableStateOf(0f) }

    // Audio playback simulation effect
    LaunchedEffect(isPlayingAudio) {
        if (isPlayingAudio) {
            val totalSeconds = if (post.audioDurationSec > 0) post.audioDurationSec else 12
            val startTime = System.currentTimeMillis()
            while (isPlayingAudio && audioProgress < 1f) {
                val elapsed = (System.currentTimeMillis() - startTime) / 1000f
                audioProgress = (elapsed / totalSeconds).coerceIn(0f, 1f)
                kotlinx.coroutines.delay(100)
            }
            isPlayingAudio = false
            audioProgress = 0f
        }
    }

    val badgeColor = try {
        Color(android.graphics.Color.parseColor(post.anonymousColor))
    } catch (_: Exception) {
        theme.accentColor
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = theme.backgroundColor),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, theme.borderColor),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(theme.gradientColors))
                .padding(18.dp)
        ) {
            // Header Row: Avatar, Alias, PostType, Category, Date, Menu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.25f))
                        .border(1.5.dp, badgeColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = post.anonymousAvatar, fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.anonymousId,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = theme.textColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(theme.accentColor.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = post.postType,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.textColor
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "#${post.category}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = theme.accentColor
                        )
                        Text(text = "•", fontSize = 10.sp, color = theme.textColor.copy(alpha = 0.5f))
                        val formattedDate = post.createdAt?.toDate()?.let {
                            SimpleDateFormat("MMM d • h:mm a", Locale.getDefault()).format(it)
                        } ?: "Just now"
                        Text(
                            text = formattedDate,
                            fontSize = 11.sp,
                            color = theme.textColor.copy(alpha = 0.65f)
                        )
                    }
                }

                // More Menu Button
                Box {
                    IconButton(
                        onClick = {
                            HapticsHelper.playLightTick(context)
                            showMenu = true
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Post Options",
                            tint = theme.textColor.copy(alpha = 0.8f)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export 4K/8K Card") },
                            leadingIcon = { Icon(Icons.Default.PhotoSizeSelectActual, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onExportClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share Confession") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onShareClick()
                            }
                        )
                        if (post.authorId == currentUserId) {
                            DropdownMenuItem(
                                text = { Text("Delete Confession", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDeleteClick()
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Report Confession") },
                                leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onReportClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Block ${post.anonymousId}") },
                                leadingIcon = { Icon(Icons.Default.Block, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onBlockClick()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content Warning Accordion if applicable
            if (post.contentWarning.isNotBlank() && !isWarningDismissed) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFEBEE),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFCDD2)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            HapticsHelper.playLightTick(context)
                            isWarningDismissed = true
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(text = "⚠️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Content Warning: ${post.contentWarning}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFC62828)
                            )
                            Text(
                                text = "Tap to reveal confession",
                                fontSize = 11.sp,
                                color = Color(0xFFB71C1C)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = "Reveal",
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                // Confession Text Content
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = post.content,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = if (post.content.length < 140) 18.sp else 15.sp,
                            lineHeight = if (post.content.length < 140) 26.sp else 22.sp,
                            fontWeight = FontWeight.Normal,
                            fontFamily = FontFamily.Serif
                        ),
                        color = theme.textColor,
                        maxLines = if (isDetailView) Int.MAX_VALUE else 8,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Audio Voice Note Snippet if present
                if (post.audioDurationSec > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = theme.backgroundColor.copy(alpha = 0.85f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, theme.accentColor.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    HapticsHelper.playClick(context)
                                    isPlayingAudio = !isPlayingAudio
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(theme.accentColor)
                            ) {
                                Icon(
                                    imageVector = if (isPlayingAudio) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play voice note",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Voice Memo Confession",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = theme.textColor
                                    )
                                    Text(
                                        text = "${post.audioDurationSec}s",
                                        fontSize = 11.sp,
                                        color = theme.textColor.copy(alpha = 0.7f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { audioProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = theme.accentColor,
                                    trackColor = theme.borderColor
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Divider inside card
            HorizontalDivider(
                color = theme.borderColor.copy(alpha = 0.6f),
                thickness = 1.dp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Reaction Bar: 6 Animated Reactions
            if (post.reactionsEnabled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ReactionModel.Type.entries.forEach { type ->
                        val isSelected = userReaction == type.key
                        val count = post.getReactionCount(type.key)

                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.15f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "reactionScale"
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) theme.accentColor.copy(alpha = 0.35f) else theme.backgroundColor.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) theme.accentColor else theme.borderColor
                            ),
                            modifier = Modifier
                                .scale(scale)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    onReactionClick(type)
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text = type.emoji, fontSize = 14.sp)
                                if (count > 0 || isSelected) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isSelected && count == 0L) "1" else count.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = theme.textColor
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Action Buttons Row: Comments, Save, 4K Export, Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Comments Button
                if (post.commentsEnabled) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                HapticsHelper.playLightTick(context)
                                onCommentClick()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comments",
                            tint = theme.textColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (post.commentCount > 0) "${post.commentCount}" else "Comment",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = theme.textColor
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Export 4K/8K Button
                    IconButton(
                        onClick = {
                            HapticsHelper.playLightTick(context)
                            onExportClick()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.HighQuality,
                            contentDescription = "Export 4K/8K Card",
                            tint = theme.textColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Save / Bookmark Button
                    IconButton(
                        onClick = {
                            HapticsHelper.playClick(context)
                            onSaveClick()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save Post",
                            tint = if (isSaved) theme.accentColor else theme.textColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Share Button
                    IconButton(
                        onClick = {
                            HapticsHelper.playClick(context)
                            onShareClick()
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share",
                            tint = theme.textColor.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
