package com.example.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException
import org.json.JSONObject

enum class OperationType {
    GET, LIST, CREATE, UPDATE, DELETE, TRANSACTION, BATCH
}

fun handleFirestoreError(
    exception: Exception,
    operationType: OperationType,
    collectionOrPath: String
) {
    val errorDetails = JSONObject().apply {
        put("operation", operationType.name)
        put("target", collectionOrPath)
        put("timestamp", System.currentTimeMillis())

        if (exception is FirebaseFirestoreException) {
            put("code", exception.code.name)
            put("errorCode", exception.code.value())
            put("message", exception.message ?: "Unknown Firestore error")
            put("isFirestoreException", true)
        } else {
            put("code", "UNKNOWN")
            put("message", exception.message ?: "Non-Firestore exception")
            put("exceptionClass", exception.javaClass.simpleName)
            put("isFirestoreException", false)
        }
    }

    Log.e("FirestoreError", errorDetails.toString(2), exception)
}
