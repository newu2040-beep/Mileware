package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class CommentModel(
    @DocumentId
    val commentId: String = "",
    val postId: String = "",
    val authorId: String = "",
    val anonymousId: String = "Anonymous",
    val anonymousAvatar: String = "🎭",
    val anonymousColor: String = "#9D7BFF",
    val content: String = "",
    val parentId: String = "", // empty if top-level, commentId if reply
    val likeCount: Int = 0,
    @ServerTimestamp
    val createdAt: Timestamp? = null
)
