package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class ReportModel(
    @DocumentId
    val reportId: String = "",
    val reporterId: String = "",
    val targetType: String = "post", // post, comment, user
    val targetId: String = "",
    val reason: String = "Spam",
    val notes: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null
)

data class UserModel(
    @DocumentId
    val userId: String = "",
    val anonymousHandle: String = "Anonymous #0000",
    val anonymousAvatar: String = "🎭",
    val anonymousColor: String = "#9D7BFF",
    val bio: String = "Whispering thoughts into the void...",
    val themePreference: String = "pastel_lavender",
    val totalConfessions: Int = 0,
    val totalReactionsReceived: Int = 0,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)
