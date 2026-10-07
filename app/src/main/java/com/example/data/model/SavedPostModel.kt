package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class SavedPostModel(
    @DocumentId
    val saveId: String = "",
    val userId: String = "",
    val postId: String = "",
    @ServerTimestamp
    val savedAt: Timestamp? = null
)
