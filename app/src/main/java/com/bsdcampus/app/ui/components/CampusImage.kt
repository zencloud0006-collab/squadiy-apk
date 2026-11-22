package com.bsdcampus.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bsdcampus.app.data.CampusMediaRepository
import com.bsdcampus.app.data.MediaUtils

@Composable
fun CampusImage(
    urlOrDataUri: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallbackText: String? = null
) {
    val context = LocalContext.current
    val isChunked = MediaUtils.isChunkedMediaUri(urlOrDataUri)
    val isRemoteHttp = MediaUtils.isRemoteHttpUri(urlOrDataUri)
    val isVideo = MediaUtils.isVideoMediaUri(urlOrDataUri)

    val dataBitmap = remember(urlOrDataUri) {
        val bytes = MediaUtils.decodeDataUriBytes(urlOrDataUri)
        if (bytes != null && bytes.isNotEmpty()) {
            try {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        } else null
    }

    val chunkedBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = urlOrDataUri) {
        value = when {
            isChunked && !urlOrDataUri.isNullOrBlank() -> {
                val bytes = CampusMediaRepository.resolveMediaImageBytes(context, urlOrDataUri)
                if (bytes != null && bytes.isNotEmpty()) {
                    try {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    } catch (_: Exception) {
                        null
                    }
                } else null
            }
            isRemoteHttp && isVideo && !urlOrDataUri.isNullOrBlank() -> {
                val bytes = CampusMediaRepository.resolveRemoteVideoThumbnailBytes(context, urlOrDataUri)
                if (bytes != null && bytes.isNotEmpty()) {
                    try {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    } catch (_: Exception) {
                        null
                    }
                } else null
            }
            else -> null
        }
    }

    val resolvedBitmap = dataBitmap ?: chunkedBitmap

    when {
        resolvedBitmap != null -> {
            Box(modifier = modifier) {
                Image(
                    bitmap = resolvedBitmap,
                    contentDescription = contentDescription,
                    modifier = Modifier.matchParentSize(),
                    contentScale = contentScale
                )
                if (isVideo) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Video",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
        isChunked || (isRemoteHttp && isVideo) -> {
            Box(
                modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (isVideo) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Loading video thumbnail",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                }
            }
        }
        !urlOrDataUri.isNullOrBlank() -> {
            AsyncImage(
                model = urlOrDataUri,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        else -> {
            Box(
                modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                val initials = fallbackText
                    ?.trim()
                    ?.split(" ")
                    ?.take(2)
                    ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    ?.joinToString("")
                    ?.ifBlank { "B" } ?: "B"
                Text(
                    text = initials,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
