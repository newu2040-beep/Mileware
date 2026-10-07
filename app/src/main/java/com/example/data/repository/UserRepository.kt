package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AnonymousPersona
import com.example.data.model.UserModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class UserRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeUser(userId: String): Flow<UserModel?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot -> snapshot.toObject(UserModel::class.java) }
                .catch { error ->
                    if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun getOrCreateUser(userId: String): UserModel {
        val userDoc = db.collection("users").document(userId)
        val snapshot = userDoc.get().await()

        if (snapshot.exists()) {
            return snapshot.toObject(UserModel::class.java) ?: UserModel(userId = userId)
        }

        val generatedPersona = AnonymousPersona.generateNumbered(userId)
        val newUser = UserModel(
            userId = userId,
            anonymousHandle = generatedPersona.name,
            anonymousAvatar = generatedPersona.avatarEmoji,
            anonymousColor = generatedPersona.badgeColorHex,
            bio = "Whispering secrets anonymously in the void...",
            themePreference = "pastel_lavender",
            totalConfessions = 0,
            totalReactionsReceived = 0
        )

        val payload = mapOf(
            "userId" to userId,
            "anonymousHandle" to newUser.anonymousHandle,
            "anonymousAvatar" to newUser.anonymousAvatar,
            "anonymousColor" to newUser.anonymousColor,
            "bio" to newUser.bio,
            "themePreference" to newUser.themePreference,
            "totalConfessions" to 0,
            "totalReactionsReceived" to 0,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            userDoc.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$userId")
        }

        return newUser
    }

    suspend fun updatePersona(
        userId: String,
        handle: String,
        avatar: String,
        color: String,
        bio: String
    ): Result<Unit> {
        val userDoc = db.collection("users").document(userId)
        val updates = mapOf(
            "anonymousHandle" to handle,
            "anonymousAvatar" to avatar,
            "anonymousColor" to color,
            "bio" to bio,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            userDoc.update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$userId")
            Result.failure(e)
        }
    }

    suspend fun updateThemePreference(userId: String, themeId: String): Result<Unit> {
        val userDoc = db.collection("users").document(userId)
        return try {
            userDoc.update(
                mapOf(
                    "themePreference" to themeId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$userId")
            Result.failure(e)
        }
    }
}
