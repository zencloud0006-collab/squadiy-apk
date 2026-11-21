package com.bsdcampus.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.bsdcampus.app.data.MusicSearchRepository
import com.bsdcampus.app.data.StoryMusicTrack
import com.bsdcampus.app.ui.theme.InstaBlue
import com.bsdcampus.app.ui.theme.InstaLikeRed
import com.bsdcampus.app.ui.theme.InstaPink
import com.bsdcampus.app.ui.theme.InstaPurple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StoryMusicStickerPill(
    title: String,
    artist: String,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "musicEq")
    val barPulse by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "barPulse"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xCC111116),
        border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.22f)),
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(listOf(InstaPurple, InstaPink))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                if (!artworkUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = artworkUrl,
                        contentDescription = "$title artwork",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = if (isPlaying) InstaPink.copy(alpha = barPulse) else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = artist,
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (onRemove != null) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Remove music",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryMusicPickerSheet(
    currentTrack: StoryMusicTrack?,
    currentStartSec: Int,
    allowMuteVideoAudio: Boolean,
    muteOriginalAudio: Boolean,
    onApply: (track: StoryMusicTrack?, startSec: Int, muteOriginalAudio: Boolean) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Add Music to Story"
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var searchQuery by remember { mutableStateOf("") }
    var activeChipLabel by remember { mutableStateOf(MusicSearchRepository.FEATURED_CHIPS.first().first) }
    var tracks by remember { mutableStateOf<List<StoryMusicTrack>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedTrack by remember { mutableStateOf(currentTrack) }
    var startSec by remember { mutableIntStateOf(currentStartSec.coerceIn(0, 15)) }
    var muteVideo by remember { mutableStateOf(muteOriginalAudio) }
    var previewingUrl by remember { mutableStateOf<String?>(null) }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    val previewPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPreviewPlaying = playing
            }
        }
        previewPlayer.addListener(listener)
        onDispose {
            previewPlayer.removeListener(listener)
            previewPlayer.release()
        }
    }

    fun togglePreview(track: StoryMusicTrack, seekSec: Int = 0) {
        if (previewingUrl == track.previewUrl && previewPlayer.isPlaying) {
            previewPlayer.pause()
        } else {
            previewingUrl = track.previewUrl
            previewPlayer.setMediaItem(MediaItem.fromUri(track.previewUrl))
            previewPlayer.prepare()
            if (seekSec > 0) {
                previewPlayer.seekTo(seekSec * 1000L)
            }
            previewPlayer.playWhenReady = true
            previewPlayer.play()
        }
    }

    LaunchedEffect(searchQuery) {
        isLoading = true
        errorMessage = null
        if (searchQuery.isNotBlank()) {
            delay(320L)
        }
        val queryToRun = searchQuery.trim().ifBlank {
            MusicSearchRepository.FEATURED_CHIPS.firstOrNull { it.first == activeChipLabel }?.second
                ?: "bollywood hits"
        }
        MusicSearchRepository.searchSongs(queryToRun)
            .onSuccess { list ->
                tracks = list
                errorMessage = null
            }
            .onFailure { err ->
                errorMessage = err.localizedMessage ?: "Couldn't load songs right now."
            }
        isLoading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF101014),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = InstaPink,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedTrack != null || currentTrack != null) {
                        TextButton(
                            onClick = {
                                previewPlayer.pause()
                                onApply(null, 0, muteVideo)
                                onDismiss()
                            },
                            modifier = Modifier.testTag("music_remove_button")
                        ) {
                            Text("Remove", color = InstaLikeRed, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close music picker", tint = Color.White)
                    }
                }
            }

            // Live Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it.take(80) },
                placeholder = {
                    Text("Search songs, singers or mood...", color = Color.White.copy(alpha = 0.52f), fontSize = 14.sp)
                },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.65f))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = Color.White.copy(alpha = 0.7f))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = InstaBlue,
                    unfocusedBorderColor = Color(0xFF2C2C34),
                    focusedContainerColor = Color(0xFF18181F),
                    unfocusedContainerColor = Color(0xFF18181F),
                    cursorColor = InstaBlue
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("music_search_input")
            )

            // Featured Genre Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MusicSearchRepository.FEATURED_CHIPS.forEach { (label, term) ->
                    val selected = searchQuery.isBlank() && activeChipLabel == label
                    FilterChip(
                        selected = selected,
                        onClick = {
                            activeChipLabel = label
                            searchQuery = ""
                            scope.launch {
                                isLoading = true
                                MusicSearchRepository.searchSongs(term)
                                    .onSuccess { list ->
                                        tracks = list
                                        errorMessage = null
                                    }
                                    .onFailure { err ->
                                        errorMessage = err.localizedMessage ?: "Search failed"
                                    }
                                isLoading = false
                            }
                        },
                        label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = InstaPink.copy(alpha = 0.25f),
                            selectedLabelColor = InstaPink,
                            containerColor = Color(0xFF1A1A22),
                            labelColor = Color.White.copy(alpha = 0.85f)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = Color(0xFF2E2E38),
                            selectedBorderColor = InstaPink
                        )
                    )
                }
            }

            // Video Original Sound Mute Toggle (when editing video story or reel repost)
            if (allowMuteVideoAudio) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF17171E),
                    border = BorderStroke(0.75.dp, Color(0xFF2B2B36))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = null,
                                tint = if (muteVideo) InstaBlue else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "Mute original video sound",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Let the selected song lead cleanly",
                                    color = Color.White.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                        Switch(
                            checked = muteVideo,
                            onCheckedChange = { muteVideo = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = InstaBlue
                            ),
                            modifier = Modifier.testTag("music_mute_video_switch")
                        )
                    }
                }
            }

            // Selected Track 15s Clip Scrubber & Apply Card
            selectedTrack?.let { chosen ->
                val maxStart = ((chosen.durationMs / 1000L).toInt() - 10).coerceIn(0, 18)
                val safeStart = startSec.coerceIn(0, maxStart)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1B1626),
                    border = BorderStroke(1.dp, InstaPink.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Brush.linearGradient(listOf(InstaPurple, InstaPink))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White)
                                if (chosen.artworkUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = chosen.artworkUrl,
                                        contentDescription = chosen.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.matchParentSize()
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = chosen.title,
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${chosen.artist} · Clip ${safeStart}s–${safeStart + 15}s",
                                    color = Color.White.copy(alpha = 0.75f),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(
                                onClick = { togglePreview(chosen, safeStart) },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.14f))
                            ) {
                                val activePlaying = previewingUrl == chosen.previewUrl && isPreviewPlaying
                                Icon(
                                    imageVector = if (activePlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (activePlaying) "Pause preview" else "Play preview",
                                    tint = Color.White
                                )
                            }
                        }

                        if (maxStart > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Part", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
                                Slider(
                                    value = safeStart.toFloat(),
                                    onValueChange = {
                                        val nextSec = it.toInt().coerceIn(0, maxStart)
                                        startSec = nextSec
                                        if (previewingUrl == chosen.previewUrl) {
                                            previewPlayer.seekTo(nextSec * 1000L)
                                        }
                                    },
                                    valueRange = 0f..maxStart.toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = InstaPink,
                                        activeTrackColor = InstaPink,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Text("${safeStart}s", color = InstaPink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = {
                                previewPlayer.pause()
                                onApply(chosen, safeStart, muteVideo)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = InstaBlue, contentColor = Color.White),
                            contentPadding = PaddingValues(vertical = 11.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("music_apply_button")
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Use \"${chosen.title.take(24)}\"", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF272732))

            // Track Results List
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp, max = 320.dp)
            ) {
                when {
                    isLoading -> {
                        Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(color = InstaPink, strokeWidth = 2.5.dp, modifier = Modifier.size(28.dp))
                                Text("Searching songs...", color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    errorMessage != null -> {
                        Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = errorMessage ?: "Unable to fetch songs",
                                    color = Color.White.copy(alpha = 0.75f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                TextButton(
                                    onClick = {
                                        val term = searchQuery.trim().ifBlank { "bollywood hits" }
                                        scope.launch {
                                            isLoading = true
                                            MusicSearchRepository.searchSongs(term)
                                                .onSuccess {
                                                    tracks = it
                                                    errorMessage = null
                                                }
                                                .onFailure { err ->
                                                    errorMessage = err.localizedMessage ?: "Retry failed"
                                                }
                                            isLoading = false
                                        }
                                    }
                                ) {
                                    Text("Retry", color = InstaBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    tracks.isEmpty() -> {
                        Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "No matching tracks found. Try another keyword.",
                                color = Color.White.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(tracks, key = { "${it.trackId}_${it.previewUrl}" }) { track ->
                                val isSelected = selectedTrack?.previewUrl == track.previewUrl
                                val playingThis = previewingUrl == track.previewUrl && isPreviewPlaying
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFF1F1D2C) else Color(0xFF15151B),
                                    border = if (isSelected) BorderStroke(1.dp, InstaPink.copy(alpha = 0.65f)) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedTrack = track
                                            startSec = 0
                                            togglePreview(track, 0)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Brush.linearGradient(listOf(InstaPurple, InstaPink))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White)
                                            if (track.artworkUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = track.artworkUrl,
                                                    contentDescription = track.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.matchParentSize()
                                                )
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = track.title,
                                                color = if (isSelected) InstaPink else Color.White,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = track.artist,
                                                color = Color.White.copy(alpha = 0.65f),
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        IconButton(
                                            onClick = { togglePreview(track, if (isSelected) startSec else 0) },
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (playingThis) InstaPink.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)
                                                )
                                        ) {
                                            Icon(
                                                imageVector = if (playingThis) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                contentDescription = if (playingThis) "Pause track" else "Preview track",
                                                tint = if (playingThis) InstaPink else Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
