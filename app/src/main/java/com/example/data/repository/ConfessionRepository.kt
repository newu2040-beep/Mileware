package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.CommentModel
import com.example.data.model.ConfessionPost
import com.example.data.model.NotificationModel
import com.example.data.model.ReactionModel
import com.example.data.model.ReportModel
import com.example.data.model.SavedPostModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ConfessionRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be authenticated before performing this action.")
    }

    fun observeFeed(category: String = "All", filter: String = "latest"): Flow<List<ConfessionPost>> = flow {
        val path = "posts"
        var query: Query = db.collection(path)
            .whereEqualTo("isDeleted", false)
            .whereEqualTo("isArchived", false)

        if (category != "All" && category.isNotBlank()) {
            query = query.whereEqualTo("category", category)
        }

        query = when (filter.lowercase()) {
            "trending" -> query.orderBy("reactionCount", Query.Direction.DESCENDING)
            "popular" -> query.orderBy("commentCount", Query.Direction.DESCENDING)
            else -> query.orderBy("createdAt", Query.Direction.DESCENDING)
        }

        emitAll(
            query.snapshots()
                .map { snapshot -> snapshot.toObjects(ConfessionPost::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observePost(postId: String): Flow<ConfessionPost?> = flow {
        val path = "posts/$postId"
        emitAll(
            db.collection("posts").document(postId)
                .snapshots()
                .map { snapshot -> snapshot.toObject(ConfessionPost::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    fun observeComments(postId: String): Flow<List<CommentModel>> = flow {
        val path = "posts/$postId/comments"
        emitAll(
            db.collection("posts").document(postId).collection("comments")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(CommentModel::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeUserReactions(userId: String): Flow<Map<String, String>> = flow {
        val path = "reactions"
        emitAll(
            db.collection("reactions")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.associate { doc ->
                        val postId = doc.getString("postId") ?: ""
                        val reactionType = doc.getString("reactionType") ?: ""
                        postId to reactionType
                    }
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeUserSavedPostIds(userId: String): Flow<Set<String>> = flow {
        val path = "saved_posts"
        emitAll(
            db.collection("saved_posts")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { it.getString("postId") }.toSet()
                }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeMyPosts(userId: String): Flow<List<ConfessionPost>> = flow {
        val path = "posts"
        emitAll(
            db.collection("posts")
                .whereEqualTo("authorId", userId)
                .whereEqualTo("isDeleted", false)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(ConfessionPost::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeNotifications(userId: String): Flow<List<NotificationModel>> = flow {
        val path = "notifications"
        emitAll(
            db.collection("notifications")
                .whereEqualTo("recipientId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot -> snapshot.toObjects(NotificationModel::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                    throw error
                }
        )
    }

    suspend fun createPost(post: ConfessionPost): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("posts").document()
        val payload = mapOf(
            "postId" to docRef.id,
            "authorId" to uid,
            "anonymousId" to post.anonymousId,
            "anonymousAvatar" to post.anonymousAvatar,
            "anonymousColor" to post.anonymousColor,
            "content" to post.content,
            "category" to post.category,
            "postType" to post.postType,
            "pastelTheme" to post.pastelTheme,
            "contentWarning" to post.contentWarning,
            "commentsEnabled" to post.commentsEnabled,
            "reactionsEnabled" to post.reactionsEnabled,
            "mediaUrl" to post.mediaUrl,
            "audioDurationSec" to post.audioDurationSec,
            "reactionCount" to 0,
            "commentCount" to 0,
            "saveCount" to 0,
            "shareCount" to 0,
            "isArchived" to false,
            "isDeleted" to false,
            "reactions" to emptyMap<String, Long>(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        return try {
            docRef.set(payload).await()
            // Increment user total confessions
            val userRef = db.collection("users").document(uid)
            userRef.update("totalConfessions", FieldValue.increment(1))
            Result.success(docRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "posts/${docRef.id}")
            Result.failure(e)
        }
    }

    suspend fun toggleReaction(
        postId: String,
        postAuthorId: String,
        currentReactionType: String?,
        newReactionType: String,
        userAnonymousName: String
    ): Result<Unit> {
        val uid = requireUserId()
        val reactionDocId = "${postId}_$uid"
        val reactionRef = db.collection("reactions").document(reactionDocId)
        val postRef = db.collection("posts").document(postId)

        return try {
            if (currentReactionType == newReactionType) {
                // Remove reaction
                reactionRef.delete().await()
                postRef.update(
                    mapOf(
                        "reactionCount" to FieldValue.increment(-1),
                        "reactions.$newReactionType" to FieldValue.increment(-1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                // Add or change reaction
                val isNew = currentReactionType == null
                val payload = mapOf(
                    "reactionId" to reactionDocId,
                    "postId" to postId,
                    "userId" to uid,
                    "reactionType" to newReactionType,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                reactionRef.set(payload).await()

                val updates = mutableMapOf<String, Any>(
                    "reactions.$newReactionType" to FieldValue.increment(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (isNew) {
                    updates["reactionCount"] = FieldValue.increment(1)
                } else {
                    updates["reactions.$currentReactionType"] = FieldValue.increment(-1)
                }
                postRef.update(updates).await()

                // Notify post author if not self
                if (postAuthorId.isNotBlank() && postAuthorId != uid) {
                    val notifRef = db.collection("notifications").document()
                    notifRef.set(
                        mapOf(
                            "notificationId" to notifRef.id,
                            "recipientId" to postAuthorId,
                            "senderAnonymousId" to userAnonymousName,
                            "type" to "reaction",
                            "postId" to postId,
                            "title" to "New reaction on your confession",
                            "message" to "$userAnonymousName reacted with $newReactionType",
                            "read" to false,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "posts/$postId")
            Result.failure(e)
        }
    }

    suspend fun addComment(
        postId: String,
        postAuthorId: String,
        commentText: String,
        anonymousId: String,
        anonymousAvatar: String,
        anonymousColor: String,
        parentId: String = ""
    ): Result<String> {
        val uid = requireUserId()
        val commentRef = db.collection("posts").document(postId).collection("comments").document()
        val payload = mapOf(
            "commentId" to commentRef.id,
            "postId" to postId,
            "authorId" to uid,
            "anonymousId" to anonymousId,
            "anonymousAvatar" to anonymousAvatar,
            "anonymousColor" to anonymousColor,
            "content" to commentText,
            "parentId" to parentId,
            "likeCount" to 0,
            "createdAt" to FieldValue.serverTimestamp()
        )

        return try {
            commentRef.set(payload).await()
            db.collection("posts").document(postId).update(
                mapOf(
                    "commentCount" to FieldValue.increment(1),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            if (postAuthorId.isNotBlank() && postAuthorId != uid) {
                val notifRef = db.collection("notifications").document()
                notifRef.set(
                    mapOf(
                        "notificationId" to notifRef.id,
                        "recipientId" to postAuthorId,
                        "senderAnonymousId" to anonymousId,
                        "type" to if (parentId.isEmpty()) "comment" else "reply",
                        "postId" to postId,
                        "title" to if (parentId.isEmpty()) "New comment on your confession" else "New reply to a comment",
                        "message" to "\"${commentText.take(60)}\"",
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            Result.success(commentRef.id)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "posts/$postId/comments/${commentRef.id}")
            Result.failure(e)
        }
    }

    suspend fun toggleSavePost(postId: String, isCurrentlySaved: Boolean): Result<Boolean> {
        val uid = requireUserId()
        val saveDocId = "${uid}_$postId"
        val saveRef = db.collection("saved_posts").document(saveDocId)
        val postRef = db.collection("posts").document(postId)

        return try {
            if (isCurrentlySaved) {
                saveRef.delete().await()
                postRef.update(
                    mapOf(
                        "saveCount" to FieldValue.increment(-1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                Result.success(false)
            } else {
                saveRef.set(
                    mapOf(
                        "saveId" to saveDocId,
                        "userId" to uid,
                        "postId" to postId,
                        "savedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                postRef.update(
                    mapOf(
                        "saveCount" to FieldValue.increment(1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                Result.success(true)
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "saved_posts/$saveDocId")
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        val uid = requireUserId()
        val postRef = db.collection("posts").document(postId)
        return try {
            postRef.update(
                mapOf(
                    "isDeleted" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "posts/$postId")
            Result.failure(e)
        }
    }

    suspend fun archivePost(postId: String, isArchived: Boolean): Result<Unit> {
        val uid = requireUserId()
        val postRef = db.collection("posts").document(postId)
        return try {
            postRef.update(
                mapOf(
                    "isArchived" to isArchived,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "posts/$postId")
            Result.failure(e)
        }
    }

    suspend fun reportContent(
        targetType: String,
        targetId: String,
        reason: String,
        notes: String = ""
    ): Result<Unit> {
        val uid = requireUserId()
        val reportRef = db.collection("reports").document()
        val payload = mapOf(
            "reportId" to reportRef.id,
            "reporterId" to uid,
            "targetType" to targetType,
            "targetId" to targetId,
            "reason" to reason,
            "notes" to notes,
            "createdAt" to FieldValue.serverTimestamp()
        )
        return try {
            reportRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "reports/${reportRef.id}")
            Result.failure(e)
        }
    }

    suspend fun blockUser(blockedAnonymousId: String): Result<Unit> {
        val uid = requireUserId()
        val blockId = "${uid}_${blockedAnonymousId.replace(" ", "_")}"
        val blockRef = db.collection("blocks").document(blockId)
        val payload = mapOf(
            "blockId" to blockId,
            "userId" to uid,
            "blockedAnonymousId" to blockedAnonymousId,
            "createdAt" to FieldValue.serverTimestamp()
        )
        return try {
            blockRef.set(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "blocks/$blockId")
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notificationId: String) {
        try {
            db.collection("notifications").document(notificationId).update("read", true).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications/$notificationId")
        }
    }
}
