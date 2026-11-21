package com.bsdcampus.app.notifications

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.bsdcampus.app.R
import com.bsdcampus.app.data.CampusMediaRepository
import com.bsdcampus.app.data.PreferencesRepository
import com.bsdcampus.app.firebase.configureAppCheck
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class PostDeleteWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        configureAppCheck(applicationContext, null)
        val postId = inputData.getString(KEY_POST_ID).orEmpty().trim()
        if (postId.isBlank()) return Result.success()

        val mediaUris = inputData.getStringArray(KEY_MEDIA_URIS).orEmpty().toList()
        val isAdmin = inputData.getBoolean(KEY_IS_ADMIN, false)

        val prefs = PreferencesRepository(applicationContext)
        val auth = Firebase.auth
        if (auth.currentUser == null) {
            return Result.retry()
        }

        val db = FirebaseFirestore.getInstance(applicationContext.getString(R.string.firestore_database_id))
        return try {
            db.collection("posts").document(postId).delete().await()
            if (mediaUris.isNotEmpty()) {
                CampusMediaRepository.deleteMediaAssetsForUris(
                    uris = mediaUris,
                    isAdmin = isAdmin,
                    db = db,
                    auth = auth,
                    context = applicationContext
                )
            }
            prefs.removePendingDeletedPostId(postId)
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            if (isPermanentDeleteError(t)) {
                Log.w(TAG, "Permanent failure deleting post $postId: ${t.message}")
                prefs.removePendingDeletedPostId(postId)
                Result.failure()
            } else if (runAttemptCount < 8) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    private fun isPermanentDeleteError(t: Throwable): Boolean {
        var cur: Throwable? = t
        while (cur != null) {
            if (cur is SecurityException || cur is IllegalArgumentException) return true
            if (cur is FirebaseFirestoreException && (
                    cur.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                    cur.code == FirebaseFirestoreException.Code.INVALID_ARGUMENT
                )
            ) {
                return true
            }
            cur = cur.cause
        }
        return false
    }

    companion object {
        private const val TAG = "PostDeleteWorker"
        private const val KEY_POST_ID = "post_id"
        private const val KEY_MEDIA_URIS = "media_uris"
        private const val KEY_IS_ADMIN = "is_admin"

        private fun workName(postId: String) = "post_delete_$postId"

        fun enqueue(
            context: Context,
            postId: String,
            mediaUris: List<String>,
            isAdmin: Boolean
        ) {
            if (postId.isBlank()) return
            val appCtx = context.applicationContext
            val wm = try {
                if (!WorkManager.isInitialized()) {
                    WorkManager.initialize(appCtx, Configuration.Builder().build())
                }
                WorkManager.getInstance(appCtx)
            } catch (t: Throwable) {
                Log.w(TAG, "WorkManager unavailable for post delete: ${t.message}")
                return
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<PostDeleteWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 5L, TimeUnit.SECONDS)
                .setInputData(
                    workDataOf(
                        KEY_POST_ID to postId,
                        KEY_MEDIA_URIS to mediaUris.toTypedArray(),
                        KEY_IS_ADMIN to isAdmin
                    )
                )
                .build()

            wm.enqueueUniqueWork(
                workName(postId),
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun cancel(context: Context, postId: String) {
            if (postId.isBlank()) return
            runCatching {
                val appCtx = context.applicationContext
                if (WorkManager.isInitialized()) {
                    WorkManager.getInstance(appCtx).cancelUniqueWork(workName(postId))
                }
            }
        }
    }
}
