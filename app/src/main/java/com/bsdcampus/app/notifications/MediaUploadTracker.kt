package com.bsdcampus.app.notifications

import android.net.Uri
import com.bsdcampus.app.data.StoryEditParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class MediaUploadKind {
    POST,
    REEL
}

data class StagedDraftMediaInput(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val isVideo: Boolean,
    val editParams: StoryEditParams = StoryEditParams()
)

data class MediaUploadState(
    val uploadId: String,
    val kind: MediaUploadKind,
    val previewPath: String?,
    val itemCount: Int = 1,
    val progress: Float = 0.05f,
    val statusLabel: String = "Preparing upload...",
    val isCompleted: Boolean = false,
    val isFailed: Boolean = false,
    val errorMessage: String? = null
)

object MediaUploadTracker {
    private val _state = MutableStateFlow<MediaUploadState?>(null)
    val state: StateFlow<MediaUploadState?> = _state.asStateFlow()

    fun start(
        uploadId: String,
        kind: MediaUploadKind,
        itemCount: Int = 1,
        previewPath: String? = null,
        statusLabel: String? = null
    ) {
        val defaultLabel = when (kind) {
            MediaUploadKind.POST -> if (itemCount > 1) "Preparing $itemCount items..." else "Preparing post upload..."
            MediaUploadKind.REEL -> "Preparing reel upload..."
        }
        _state.value = MediaUploadState(
            uploadId = uploadId,
            kind = kind,
            previewPath = previewPath,
            itemCount = itemCount.coerceAtLeast(1),
            progress = 0.06f,
            statusLabel = statusLabel ?: defaultLabel
        )
    }

    fun updatePreviewPath(uploadId: String, previewPath: String?) {
        val current = _state.value
        if (current != null && current.uploadId == uploadId && !current.isCompleted && !current.isFailed) {
            _state.value = current.copy(previewPath = previewPath)
        }
    }

    fun updateProgress(uploadId: String, progress: Float, statusLabel: String? = null) {
        val current = _state.value
        if (current != null && current.uploadId == uploadId && !current.isCompleted && !current.isFailed) {
            _state.value = current.copy(
                progress = progress.coerceIn(0.05f, 0.99f),
                statusLabel = statusLabel ?: current.statusLabel
            )
        }
    }

    fun markSuccess(uploadId: String, kind: MediaUploadKind) {
        val current = _state.value
        val label = if (kind == MediaUploadKind.REEL) "Reel published!" else "Post published!"
        if (current == null || current.uploadId == uploadId) {
            _state.value = (current ?: MediaUploadState(uploadId, kind, previewPath = null)).copy(
                progress = 1f,
                statusLabel = label,
                isCompleted = true,
                isFailed = false,
                errorMessage = null
            )
        }
    }

    fun markFailure(uploadId: String, kind: MediaUploadKind, message: String) {
        val current = _state.value
        val label = if (kind == MediaUploadKind.REEL) "Reel upload failed" else "Post upload failed"
        _state.value = (current ?: MediaUploadState(uploadId, kind, previewPath = null)).copy(
            statusLabel = label,
            isCompleted = false,
            isFailed = true,
            errorMessage = message
        )
    }

    fun clear() {
        _state.value = null
    }
}
