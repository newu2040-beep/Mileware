package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class ReactionModel(
    @DocumentId
    val reactionId: String = "",
    val postId: String = "",
    val userId: String = "",
    val reactionType: String = "love", // love, sad, funny, wow, angry, relatable
    @ServerTimestamp
    val createdAt: Timestamp? = null
) {
    enum class Type(val key: String, val emoji: String, val label: String) {
        LOVE("love", "❤️", "Love"),
        SAD("sad", "😢", "Sad"),
        FUNNY("funny", "😂", "Funny"),
        WOW("wow", "😮", "Wow"),
        ANGRY("angry", "😡", "Angry"),
        RELATABLE("relatable", "💭", "Relatable");

        companion object {
            fun fromKey(key: String): Type {
                return entries.find { it.key == key } ?: LOVE
            }
        }
    }
}
