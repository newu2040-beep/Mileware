package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class ConfessionPost(
    @DocumentId
    val postId: String = "",
    val authorId: String = "",
    val anonymousId: String = "Anonymous #0000",
    val anonymousAvatar: String = "🎭",
    val anonymousColor: String = "#9D7BFF",
    val content: String = "",
    val category: String = "Confessions",
    val postType: String = "Confession", // Confession, Secret, Story, Question, Experience, Advice, Funny, Mystery
    val pastelTheme: String = "pastel_lavender",
    val contentWarning: String = "",
    val commentsEnabled: Boolean = true,
    val reactionsEnabled: Boolean = true,
    val mediaUrl: String = "",
    val audioDurationSec: Int = 0,
    val reactionCount: Int = 0,
    val commentCount: Int = 0,
    val saveCount: Int = 0,
    val shareCount: Int = 0,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val reactions: Map<String, Long> = emptyMap(),
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    fun getReactionCount(type: String): Long {
        return reactions[type] ?: 0L
    }

    companion object {
        val CATEGORIES = listOf(
            "All",
            "Love",
            "Secrets",
            "Life",
            "Relationships",
            "School",
            "Work",
            "Mental Thoughts",
            "Funny",
            "Horror",
            "Mystery",
            "Advice",
            "Confessions",
            "Other"
        )

        val POST_TYPES = listOf(
            "Confession" to "🤫",
            "Secret" to "🔒",
            "Story" to "📖",
            "Question" to "❓",
            "Experience" to "💡",
            "Advice" to "🌱",
            "Funny" to "😂",
            "Mystery" to "🔮"
        )

        val WARNING_PRESETS = listOf(
            "None",
            "Sensitive Topic",
            "Mental Health",
            "Relationship Drama",
            "Academic Stress",
            "Work Pressure",
            "Spoilers",
            "Unfiltered Emotion"
        )
    }
}
