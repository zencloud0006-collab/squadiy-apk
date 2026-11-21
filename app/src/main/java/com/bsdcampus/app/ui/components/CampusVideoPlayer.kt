package com.bsdcampus.app.ui.components

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import com.bsdcampus.app.R
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.bsdcampus.app.data.CampusChunkedVideoCache
import com.bsdcampus.app.data.CampusMediaRepository
import com.bsdcampus.app.data.MediaUtils
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun CampusVideoPlayer(
    videoUri: String,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = false,
    initialMuted: Boolean = false,
    forceMuted: Boolean? = null,
    startMs: Long = 0L,
    endMs: Long = 0L,
    playbackSpeed: Float = 1f,
    paused: Boolean = false,
    showControls: Boolean = true,
    showVideoBadge: Boolean = true,
    fitVideoToBounds: Boolean = false,
    onVideoSizeChanged: ((Float) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isChunked = MediaUtils.isChunkedMediaUri(videoUri)
    val cachedLocalFile = remember(videoUri) {
        if (isChunked) {
            MediaUtils.extractAssetId(videoUri)?.let { CampusChunkedVideoCache.cachedMp4File(context, it) }
        } else null
    }

    var localFile by remember(videoUri) { mutableStateOf<File?>(cachedLocalFile) }
    var isLoading by remember(videoUri) { mutableStateOf(false) }
    var isBuffering by remember(videoUri) { mutableStateOf(false) }
    var loadError by remember(videoUri) { mutableStateOf<String?>(null) }
    var shouldPlay by remember(videoUri) { mutableStateOf(autoPlay) }
    var userMuted by remember(videoUri) { mutableStateOf(initialMuted) }
    val isMuted = forceMuted ?: userMuted
    var isActuallyPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(videoUri, shouldPlay) {
        if (!isChunked && shouldPlay && localFile == null && !isLoading) {
            isLoading = true
            loadError = null
            CampusMediaRepository.resolveVideoFile(context, videoUri)
                .onSuccess { file -> localFile = file }
                .onFailure { loadError = it.localizedMessage ?: "Unable to load video" }
            isLoading = false
        }
    }

    val shouldCreatePlayer = shouldPlay && (isChunked || localFile != null)
    val exoPlayer = remember(videoUri, shouldCreatePlayer, localFile, startMs, endMs) {
        if (!shouldCreatePlayer) return@remember null
        val fileToPlay = localFile
        val rawItemBuilder = if (fileToPlay != null) {
            MediaItem.Builder().setUri(Uri.fromFile(fileToPlay))
        } else {
            MediaItem.Builder().setUri(videoUri)
        }
        if (endMs > startMs && endMs > 0L) {
            rawItemBuilder.setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs.coerceAtLeast(0L))
                    .setEndPositionMs(endMs)
                    .build()
            )
        }
        val item = rawItemBuilder.build()
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(2_000, 12_000, 350, 800)
            .build()
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
            .apply {
                if (fileToPlay == null && isChunked) {
                    setMediaSource(
                        ProgressiveMediaSource.Factory(CampusChunkedVideoCache.Factory(context))
                            .createMediaSource(item)
                    )
                } else {
                    setMediaItem(item)
                }
                repeatMode = Player.REPEAT_MODE_ALL
                volume = if (isMuted) 0f else 1f
                setPlaybackSpeed(playbackSpeed.coerceIn(0.25f, 3f))
                prepare()
                playWhenReady = shouldPlay && !paused
            }
    }
    var hasRenderedFirstFrame by remember(exoPlayer) { mutableStateOf(false) }

    LaunchedEffect(exoPlayer, isMuted) {
        exoPlayer?.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(exoPlayer, playbackSpeed) {
        exoPlayer?.setPlaybackSpeed(playbackSpeed.coerceIn(0.25f, 3f))
    }

    LaunchedEffect(autoPlay) {
        shouldPlay = autoPlay
        val player = exoPlayer
        if (player != null) {
            if (autoPlay && !paused) {
                player.play()
            } else if (!autoPlay) {
                player.pause()
            }
        }
    }

    LaunchedEffect(exoPlayer, paused, shouldPlay) {
        val player = exoPlayer ?: return@LaunchedEffect
        if (paused || !shouldPlay) {
            player.pause()
        } else {
            player.play()
        }
    }

    DisposableEffect(exoPlayer) {
        val player = exoPlayer
        if (player == null) {
            onDispose {}
        } else {
            val listener = object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    isActuallyPlaying = isPlaying
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    isBuffering = playbackState == Player.STATE_BUFFERING
                    if (playbackState == Player.STATE_READY) {
                        loadError = null
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    isBuffering = false
                    loadError = error.localizedMessage ?: "Unable to play video"
                }

                override fun onRenderedFirstFrame() {
                    hasRenderedFirstFrame = true
                    isBuffering = false
                }

                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        val ratio = ((videoSize.width.toFloat() * videoSize.pixelWidthHeightRatio) /
                            videoSize.height.toFloat()).coerceIn(0.45f, 2.1f)
                        onVideoSizeChanged?.invoke(ratio)
                    }
                }
            }
            player.addListener(listener)
            onDispose {
                player.removeListener(listener)
                player.release()
            }
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                exoPlayer?.pause()
            } else if (event == Lifecycle.Event.ON_RESUME && shouldPlay && !paused) {
                exoPlayer?.play()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
            .then(
                if (showControls) {
                    Modifier.clickable {
                        if (exoPlayer == null) {
                            shouldPlay = true
                        } else if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                            shouldPlay = false
                        } else {
                            exoPlayer.play()
                            shouldPlay = true
                        }
                    }
                } else Modifier
            )
    ) {
        if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    (LayoutInflater.from(ctx).inflate(
                        R.layout.campus_texture_player_view,
                        null,
                        false
                    ) as PlayerView).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = if (fitVideoToBounds) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT
                        } else {
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        }
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { view ->
                    if (view.player !== exoPlayer) {
                        view.player = exoPlayer
                    }
                    val targetMode = if (fitVideoToBounds) {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT
                    } else {
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    }
                    if (view.resizeMode != targetMode) {
                        view.resizeMode = targetMode
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        if (exoPlayer == null || !hasRenderedFirstFrame) {
            CampusImage(
                urlOrDataUri = videoUri,
                contentDescription = "Video thumbnail",
                contentScale = if (fitVideoToBounds) ContentScale.Fit else ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isLoading || (isBuffering && !hasRenderedFirstFrame)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.5.dp, modifier = Modifier.size(32.dp))
                    Text("Buffering video...", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
            }
        } else if (loadError != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.72f)
            ) {
                Text(
                    text = loadError ?: "Tap to retry",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        } else if (showControls && !isActuallyPlaying && exoPlayer != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play video",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        if (showControls) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (exoPlayer != null) {
                    IconButton(
                        onClick = {
                            if (exoPlayer.isPlaying) {
                                exoPlayer.pause()
                                shouldPlay = false
                            } else {
                                exoPlayer.play()
                                shouldPlay = true
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = if (isActuallyPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isActuallyPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (forceMuted == null) {
                    IconButton(
                        onClick = { userMuted = !userMuted },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }

        if (showVideoBadge) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.58f)
            ) {
                Text(
                    text = "VIDEO",
                    color = Color.White,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
