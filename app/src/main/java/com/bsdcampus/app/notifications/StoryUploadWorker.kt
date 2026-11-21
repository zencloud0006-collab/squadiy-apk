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
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.bsdcampus.app.R
import com.bsdcampus.app.data.CampusMediaRepository
import com.bsdcampus.app.data.CampusRepository
import com.bsdcampus.app.data.StoryMusicTrack
import com.bsdcampus.app.firebase.configureAppCheck
import com.bsdcampus.app.model.CampusUser
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.concurrent.TimeUnit

class StoryUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        configureAppCheck(applicationContext, null)
        NotificationHelper.ensureChannels(applicationContext)

        val uploadId = inputData.getString(KEY_UPLOAD_ID).orEmpty()
        val stagedPath = inputData.getString(KEY_STAGED_PATH).orEmpty()
        val isVideo = inputData.getBoolean(KEY_IS_VIDEO, false)
        val caption = inputData.getString(KEY_CAPTION).orEmpty()
        val isAdminInput = inputData.getBoolean(KEY_IS_ADMIN, false)

        val musicPreviewUrl = inputData.getString(KEY_MUSIC_PREVIEW_URL)?.takeIf { it.isNotBlank() }
        val musicTrack = if (musicPreviewUrl != null) {
            StoryMusicTrack(
                title = inputData.getString(KEY_MUSIC_TITLE).orEmpty().ifBlank { "Track" },
                artist = inputData.getString(KEY_MUSIC_ARTIST).orEmpty().ifBlank { "Artist" },
                artworkUrl = inputData.getString(KEY_MUSIC_ARTWORK_URL).orEmpty(),
                previewUrl = musicPreviewUrl
            )
        } else null
        val musicStartSec = inputData.getInt(KEY_MUSIC_START_SEC, 0)
        val muteOriginalAudio = inputData.getBoolean(KEY_MUTE_ORIGINAL_AUDIO, false)
        val videoStartMs = inputData.getLong(KEY_VIDEO_START_MS, 0L)
        val videoEndMs = inputData.getLong(KEY_VIDEO_END_MS, 0L)
        val videoSpeed = inputData.getDouble(KEY_VIDEO_SPEED, 1.0)
        val videoFilter = inputData.getString(KEY_VIDEO_FILTER)?.takeIf { it.isNotBlank() }
        val videoOverlaysJson = inputData.getString(KEY_VIDEO_OVERLAYS_JSON)?.takeIf { it.isNotBlank() }

        val stagedFile = File(stagedPath)
        if (stagedPath.isBlank() || !stagedFile.exists()) {
            StoryUploadTracker.markFailure(uploadId, "Story media file was removed before upload.")
            return Result.failure()
        }

        val auth = Firebase.auth
        val uid = auth.currentUser?.uid
        if (uid == null) {
            StoryUploadTracker.markFailure(uploadId, "Sign in required to share story.")
            runCatching { stagedFile.delete() }
            return Result.failure()
        }

        val db = FirebaseFirestore.getInstance(applicationContext.getString(R.string.firestore_database_id))

        return try {
            StoryUploadTracker.updateProgress(uploadId, 0.12f, "Uploading story media...")

            val profileSnap = db.collection("campus_profiles").document(uid).get().await()
            val author = profileSnap.toObject(CampusUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return failAndCleanup(uploadId, stagedFile, "Campus profile not loaded.")

            val isAdmin = isAdminInput || author.role == "admin"
            if (author.isSuspended && !isAdmin) {
                return failAndCleanup(uploadId, stagedFile, "Suspended accounts cannot share stories.")
            }

            val uploadRes = CampusMediaRepository.uploadStagedStoryFile(
                context = applicationContext,
                file = stagedFile,
                isVideo = isVideo,
                isAdmin = isAdmin,
                db = db,
                auth = auth,
                onProgress = { p ->
                    val scaled = 0.12f + p.coerceIn(0f, 1f) * 0.78f
                    val pct = (scaled * 100).toInt().coerceIn(1, 99)
                    StoryUploadTracker.updateProgress(uploadId, scaled, "Uploading story... $pct%")
                }
            )

            val mediaUrl = uploadRes.getOrElse { err ->
                if (!isPermanentError(err) && runAttemptCount < 2) {
                    StoryUploadTracker.updateProgress(uploadId, 0.15f, "Retrying story upload...")
                    return Result.retry()
                }
                return failAndCleanup(uploadId, stagedFile, err.localizedMessage ?: "Failed to upload story media.")
            }

            StoryUploadTracker.updateProgress(uploadId, 0.94f, "Publishing story...")
            val repo = CampusRepository(db, auth)
            val createRes = repo.createStory(
                author = author,
                isAdmin = isAdmin,
                mediaUrl = mediaUrl,
                mediaType = if (isVideo) "video" else "image",
                caption = caption,
                musicTrack = musicTrack,
                musicStartSec = musicStartSec,
                muteOriginalAudio = muteOriginalAudio,
                videoStartMs = videoStartMs,
                videoEndMs = videoEndMs,
                videoSpeed = videoSpeed,
                videoFilter = videoFilter,
                videoOverlaysJson = videoOverlaysJson
            )

            createRes.fold(
                onSuccess = {
                    runCatching { stagedFile.delete() }
                    StoryUploadTracker.markSuccess(uploadId)
                    if (!CampusNotificationCoordinator.isAppInForeground) {
                        NotificationHelper.showStoryUploadNotification(
                            context = applicationContext,
                            title = "Story Shared",
                            message = "Your story is now live for your followers for 24 hours."
                        )
                    }
                    delay(2800L)
                    if (StoryUploadTracker.state.value?.uploadId == uploadId &&
                        StoryUploadTracker.state.value?.isCompleted == true
                    ) {
                        StoryUploadTracker.clear()
                    }
                    Result.success()
                },
                onFailure = { err ->
                    if (!isPermanentError(err) && runAttemptCount < 2) {
                        Result.retry()
                    } else {
                        failAndCleanup(uploadId, stagedFile, err.localizedMessage ?: "Failed to publish story.")
                    }
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.w(TAG, "Story upload worker error: ${t.message}")
            if (!isPermanentError(t) && runAttemptCount < 2) {
                Result.retry()
            } else {
                failAndCleanup(uploadId, stagedFile, t.localizedMessage ?: "Story upload interrupted.")
            }
        }
    }

    private fun isPermanentError(t: Throwable): Boolean {
        var cur: Throwable? = t
        while (cur != null) {
            when (cur) {
                is IllegalArgumentException,
                is SecurityException,
                is OutOfMemoryError -> return true
                is FirebaseFirestoreException -> {
                    if (cur.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                        cur.code == FirebaseFirestoreException.Code.INVALID_ARGUMENT ||
                        cur.code == FirebaseFirestoreException.Code.UNAUTHENTICATED ||
                        cur.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION
                    ) {
                        return true
                    }
                }
            }
            cur = cur.cause
        }
        return false
    }

    private fun failAndCleanup(uploadId: String, file: File, message: String): Result {
        runCatching { file.delete() }
        StoryUploadTracker.markFailure(uploadId, message)
        return Result.failure()
    }

    companion object {
        private const val TAG = "StoryUploadWorker"
        private const val KEY_UPLOAD_ID = "upload_id"
        private const val KEY_STAGED_PATH = "staged_path"
        private const val KEY_IS_VIDEO = "is_video"
        private const val KEY_CAPTION = "caption"
        private const val KEY_IS_ADMIN = "is_admin"
        private const val KEY_MUSIC_TITLE = "music_title"
        private const val KEY_MUSIC_ARTIST = "music_artist"
        private const val KEY_MUSIC_ARTWORK_URL = "music_artwork_url"
        private const val KEY_MUSIC_PREVIEW_URL = "music_preview_url"
        private const val KEY_MUSIC_START_SEC = "music_start_sec"
        private const val KEY_MUTE_ORIGINAL_AUDIO = "mute_original_audio"
        private const val KEY_VIDEO_START_MS = "video_start_ms"
        private const val KEY_VIDEO_END_MS = "video_end_ms"
        private const val KEY_VIDEO_SPEED = "video_speed"
        private const val KEY_VIDEO_FILTER = "video_filter"
        private const val KEY_VIDEO_OVERLAYS_JSON = "video_overlays_json"

        fun enqueue(
            context: Context,
            uploadId: String,
            stagedFile: File,
            isVideo: Boolean,
            caption: String,
            isAdmin: Boolean,
            musicTrack: StoryMusicTrack? = null,
            musicStartSec: Int = 0,
            muteOriginalAudio: Boolean = false,
            videoStartMs: Long = 0L,
            videoEndMs: Long = 0L,
            videoSpeed: Double = 1.0,
            videoFilter: String? = null,
            videoOverlaysJson: String? = null
        ) {
            val appCtx = context.applicationContext
            StoryUploadTracker.start(
                uploadId = uploadId,
                isVideo = isVideo,
                previewPath = if (isVideo) null else stagedFile.absolutePath
            )
            val wm = try {
                if (!WorkManager.isInitialized()) {
                    WorkManager.initialize(appCtx, Configuration.Builder().build())
                }
                WorkManager.getInstance(appCtx)
            } catch (t: Throwable) {
                Log.w(TAG, "WorkManager unavailable for story upload: ${t.message}")
                StoryUploadTracker.markFailure(uploadId, "Background uploader unavailable")
                return
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<StoryUploadWorker>()
                .setConstraints(constraints)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 5L, TimeUnit.SECONDS)
                .setInputData(
                    workDataOf(
                        KEY_UPLOAD_ID to uploadId,
                        KEY_STAGED_PATH to stagedFile.absolutePath,
                        KEY_IS_VIDEO to isVideo,
                        KEY_CAPTION to caption.take(300),
                        KEY_IS_ADMIN to isAdmin,
                        KEY_MUSIC_TITLE to (musicTrack?.title?.take(120) ?: ""),
                        KEY_MUSIC_ARTIST to (musicTrack?.artist?.take(120) ?: ""),
                        KEY_MUSIC_ARTWORK_URL to (musicTrack?.artworkUrl?.take(500) ?: ""),
                        KEY_MUSIC_PREVIEW_URL to (musicTrack?.previewUrl?.take(500) ?: ""),
                        KEY_MUSIC_START_SEC to musicStartSec.coerceIn(0, 60),
                        KEY_MUTE_ORIGINAL_AUDIO to muteOriginalAudio,
                        KEY_VIDEO_START_MS to videoStartMs,
                        KEY_VIDEO_END_MS to videoEndMs,
                        KEY_VIDEO_SPEED to videoSpeed,
                        KEY_VIDEO_FILTER to (videoFilter ?: ""),
                        KEY_VIDEO_OVERLAYS_JSON to (videoOverlaysJson?.take(6000) ?: "")
                    )
                )
                .build()

            wm.enqueueUniqueWork(
                "story_upload_$uploadId",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
