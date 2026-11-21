package com.bsdcampus.app.notifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class StoryUploadState(
    val uploadId: String,
    val isVideo: Boolean,
    val previewPath: String?,
    val progress: Float = 0.05f,
    val statusLabel: String = "Uploading story...",
    val isCompleted: Boolean = false,
    val isFailed: Boolean = false,
    val errorMessage: String? = null
)

object StoryUploadTracker {
    private val _state = MutableStateFlow<StoryUploadState?>(null)
    val state: StateFlow<StoryUploadState?> = _state.asStateFlow()

    fun start(uploadId: String, isVideo: Boolean, previewPath: String?) {
        _state.value = StoryUploadState(
            uploadId = uploadId,
            isVideo = isVideo,
            previewPath = previewPath,
            progress = 0.08f,
            statusLabel = if (isVideo) "Uploading video story in background..." else "Uploading story in background..."
        )
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

    fun markSuccess(uploadId: String) {
        val current = _state.value
        if (current == null || current.uploadId == uploadId) {
            _state.value = (current ?: StoryUploadState(uploadId, isVideo = false, previewPath = null)).copy(
                progress = 1f,
                statusLabel = "Story shared for 24h!",
                isCompleted = true,
                isFailed = false
            )
        }
    }

    fun markFailure(uploadId: String, message: String) {
        val current = _state.value
        _state.value = (current ?: StoryUploadState(uploadId, isVideo = false, previewPath = null)).copy(
            statusLabel = "Story upload failed",
            isCompleted = false,
            isFailed = true,
            errorMessage = message
        )
    }

    fun clear() {
        _state.value = null
    }
}
