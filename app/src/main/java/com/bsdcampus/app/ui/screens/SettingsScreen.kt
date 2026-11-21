package com.bsdcampus.app.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsdcampus.app.BuildConfig
import com.bsdcampus.app.data.AppThemeMode
import com.bsdcampus.app.data.CacheManager
import com.bsdcampus.app.data.CacheStats
import com.bsdcampus.app.data.PreferencesRepository
import com.bsdcampus.app.model.AppUpdateConfig
import com.bsdcampus.app.model.CampusConstants
import com.bsdcampus.app.model.CampusPost
import com.bsdcampus.app.model.CampusTab
import com.bsdcampus.app.model.CampusUser
import com.bsdcampus.app.model.FeedLayoutMode
import com.bsdcampus.app.model.UserPrivateInfo
import com.bsdcampus.app.notifications.CampusNotificationWorker
import com.bsdcampus.app.notifications.NotificationHelper
import com.bsdcampus.app.ui.auth.AuthHelper
import com.bsdcampus.app.ui.theme.Emerald500
import com.bsdcampus.app.ui.theme.InstaBlue
import com.bsdcampus.app.ui.theme.Rose500
import com.bsdcampus.app.ui.theme.Zinc700
import com.bsdcampus.app.ui.theme.Zinc900
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    currentUser: CampusUser,
    isAdmin: Boolean,
    googleEmail: String,
    privateInfo: UserPrivateInfo?,
    posts: List<CampusPost>,
    themeMode: AppThemeMode,
    appUpdateConfig: AppUpdateConfig?,
    isCheckingForUpdates: Boolean,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onUpdatePrivateSettings: ((UserPrivateInfo) -> UserPrivateInfo) -> Unit,
    onCheckForUpdates: () -> Unit,
    onNavigate: (CampusTab) -> Unit,
    onOpenReportApp: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsRepo = remember(context) { PreferencesRepository(context.applicationContext) }
    val autoCacheEnabled by prefsRepo.autoCacheEnabled.collectAsStateWithLifecycle(initialValue = true)
    val cacheMaxSizeMb by prefsRepo.cacheMaxSizeMb.collectAsStateWithLifecycle(initialValue = 150)
    val cacheMaxAgeHours by prefsRepo.cacheMaxAgeHours.collectAsStateWithLifecycle(initialValue = 48)
    var cacheStats by remember { mutableStateOf(CacheStats()) }
    var isClearingCache by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        cacheStats = CacheManager.measureCache(context)
    }

    var query by remember { mutableStateOf("") }
    var hasSysNotifPermission by remember { mutableStateOf(NotificationHelper.hasNotificationPermission(context)) }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasSysNotifPermission = granted
        Toast.makeText(
            context,
            if (granted) "System notifications enabled for chat & activity" else "Enable notifications in Android Settings",
            Toast.LENGTH_SHORT
        ).show()
    }

    var activeModal by remember { mutableStateOf<String?>(null) } // personal, saved, activity, layout, language, honor, help, status, logout
    val effectiveInfo = privateInfo ?: UserPrivateInfo(userId = currentUser.uid, email = googleEmail)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Campus Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                placeholder = { Text("Search settings & preferences...") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            )

            val match = { text: String -> query.isBlank() || text.contains(query, ignoreCase = true) }

            if (match("accounts center personal details edit profile")) {
                SettingsGroupCard(title = "Accounts Center") {
                    SettingsNavRow(Icons.Outlined.Person, "Personal Details", googleEmail.ifBlank { currentUser.email }) {
                        activeModal = "personal"
                    }
                    HorizontalDivider()
                    SettingsNavRow(Icons.Outlined.BookmarkBorder, "Saved Posts (${effectiveInfo.savedPostIds.size})", "Bookmarked announcements & moments") {
                        activeModal = "saved"
                    }
                    HorizontalDivider()
                    SettingsNavRow(Icons.Outlined.Info, "Your Campus Activity", "Likes, posts, and engagement summary") {
                        activeModal = "activity"
                    }
                }
            }

            if (match("appearance dark mode light system layout feed theme language media")) {
                SettingsGroupCard(title = "Display & App Preferences") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("App Theme", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Currently: ${themeMode.label}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppThemeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = themeMode == mode,
                                    onClick = { onSelectThemeMode(mode) },
                                    label = {
                                        Text(
                                            when (mode) {
                                                AppThemeMode.SYSTEM -> "System"
                                                AppThemeMode.LIGHT -> "Light"
                                                AppThemeMode.DARK -> "Dark"
                                            }
                                        )
                                    },
                                    modifier = Modifier.testTag("settings_theme_${mode.key}")
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                    SettingsNavRow(
                        icon = Icons.Outlined.ViewAgenda,
                        title = "Feed Layout Style",
                        subtitle = FeedLayoutMode.fromKey(effectiveInfo.feedLayoutMode).label
                    ) { activeModal = "layout" }
                    HorizontalDivider()
                    SettingsNavRow(
                        icon = Icons.Outlined.Language,
                        title = "App Language",
                        subtitle = effectiveInfo.language
                    ) { activeModal = "language" }
                    HorizontalDivider()
                    SettingsSwitchRow(
                        icon = Icons.Outlined.ViewAgenda,
                        title = "Data Saver Mode",
                        subtitle = "Minimize bandwidth usage on cellular networks",
                        checked = effectiveInfo.dataSaver,
                        onCheckedChange = { enabled ->
                            onUpdatePrivateSettings { it.copy(dataSaver = enabled) }
                        }
                    )
                }
            }

            if (match("privacy notifications push likes comments announcements chat")) {
                SettingsGroupCard(title = "Privacy & Notifications") {
                    SettingsNavRow(
                        icon = Icons.Outlined.NotificationsActive,
                        title = "Android Status Bar Notifications",
                        subtitle = if (hasSysNotifPermission) "1-min background & closed-app sync active (tap to sync)" else "Tap to allow message & notice alerts"
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasSysNotifPermission) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            NotificationHelper.ensureChannels(context)
                            CampusNotificationWorker.schedulePeriodic(context)
                            CampusNotificationWorker.triggerImmediateCheck(context)
                            Toast.makeText(
                                context,
                                if (hasSysNotifPermission) "Notification sync triggered" else "Enable notifications in device App Settings",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    HorizontalDivider()
                    SettingsSwitchRow(
                        icon = Icons.Outlined.Lock,
                        title = "Private Campus Account",
                        subtitle = "Control visibility across student directories",
                        checked = effectiveInfo.isPrivateAccount,
                        onCheckedChange = { enabled ->
                            onUpdatePrivateSettings { it.copy(isPrivateAccount = enabled) }
                        }
                    )
                    HorizontalDivider()
                    SettingsSwitchRow(
                        icon = Icons.Outlined.Lock,
                        title = "Hide Like Counts",
                        subtitle = "Hide total like numbers on feed posts",
                        checked = effectiveInfo.hideLikeCounts,
                        onCheckedChange = { enabled ->
                            onUpdatePrivateSettings { it.copy(hideLikeCounts = enabled) }
                        }
                    )
                    HorizontalDivider()
                    SettingsSwitchRow(
                        icon = Icons.Outlined.Info,
                        title = "Push: Likes & Reactions",
                        subtitle = "Alerts when someone likes your post",
                        checked = effectiveInfo.pushLikes,
                        onCheckedChange = { enabled ->
                            onUpdatePrivateSettings { it.copy(pushLikes = enabled) }
                        }
                    )
                    HorizontalDivider()
                    SettingsSwitchRow(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        title = "Push: Comments & Replies",
                        subtitle = "Alerts when someone comments on your post",
                        checked = effectiveInfo.pushComments,
                        onCheckedChange = { enabled ->
                            onUpdatePrivateSettings { it.copy(pushComments = enabled) }
                        }
                    )
                    HorizontalDivider()
                    SettingsSwitchRow(
                        icon = Icons.Outlined.Info,
                        title = "Push: Official Announcements",
                        subtitle = "Urgent notices from school administration",
                        checked = effectiveInfo.pushAnnouncements,
                        onCheckedChange = { enabled ->
                            onUpdatePrivateSettings { it.copy(pushAnnouncements = enabled) }
                        }
                    )
                }
            }

            if (match("storage cache clear reels video auto cleanup memory space")) {
                SettingsGroupCard(title = "Storage & Auto Cache") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Media Cache Usage: ${CacheManager.formatBytes(cacheStats.totalBytes)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${cacheStats.videoFilesCount} reels • ${CacheManager.formatBytes(cacheStats.mediaCacheBytes)} video/media • ${CacheManager.formatBytes(cacheStats.coilCacheBytes)} thumbnails",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider()
                    SettingsSwitchRow(
                        icon = Icons.Outlined.CleaningServices,
                        title = "Auto-Clear Reel & Media Cache",
                        subtitle = "Automatically trims old reel videos & temp files on startup",
                        checked = autoCacheEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch { prefsRepo.setAutoCacheEnabled(enabled) }
                        },
                        tag = "settings_auto_cache_switch"
                    )
                    if (autoCacheEnabled) {
                        HorizontalDivider()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Cache Size Cap ($cacheMaxSizeMb MB)",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(100, 150, 250, 500).forEach { limitMb ->
                                    FilterChip(
                                        selected = cacheMaxSizeMb == limitMb,
                                        onClick = { scope.launch { prefsRepo.setCacheMaxSizeMb(limitMb) } },
                                        label = { Text("${limitMb}MB") },
                                        modifier = Modifier.testTag("settings_cache_limit_$limitMb")
                                    )
                                }
                            }
                            Text(
                                text = "Keep Cached Videos For",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(24 to "24h", 48 to "48h", 168 to "7 days").forEach { (hours, label) ->
                                    FilterChip(
                                        selected = cacheMaxAgeHours == hours,
                                        onClick = { scope.launch { prefsRepo.setCacheMaxAgeHours(hours) } },
                                        label = { Text(label) },
                                        modifier = Modifier.testTag("settings_cache_age_$hours")
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isClearingCache) {
                                isClearingCache = true
                                scope.launch {
                                    val freed = CacheManager.clearAllMediaCache(context)
                                    cacheStats = CacheManager.measureCache(context)
                                    prefsRepo.recordAutoCacheClean()
                                    isClearingCache = false
                                    Toast.makeText(
                                        context,
                                        "Cleared ${CacheManager.formatBytes(freed)} of cached reels & photos",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("settings_clear_cache_row"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = null, tint = InstaBlue)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isClearingCache) "Clearing Cache..." else "Clear Cache Now",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = InstaBlue
                            )
                            Text(
                                text = "Free storage by removing downloaded reels, photos & temp APKs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isClearingCache) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (currentUser.isBlackSociety) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Zinc900,
                    border = BorderStroke(1.dp, Zinc700),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("♠ Dark Society Chamber", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                        Text(
                            "Active Confidential Clearance • Designation Member - #${currentUser.blackSocietyNumber ?: 1}",
                            color = Color.White.copy(alpha = 0.78f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (match("honor code help support admin disclaimer unofficial founder email logout")) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "About This Platform (Unofficial)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = InstaBlue
                        )
                        Text(
                            text = CampusConstants.UNOFFICIAL_FULL_DISCLAIMER,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Having a problem on the platform? Email ${CampusConstants.DEVELOPER_SUPPORT_EMAIL}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:${CampusConstants.DEVELOPER_SUPPORT_EMAIL}")
                                    putExtra(Intent.EXTRA_SUBJECT, "Squadify Support Request")
                                }
                                runCatching { context.startActivity(intent) }
                            }
                        )
                    }
                }

                SettingsGroupCard(title = "Platform Guidelines, Updates & Support") {
                    val hasNewUpdate = appUpdateConfig != null && appUpdateConfig.latestVersionCode > BuildConfig.VERSION_CODE
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isCheckingForUpdates) { onCheckForUpdates() }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("settings_check_updates_row"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.SystemUpdate,
                            contentDescription = null,
                            tint = if (hasNewUpdate) Emerald500 else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (hasNewUpdate) {
                                    "Update Available: v${appUpdateConfig!!.latestVersionName}"
                                } else {
                                    "Check for App Updates"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (hasNewUpdate) Emerald500 else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isCheckingForUpdates) {
                                    "Checking Squadify server..."
                                } else {
                                    "Installed: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isCheckingForUpdates) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    HorizontalDivider()
                    if (isAdmin) {
                        SettingsNavRow(Icons.Outlined.Shield, "Admin Moderation Console", "Manage teachers, classes, updates & reports") {
                            onNavigate(CampusTab.ADMIN)
                        }
                        HorizontalDivider()
                    }
                    SettingsNavRow(Icons.Outlined.Shield, "Community Honor Code", "Student-led respect & safety charter") {
                        activeModal = "honor"
                    }
                    HorizontalDivider()
                    SettingsNavRow(Icons.Outlined.HelpOutline, "Developer Support & Contact", CampusConstants.DEVELOPER_SUPPORT_EMAIL) {
                        activeModal = "help"
                    }
                    HorizontalDivider()
                    SettingsNavRow(Icons.Outlined.Flag, "Report a Technical Problem", "In-app report or email ${CampusConstants.DEVELOPER_SUPPORT_EMAIL}") {
                        onOpenReportApp()
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { activeModal = "logout" }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("settings_logout_row"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Logout, contentDescription = null, tint = Rose500)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Log Out of Squadify", color = Rose500, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text(
                text = "${CampusConstants.APP_VERSION} • ${CampusConstants.BUILD_NUMBER}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    when (activeModal) {
        "personal" -> AlertDialog(
            onDismissRequest = { activeModal = null },
            title = { Text("Personal Details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Name: ${currentUser.name}")
                    Text("Email: ${googleEmail.ifBlank { currentUser.email }}")
                    Text("Role: ${currentUser.role}")
                    Text("Class/Dept: ${currentUser.grade ?: "N/A"}")
                    Text("Status: ${if (currentUser.isApproved) "Active & Approved" else "Pending"}")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    activeModal = null
                    onNavigate(CampusTab.EDIT_PROFILE)
                }) { Text("Edit Profile") }
            },
            dismissButton = {
                TextButton(onClick = { activeModal = null }) { Text("Close") }
            }
        )

        "saved" -> {
            val savedPosts = posts.filter { effectiveInfo.savedPostIds.contains(it.id) }
            AlertDialog(
                onDismissRequest = { activeModal = null },
                title = { Text("Saved Posts (${savedPosts.size})") },
                text = {
                    if (savedPosts.isEmpty()) {
                        Text("No bookmarked posts yet. Tap the bookmark icon on any post to save it.")
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 280.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(savedPosts, key = { it.id }) { p ->
                                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                                        Text("${p.authorName} • ${p.category}", style = MaterialTheme.typography.labelLarge)
                                        Text(p.content, maxLines = 2, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { activeModal = null }) { Text("Done") } }
            )
        }

        "activity" -> {
            val myPostsCount = posts.count { it.authorId == currentUser.uid }
            val likesGivenCount = posts.count { it.likedBy.contains(currentUser.uid) }
            AlertDialog(
                onDismissRequest = { activeModal = null },
                title = { Text("Your Campus Activity") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Posts Published: $myPostsCount")
                        Text("Posts Liked: $likesGivenCount")
                        Text("Posts Bookmarked: ${effectiveInfo.savedPostIds.size}")
                        Text("Followers: ${currentUser.followers.size} • Following: ${currentUser.following.size}")
                    }
                },
                confirmButton = { TextButton(onClick = { activeModal = null }) { Text("Close") } }
            )
        }

        "layout" -> AlertDialog(
            onDismissRequest = { activeModal = null },
            title = { Text("Choose Feed Layout") },
            text = {
                Column {
                    FeedLayoutMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdatePrivateSettings { it.copy(feedLayoutMode = mode.key) }
                                    activeModal = null
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = effectiveInfo.feedLayoutMode == mode.key, onClick = {
                                onUpdatePrivateSettings { it.copy(feedLayoutMode = mode.key) }
                                activeModal = null
                            })
                            Column {
                                Text(mode.label, style = MaterialTheme.typography.titleSmall)
                                Text(mode.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { activeModal = null }) { Text("Done") } }
        )

        "language" -> AlertDialog(
            onDismissRequest = { activeModal = null },
            title = { Text("Campus App Language") },
            text = {
                Column {
                    CampusConstants.SUPPORTED_LANGUAGES.forEach { lang ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdatePrivateSettings { it.copy(language = lang) }
                                    activeModal = null
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = effectiveInfo.language == lang, onClick = {
                                onUpdatePrivateSettings { it.copy(language = lang) }
                                activeModal = null
                            })
                            Text(lang)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { activeModal = null }) { Text("Done") } }
        )

        "honor" -> AlertDialog(
            onDismissRequest = { activeModal = null },
            title = { Text("Student Community Honor Code") },
            text = {
                Text("This is an unofficial student-created community platform (not affiliated with BSD Public School or its administration). All members agree to uphold mutual respect, kindness, academic honesty, and responsible digital conduct.")
            },
            confirmButton = { TextButton(onClick = { activeModal = null }) { Text("I Agree") } }
        )

        "help" -> AlertDialog(
            onDismissRequest = { activeModal = null },
            title = { Text("Platform Support & Disclaimer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(CampusConstants.UNOFFICIAL_FULL_DISCLAIMER)
                    Text(
                        text = "For any problems, bugs, or account concerns on this platform, please email:\n${CampusConstants.DEVELOPER_SUPPORT_EMAIL}",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:${CampusConstants.DEVELOPER_SUPPORT_EMAIL}")
                        putExtra(Intent.EXTRA_SUBJECT, "Squadify Support Request")
                    }
                    runCatching { context.startActivity(intent) }
                    activeModal = null
                }) { Text("Email Support") }
            },
            dismissButton = { TextButton(onClick = { activeModal = null }) { Text("Close") } }
        )

        "logout" -> AlertDialog(
            onDismissRequest = { activeModal = null },
            title = { Text("Log Out") },
            text = { Text("Sign out of your Google & Squadify session on this device?") },
            confirmButton = {
                Button(
                    onClick = {
                        activeModal = null
                        AuthHelper.performSignOut(context, scope)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose500, contentColor = Color.White)
                ) { Text("Log Out") }
            },
            dismissButton = { TextButton(onClick = { activeModal = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SettingsGroupCard(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (tag != null) Modifier.testTag(tag) else Modifier
        )
    }
}
