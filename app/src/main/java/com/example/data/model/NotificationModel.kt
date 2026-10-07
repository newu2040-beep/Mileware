package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

data class NotificationModel(
    @DocumentId
    val notificationId: String = "",
    val recipientId: String = "",
    val senderAnonymousId: String = "Anonymous Soul",
    val type: String = "reaction", // reaction, comment, reply, trending, system
    val postId: String = "",
    val title: String = "",
    val message: String = "",
    val read: Boolean = false,
    @ServerTimestamp
    val createdAt: Timestamp? = null
)
