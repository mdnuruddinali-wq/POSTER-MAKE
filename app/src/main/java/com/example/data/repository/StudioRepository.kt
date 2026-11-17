package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AiGenerationRecord
import com.example.data.model.DesignProject
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StudioRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUid: String? = null): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        if (expectedUid != null) {
            check(uid == expectedUid) { "Signed-in user changed; refusing cross-user access." }
        }
        return uid
    }

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        email: String?,
        photoUrl: String?
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            val safeName = displayName.trim().ifBlank { "Creator" }.take(80)
            if (!existing.exists()) {
                val payload = mutableMapOf<String, Any?>(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to email?.take(160)?.ifBlank { null },
                    "photoUrl" to photoUrl?.take(500)?.ifBlank { null },
                    "preferredFormat" to "POSTER_3_4",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).await()
            } else {
                val update = mutableMapOf<String, Any?>(
                    "displayName" to safeName,
                    "email" to email?.take(160)?.ifBlank { null },
                    "photoUrl" to photoUrl?.take(500)?.ifBlank { null },
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.update(update).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeProjects(userId: String): Flow<List<DesignProject>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/projects"
        emitAll(
            db.collection("users").document(uid).collection("projects")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DesignProject::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeAiHistory(userId: String): Flow<List<AiGenerationRecord>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/ai_generations"
        emitAll(
            db.collection("users").document(uid).collection("ai_generations")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        AiGenerationRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveProject(userId: String, project: DesignProject): Result<String> {
        val isNew = project.id.isBlank()
        val projectId = if (isNew) "proj_${UUID.randomUUID().toString().replace("-", "").take(16)}" else project.id
        val docPath = "users/$userId/projects/$projectId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
            if (isNew) {
                docRef.set(project.toCreateMap(uid)).await()
            } else {
                docRef.update(project.toUpdateMap()).await()
            }
            Result.success(projectId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, if (isNew) OperationType.CREATE else OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(userId: String, projectId: String, favorite: Boolean): Result<Unit> {
        val docPath = "users/$userId/projects/$projectId"
        return try {
            val uid = requireUserId(userId)
            require(projectId.isNotBlank()) { "Project ID cannot be blank" }
            db.collection("users").document(uid).collection("projects").document(projectId)
                .update(
                    mapOf(
                        "favorite" to favorite,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun renameProject(userId: String, projectId: String, newTitle: String): Result<Unit> {
        val docPath = "users/$userId/projects/$projectId"
        return try {
            val uid = requireUserId(userId)
            require(projectId.isNotBlank()) { "Project ID cannot be blank" }
            val cleanTitle = newTitle.trim().take(100)
            require(cleanTitle.isNotEmpty()) { "Title cannot be empty" }
            db.collection("users").document(uid).collection("projects").document(projectId)
                .update(
                    mapOf(
                        "title" to cleanTitle,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteProject(userId: String, projectId: String): Result<Unit> {
        val docPath = "users/$userId/projects/$projectId"
        return try {
            val uid = requireUserId(userId)
            require(projectId.isNotBlank()) { "Project ID cannot be blank" }
            db.collection("users").document(uid).collection("projects").document(projectId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordAiGeneration(userId: String, record: AiGenerationRecord): Result<Unit> {
        val genId = "gen_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docPath = "users/$userId/ai_generations/$genId"
        return try {
            val uid = requireUserId(userId)
            db.collection("users").document(uid).collection("ai_generations").document(genId)
                .set(record.toCreateMap(uid))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteAiGeneration(userId: String, generationId: String): Result<Unit> {
        val docPath = "users/$userId/ai_generations/$generationId"
        return try {
            val uid = requireUserId(userId)
            require(generationId.isNotBlank()) { "Generation ID cannot be blank" }
            db.collection("users").document(uid).collection("ai_generations").document(generationId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
