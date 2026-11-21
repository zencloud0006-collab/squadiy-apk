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
import com.bsdcampus.app.model.PostImageRatio
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

class PostReelUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        configureAppCheck(applicationContext, null)
        NotificationHelper.ensureChannels(applicationContext)

        val uploadId = inputData.getString(KEY_UPLOAD_ID).orEmpty()
        val kind = if (inputData.getString(KEY_KIND) == MediaUploadKind.REEL.name) {
            MediaUploadKind.REEL
        } else {
            MediaUploadKind.POST
        }
        val stagedPaths = inputData.getStringArray(KEY_STAGED_PATHS).orEmpty().toList()
        val isVideos = inputData.getBooleanArray(KEY_IS_VIDEOS)?.toList().orEmpty()
        val content = inputData.getString(KEY_CONTENT).orEmpty()
        val category = inputData.getString(KEY_CATEGORY).orEmpty().ifBlank { "General" }
        val imageAspectRatio = PostImageRatio.fromKey(inputData.getString(KEY_IMAGE_ASPECT_RATIO)).key
        val videoAspectRatio = PostImageRatio.fromKey(
            inputData.getString(KEY_VIDEO_ASPECT_RATIO) ?: PostImageRatio.FREE.key
        ).key
        val isOfficial = inputData.getBoolean(KEY_IS_OFFICIAL, false)
        val isPinned = inputData.getBoolean(KEY_IS_PINNED, false)
        val audioTitle = inputData.getString(KEY_AUDIO_TITLE).orEmpty().ifBlank { "Original audio" }
        val muteOriginalAudio = inputData.getBoolean(KEY_MUTE_ORIGINAL_AUDIO, false)
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

        val stagedFiles = stagedPaths.map { File(it) }
        if (kind == MediaUploadKind.REEL && stagedFiles.isEmpty()) {
            MediaUploadTracker.markFailure(uploadId, kind, "Reel video file was missing.")
            return Result.failure()
        }
        if (stagedFiles.any { !it.exists() }) {
            cleanupFiles(stagedFiles)
            MediaUploadTracker.markFailure(uploadId, kind, "Media file was cleared before upload finished.")
            return Result.failure()
        }

        val auth = Firebase.auth
        val uid = auth.currentUser?.uid
        if (uid == null) {
            cleanupFiles(stagedFiles)
            MediaUploadTracker.markFailure(uploadId, kind, "Sign in required to publish.")
            return Result.failure()
        }

        val db = FirebaseFirestore.getInstance(applicationContext.getString(R.string.firestore_database_id))

        return try {
            val profileSnap = db.collection("campus_profiles").document(uid).get().await()
            val author = profileSnap.toObject(CampusUser::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: return failAndCleanup(uploadId, kind, stagedFiles, "Campus profile not loaded.")

            val isAdmin = isAdminInput || author.role == "admin"
            if (author.isSuspended && !isAdmin) {
                return failAndCleanup(uploadId, kind, stagedFiles, "Suspended accounts cannot publish.")
            }

            val uploadedUrls = mutableListOf<String>()
            val totalFiles = stagedFiles.size.coerceAtLeast(1)

            for ((index, stagedFile) in stagedFiles.withIndex()) {
                val fileIsVideo = isVideos.getOrElse(index) { false }
                val baseProgress = 0.12f + (index.toFloat() / totalFiles.toFloat()) * 0.78f
                val stepRange = (0.78f / totalFiles.toFloat())
                val itemLabel = if (kind == MediaUploadKind.REEL) {
                    "Uploading reel video..."
                } else if (totalFiles > 1) {
                    "Uploading media ${index + 1}/$totalFiles..."
                } else {
                    if (fileIsVideo) "Uploading post video..." else "Uploading post photo..."
                }
                MediaUploadTracker.updateProgress(uploadId, baseProgress, itemLabel)

                val uploadRes = CampusMediaRepository.uploadStagedMediaFile(
                    context = applicationContext,
                    file = stagedFile,
                    isVideo = fileIsVideo,
                    isAdmin = isAdmin,
                    db = db,
                    auth = auth,
                    onProgress = { p ->
                        val scaled = baseProgress + p.coerceIn(0f, 1f) * stepRange
                        val pct = (scaled * 100).toInt().coerceIn(1, 99)
                        val kindWord = if (kind == MediaUploadKind.REEL) "reel" else "post"
                        MediaUploadTracker.updateProgress(uploadId, scaled, "Uploading $kindWord... $pct%")
                    }
                )

                val uploadedUrl = uploadRes.getOrElse { err ->
                    if (!isPermanentError(err) && runAttemptCount < 2) {
                        MediaUploadTracker.updateProgress(uploadId, 0.15f, "Retrying upload...")
                        return Result.retry()
                    }
                    return failAndCleanup(
                        uploadId,
                        kind,
                        stagedFiles,
                        err.localizedMessage ?: "Failed to upload media."
                    )
                }
                uploadedUrls.add(uploadedUrl)
            }

            MediaUploadTracker.updateProgress(
                uploadId,
                0.94f,
                if (kind == MediaUploadKind.REEL) "Publishing reel..." else "Publishing post..."
            )
            val repo = CampusRepository(db, auth)
            val createRes: kotlin.Result<String> = if (kind == MediaUploadKind.REEL) {
                val reelVideoUrl = uploadedUrls.firstOrNull().orEmpty()
                val resolvedAudioTitle = if (musicTrack != null) {
                    "${musicTrack.title} · ${musicTrack.artist}"
                } else {
                    audioTitle
                }
                repo.createReel(
                    author = author,
                    isAdmin = isAdmin,
                    videoUrl = reelVideoUrl,
                    videoAspectRatio = videoAspectRatio,
                    caption = content,
                    category = category,
                    audioTitle = resolvedAudioTitle,
                    musicTrack = musicTrack,
                    musicStartSec = musicStartSec,
                    muteOriginalAudio = muteOriginalAudio
                )
            } else {
                repo.createPost(
                    author = author,
                    isAdmin = isAdmin,
                    content = content,
                    category = category,
                    images = uploadedUrls,
                    imageAspectRatio = imageAspectRatio,
                    isOfficial = isOfficial,
                    isPinned = isPinned,
                    musicTrack = musicTrack,
                    musicStartSec = musicStartSec
                )
            }

            createRes.fold(
                onSuccess = {
                    cleanupFiles(stagedFiles)
                    MediaUploadTracker.markSuccess(uploadId, kind)
                    if (!CampusNotificationCoordinator.isAppInForeground) {
                        NotificationHelper.showMediaUploadNotification(
                            context = applicationContext,
                            title = if (kind == MediaUploadKind.REEL) "Reel Published" else "Post Published",
                            message = if (kind == MediaUploadKind.REEL) {
                                "Your reel is now live on Squadify."
                            } else {
                                "Your post is now live on the Squadify feed."
                            }
                        )
                    }
                    delay(2800L)
                    if (MediaUploadTracker.state.value?.uploadId == uploadId &&
                        MediaUploadTracker.state.value?.isCompleted == true
                    ) {
                        MediaUploadTracker.clear()
                    }
                    Result.success()
                },
                onFailure = { err ->
                    if (!isPermanentError(err) && runAttemptCount < 2) {
                        Result.retry()
                    } else {
                        failAndCleanup(uploadId, kind, stagedFiles, err.localizedMessage ?: "Failed to publish.")
                    }
                }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.w(TAG, "Background upload error: ${t.message}")
            if (!isPermanentError(t) && runAttemptCount < 2) {
                Result.retry()
            } else {
                failAndCleanup(uploadId, kind, stagedFiles, t.localizedMessage ?: "Upload interrupted.")
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

    private fun cleanupFiles(files: List<File>) {
        files.forEach { f -> runCatching { f.delete() } }
    }

    private fun failAndCleanup(
        uploadId: String,
        kind: MediaUploadKind,
        files: List<File>,
        message: String
    ): Result {
        cleanupFiles(files)
        MediaUploadTracker.markFailure(uploadId, kind, message)
        return Result.failure()
    }

    companion object {
        private const val TAG = "PostReelUploadWorker"
        private const val KEY_UPLOAD_ID = "upload_id"
        private const val KEY_KIND = "kind"
        private const val KEY_STAGED_PATHS = "staged_paths"
        private const val KEY_IS_VIDEOS = "is_videos"
        private const val KEY_CONTENT = "content"
        private const val KEY_CATEGORY = "category"
        private const val KEY_IMAGE_ASPECT_RATIO = "image_aspect_ratio"
        private const val KEY_VIDEO_ASPECT_RATIO = "video_aspect_ratio"
        private const val KEY_IS_OFFICIAL = "is_official"
        private const val KEY_IS_PINNED = "is_pinned"
        private const val KEY_AUDIO_TITLE = "audio_title"
        private const val KEY_MUTE_ORIGINAL_AUDIO = "mute_original_audio"
        private const val KEY_IS_ADMIN = "is_admin"
        private const val KEY_MUSIC_TITLE = "music_title"
        private const val KEY_MUSIC_ARTIST = "music_artist"
        private const val KEY_MUSIC_ARTWORK_URL = "music_artwork_url"
        private const val KEY_MUSIC_PREVIEW_URL = "music_preview_url"
        private const val KEY_MUSIC_START_SEC = "music_start_sec"

        fun enqueue(
            context: Context,
            uploadId: String,
            kind: MediaUploadKind,
            stagedFiles: List<File>,
            isVideos: List<Boolean>,
            content: String,
            category: String,
            imageAspectRatio: String = PostImageRatio.FREE.key,
            videoAspectRatio: String = PostImageRatio.FREE.key,
            isOfficial: Boolean = false,
            isPinned: Boolean = false,
            audioTitle: String = "Original audio",
            muteOriginalAudio: Boolean = false,
            isAdmin: Boolean = false,
            musicTrack: StoryMusicTrack? = null,
            musicStartSec: Int = 0
        ) {
            val appCtx = context.applicationContext
            val wm = try {
                if (!WorkManager.isInitialized()) {
                    WorkManager.initialize(appCtx, Configuration.Builder().build())
                }
                WorkManager.getInstance(appCtx)
            } catch (t: Throwable) {
                Log.w(TAG, "WorkManager unavailable: ${t.message}")
                stagedFiles.forEach { runCatching { it.delete() } }
                MediaUploadTracker.markFailure(uploadId, kind, "Background uploader unavailable")
                return
            }

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<PostReelUploadWorker>()
                .setConstraints(constraints)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 5L, TimeUnit.SECONDS)
                .setInputData(
                    workDataOf(
                        KEY_UPLOAD_ID to uploadId,
                        KEY_KIND to kind.name,
                        KEY_STAGED_PATHS to stagedFiles.map { it.absolutePath }.toTypedArray(),
                        KEY_IS_VIDEOS to isVideos.toBooleanArray(),
                        KEY_CONTENT to content.take(2000),
                        KEY_CATEGORY to category,
                        KEY_IMAGE_ASPECT_RATIO to imageAspectRatio,
                        KEY_VIDEO_ASPECT_RATIO to videoAspectRatio,
                        KEY_IS_OFFICIAL to isOfficial,
                        KEY_IS_PINNED to isPinned,
                        KEY_AUDIO_TITLE to audioTitle.take(120),
                        KEY_MUTE_ORIGINAL_AUDIO to muteOriginalAudio,
                        KEY_IS_ADMIN to isAdmin,
                        KEY_MUSIC_TITLE to (musicTrack?.title?.take(120) ?: ""),
                        KEY_MUSIC_ARTIST to (musicTrack?.artist?.take(120) ?: ""),
                        KEY_MUSIC_ARTWORK_URL to (musicTrack?.artworkUrl?.take(500) ?: ""),
                        KEY_MUSIC_PREVIEW_URL to (musicTrack?.previewUrl?.take(500) ?: ""),
                        KEY_MUSIC_START_SEC to musicStartSec.coerceIn(0, 60)
                    )
                )
                .build()

            wm.enqueueUniqueWork(
                "media_upload_$uploadId",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
