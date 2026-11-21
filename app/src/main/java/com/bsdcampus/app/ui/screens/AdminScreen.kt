package com.bsdcampus.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bsdcampus.app.BuildConfig
import com.bsdcampus.app.model.AppUpdateConfig
import com.bsdcampus.app.model.CampusAnalytics
import com.bsdcampus.app.model.CampusConstants
import com.bsdcampus.app.model.CampusPost
import com.bsdcampus.app.model.CampusReport
import com.bsdcampus.app.model.CampusUser
import com.bsdcampus.app.ui.components.AvatarSize
import com.bsdcampus.app.ui.components.UserAvatar
import com.bsdcampus.app.ui.components.VerifiedBadgeIcon
import com.bsdcampus.app.ui.theme.Amber500
import com.bsdcampus.app.ui.theme.Emerald500
import com.bsdcampus.app.ui.theme.Gold400
import com.bsdcampus.app.ui.theme.Gold500
import com.bsdcampus.app.ui.theme.InstaBlue
import com.bsdcampus.app.ui.theme.InstaLikeRed
import com.bsdcampus.app.ui.theme.Rose500
import com.bsdcampus.app.ui.theme.Zinc900

private enum class AdminSubTab(val label: String) {
    ANALYTICS("Analytics"),
    TEACHERS("Teachers"),
    USERS("Users"),
    POSTS("Posts"),
    REPORTS("Reports"),
    BROADCAST("Broadcast"),
    UPDATE("App Update")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminScreen(
    currentUser: CampusUser,
    isAdmin: Boolean,
    googleEmail: String,
    isEmailVerified: Boolean,
    analytics: CampusAnalytics,
    allUsers: List<CampusUser>,
    posts: List<CampusPost>,
    reports: List<CampusReport>,
    appUpdateConfig: AppUpdateConfig?,
    onClaimSuperAdmin: () -> Unit,
    onApproveTeacher: (String) -> Unit,
    onRejectTeacher: (String) -> Unit,
    onToggleSuspend: (String, Boolean) -> Unit,
    onToggleVerify: (String, Boolean) -> Unit,
    onUpdateRole: (String, String, String) -> Unit,
    onToggleDarkSociety: (String, Boolean) -> Unit,
    onAssignClassRep: (String, Int, Boolean) -> Unit,
    onTogglePin: (CampusPost) -> Unit,
    onDeletePost: (CampusPost) -> Unit,
    onResolveReport: (String, String, String) -> Unit,
    onBroadcast: (String, String, () -> Unit) -> Unit,
    onPublishAppUpdate: (versionCode: Int, versionName: String, apkUrl: String, releaseNotes: String, isMandatory: Boolean, onDone: () -> Unit) -> Unit
) {
    if (!isAdmin) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.Security, contentDescription = null, tint = InstaLikeRed, modifier = Modifier.size(44.dp))
                    Text("Administrator Access Required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    if (isEmailVerified && googleEmail.equals(CampusConstants.SUPER_ADMIN_EMAIL, ignoreCase = true)) {
                        Button(
                            onClick = onClaimSuperAdmin,
                            colors = ButtonDefaults.buttonColors(containerColor = InstaBlue, contentColor = Color.White)
                        ) {
                            Text("Verify & Claim Super Admin")
                        }
                    }
                }
            }
        }
        return
    }

    var activeTab by remember { mutableStateOf(AdminSubTab.ANALYTICS) }
    var userSearch by remember { mutableStateOf("") }
    var roleFilter by remember { mutableStateOf("All") }
    var reportFilter by remember { mutableStateOf("all") }
    var assigningClassGrade by remember { mutableIntStateOf(-1) }
    var broadcastCategory by remember { mutableStateOf("Announcements") }
    var broadcastText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(modifier = Modifier.widthIn(max = 820.dp).fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AdminSubTab.entries.forEach { tab ->
                    FilterChip(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        label = { Text(tab.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = InstaBlue.copy(alpha = 0.16f),
                            selectedLabelColor = InstaBlue
                        ),
                        modifier = Modifier.testTag("admin_tab_${tab.name.lowercase()}")
                    )
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (activeTab) {
                    AdminSubTab.ANALYTICS -> {
                        item {
                            FlowRow(
                                maxItemsInEachRow = 2,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                AnalyticsKpiCard("Total Users", "${analytics.totalUsers}", Modifier.weight(1f))
                                AnalyticsKpiCard("Students", "${analytics.studentsCount}", Modifier.weight(1f))
                                AnalyticsKpiCard("Approved Faculty", "${analytics.teachersCount}", Modifier.weight(1f))
                                AnalyticsKpiCard("Pending Teachers", "${analytics.pendingTeachersCount}", Modifier.weight(1f))
                                AnalyticsKpiCard("Active Posts", "${analytics.activePostsCount}", Modifier.weight(1f))
                                AnalyticsKpiCard("Pending Reports", "${analytics.pendingReportsCount}", Modifier.weight(1f))
                            }
                        }
                        item {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Category Distribution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                                    val maxCount = (analytics.categoryDistribution.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
                                    analytics.categoryDistribution.forEach { (cat, count) ->
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(cat, style = MaterialTheme.typography.labelLarge)
                                                Text("$count", style = MaterialTheme.typography.labelLarge, color = InstaBlue)
                                            }
                                            LinearProgressIndicator(
                                                progress = { count.toFloat() / maxCount.toFloat() },
                                                color = InstaBlue,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(7.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AdminSubTab.TEACHERS -> {
                        val pending = allUsers.filter { it.role == "teacher" && !it.isApproved }
                        val approved = allUsers.filter { it.role == "teacher" && it.isApproved }

                        item { Text("Pending Approvals (${pending.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) }
                        if (pending.isEmpty()) {
                            item { Text("No pending faculty applications.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        } else {
                            items(pending, key = { it.uid }) { teacher ->
                                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, Amber500)) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        UserAvatar(src = teacher.avatar, name = teacher.name, size = AvatarSize.MD)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(teacher.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text(teacher.grade ?: "Faculty", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        IconButton(onClick = { onApproveTeacher(teacher.uid) }) {
                                            Icon(Icons.Filled.Check, contentDescription = "Approve teacher", tint = Emerald500)
                                        }
                                        IconButton(onClick = { onRejectTeacher(teacher.uid) }) {
                                            Icon(Icons.Filled.Close, contentDescription = "Reject teacher", tint = Rose500)
                                        }
                                    }
                                }
                            }
                        }

                        item { Text("Approved Faculty Directory (${approved.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) }
                        items(approved, key = { it.uid }) { teacher ->
                            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    UserAvatar(src = teacher.avatar, name = teacher.name, role = teacher.role, isVerified = teacher.isVerified, size = AvatarSize.MD)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(teacher.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            if (teacher.isVerified) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                VerifiedBadgeIcon(size = 14.dp)
                                            }
                                        }
                                        Text(teacher.grade ?: "Faculty", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    AdminSubTab.USERS -> {
                        item {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, Gold400.copy(alpha = 0.7f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("👑 Class 1–12 Representatives", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                                    Text(
                                        text = "Tap a class pill below or use the Class Rep button on any user card.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        (1..12).forEach { gradeNum ->
                                            val holder = allUsers.firstOrNull { it.isClassRepresentative && it.classRepresentativeGrade == gradeNum }
                                            FilterChip(
                                                selected = holder != null,
                                                onClick = { assigningClassGrade = gradeNum },
                                                label = {
                                                    Text(if (holder != null) "Class $gradeNum: ${holder.name.substringBefore(" ")}" else "Class $gradeNum")
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = Gold400.copy(alpha = 0.2f),
                                                    selectedLabelColor = Gold500
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = userSearch,
                                onValueChange = { userSearch = it },
                                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                                placeholder = { Text("Search users by name, email, or class...") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("All", "student", "teacher", "admin").forEach { rf ->
                                    FilterChip(
                                        selected = roleFilter == rf,
                                        onClick = { roleFilter = rf },
                                        label = { Text(rf.replaceFirstChar { it.uppercase() }) }
                                    )
                                }
                            }
                        }

                        val filteredUsers = allUsers.filter {
                            (roleFilter == "All" || it.role == roleFilter) &&
                                (userSearch.isBlank() ||
                                    it.name.contains(userSearch, ignoreCase = true) ||
                                    it.email.contains(userSearch, ignoreCase = true) ||
                                    (it.grade?.contains(userSearch, ignoreCase = true) == true))
                        }

                        items(filteredUsers, key = { it.uid }) { user ->
                            var roleMenuExpanded by remember { mutableStateOf(false) }
                            var classRepMenuExpanded by remember { mutableStateOf(false) }
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    0.75.dp,
                                    if (user.isSuspended) InstaLikeRed.copy(alpha = 0.65f) else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        UserAvatar(
                                            src = user.avatar,
                                            name = user.name,
                                            role = user.role,
                                            isVerified = user.isVerified,
                                            isBlackSociety = user.isBlackSociety,
                                            blackSocietyNumber = user.blackSocietyNumber,
                                            isClassRepresentative = user.isClassRepresentative,
                                            size = AvatarSize.MD
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(user.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                                                if (user.isVerified) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    VerifiedBadgeIcon(size = 15.dp)
                                                }
                                            }
                                            Text(
                                                "${user.role} • ${user.grade ?: "N/A"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (user.email.isNotBlank()) {
                                                Text(user.email, style = MaterialTheme.typography.labelSmall, color = InstaBlue)
                                            }
                                        }
                                    }

                                    // Live Moderation Status Chips
                                    if (user.isVerified || user.isClassRepresentative || user.isSuspended || user.isBlackSociety) {
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (user.isVerified) {
                                                StatusPill("✓ Blue Tick", InstaBlue)
                                            }
                                            if (user.isClassRepresentative && user.classRepresentativeGrade != null) {
                                                StatusPill("👑 Class ${user.classRepresentativeGrade} Rep", Gold500)
                                            }
                                            if (user.isSuspended) {
                                                StatusPill("⛔ SUSPENDED", InstaLikeRed)
                                            }
                                            if (user.isBlackSociety) {
                                                StatusPill("♠ Member #${user.blackSocietyNumber ?: 1}", MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    }

                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box {
                                            OutlinedButton(onClick = { roleMenuExpanded = true }) {
                                                Text("Role: ${user.role}")
                                            }
                                            DropdownMenu(expanded = roleMenuExpanded, onDismissRequest = { roleMenuExpanded = false }) {
                                                listOf("student", "teacher", "admin").forEach { r ->
                                                    DropdownMenuItem(text = { Text(r) }, onClick = {
                                                        roleMenuExpanded = false
                                                        onUpdateRole(user.uid, user.email, r)
                                                    })
                                                }
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = { onToggleVerify(user.uid, !user.isVerified) },
                                            colors = if (user.isVerified) ButtonDefaults.outlinedButtonColors(
                                                containerColor = InstaBlue.copy(alpha = 0.14f),
                                                contentColor = InstaBlue
                                            ) else ButtonDefaults.outlinedButtonColors(),
                                            modifier = Modifier.testTag("admin_verify_${user.uid}")
                                        ) {
                                            Icon(
                                                Icons.Filled.Verified,
                                                contentDescription = null,
                                                tint = if (user.isVerified) InstaBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (user.isVerified) "Blue Tick ✓" else "Blue Tick")
                                        }

                                        Box {
                                            OutlinedButton(
                                                onClick = { classRepMenuExpanded = true },
                                                colors = if (user.isClassRepresentative) ButtonDefaults.outlinedButtonColors(
                                                    containerColor = Gold400.copy(alpha = 0.18f),
                                                    contentColor = Gold500
                                                ) else ButtonDefaults.outlinedButtonColors(),
                                                modifier = Modifier.testTag("admin_class_rep_${user.uid}")
                                            ) {
                                                Icon(Icons.Filled.School, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    if (user.isClassRepresentative && user.classRepresentativeGrade != null)
                                                        "Class ${user.classRepresentativeGrade} Rep"
                                                    else "Class Rep"
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = classRepMenuExpanded,
                                                onDismissRequest = { classRepMenuExpanded = false }
                                            ) {
                                                if (user.isClassRepresentative && user.classRepresentativeGrade != null) {
                                                    DropdownMenuItem(
                                                        text = { Text("Revoke Class ${user.classRepresentativeGrade} Rep", color = InstaLikeRed) },
                                                        onClick = {
                                                            classRepMenuExpanded = false
                                                            onAssignClassRep(user.uid, user.classRepresentativeGrade, false)
                                                        }
                                                    )
                                                }
                                                (1..12).forEach { gradeNum ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            val currentHolder = allUsers.firstOrNull {
                                                                it.isClassRepresentative && it.classRepresentativeGrade == gradeNum
                                                            }
                                                            Text(
                                                                if (currentHolder != null && currentHolder.uid != user.uid)
                                                                    "Assign Class $gradeNum (replaces ${currentHolder.name.substringBefore(" ")})"
                                                                else "Assign Class $gradeNum Rep"
                                                            )
                                                        },
                                                        onClick = {
                                                            classRepMenuExpanded = false
                                                            onAssignClassRep(user.uid, gradeNum, true)
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        OutlinedButton(
                                            onClick = { onToggleDarkSociety(user.uid, !user.isBlackSociety) },
                                            colors = if (user.isBlackSociety) ButtonDefaults.outlinedButtonColors(
                                                containerColor = Zinc900,
                                                contentColor = Color.White
                                            ) else ButtonDefaults.outlinedButtonColors()
                                        ) {
                                            Text(if (user.isBlackSociety) "♠ Member #${user.blackSocietyNumber ?: 1}" else "♠ Grant Society")
                                        }

                                        OutlinedButton(
                                            onClick = { onToggleSuspend(user.uid, !user.isSuspended) },
                                            colors = if (user.isSuspended) ButtonDefaults.outlinedButtonColors(
                                                containerColor = InstaLikeRed.copy(alpha = 0.16f),
                                                contentColor = InstaLikeRed
                                            ) else ButtonDefaults.outlinedButtonColors(contentColor = InstaLikeRed),
                                            modifier = Modifier.testTag("admin_suspend_${user.uid}")
                                        ) {
                                            Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (user.isSuspended) "Unsuspend" else "Suspend")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AdminSubTab.POSTS -> {
                        items(posts, key = { it.id }) { post ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${post.authorName} • ${post.category} (${post.images.size} media)", style = MaterialTheme.typography.labelLarge)
                                        Text(post.content, maxLines = 2, style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(onClick = { onTogglePin(post) }) {
                                        Icon(
                                            Icons.Filled.PushPin,
                                            contentDescription = "Toggle Pin",
                                            tint = if (post.isPinned) InstaBlue else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { onDeletePost(post) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete Post", tint = InstaLikeRed)
                                    }
                                }
                            }
                        }
                    }

                    AdminSubTab.REPORTS -> {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("all", "pending", "resolved", "dismissed").forEach { st ->
                                    FilterChip(
                                        selected = reportFilter == st,
                                        onClick = { reportFilter = st },
                                        label = { Text(st.replaceFirstChar { it.uppercase() }) }
                                    )
                                }
                            }
                        }
                        val filteredReports = reports.filter { reportFilter == "all" || it.status == reportFilter }
                        items(filteredReports, key = { it.id }) { report ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${report.targetType.uppercase()} • ${report.status.uppercase()}", style = MaterialTheme.typography.labelLarge, color = InstaBlue)
                                        Text(report.reason, style = MaterialTheme.typography.labelSmall, color = InstaLikeRed)
                                    }
                                    Text(report.targetSummary, style = MaterialTheme.typography.bodySmall)
                                    if (report.status == "pending") {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = { onResolveReport(report.id, "resolved", "Action taken by admin") },
                                                colors = ButtonDefaults.buttonColors(containerColor = InstaBlue, contentColor = Color.White)
                                            ) {
                                                Text("Resolve")
                                            }
                                            OutlinedButton(onClick = { onResolveReport(report.id, "dismissed", "Reviewed & dismissed") }) {
                                                Text("Dismiss")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    AdminSubTab.BROADCAST -> {
                        item {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Campaign, contentDescription = null, tint = InstaBlue)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Official School Broadcast", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                                    }
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CampusConstants.POST_CATEGORIES.forEach { cat ->
                                            FilterChip(selected = broadcastCategory == cat, onClick = { broadcastCategory = cat }, label = { Text(cat) })
                                        }
                                    }
                                    OutlinedTextField(
                                        value = broadcastText,
                                        onValueChange = { broadcastText = it },
                                        placeholder = { Text("Write an official school-wide pinned notice...") },
                                        minLines = 4,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("admin_broadcast_input")
                                    )
                                    Button(
                                        onClick = {
                                            if (broadcastText.isNotBlank()) {
                                                onBroadcast(broadcastText.trim(), broadcastCategory) { broadcastText = "" }
                                            }
                                        },
                                        enabled = broadcastText.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = InstaBlue, contentColor = Color.White),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("admin_broadcast_button")
                                    ) {
                                        Text("Publish Pinned Broadcast", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    AdminSubTab.UPDATE -> {
                        item {
                            var verCodeText by remember(appUpdateConfig) {
                                mutableStateOf(
                                    (appUpdateConfig?.latestVersionCode ?: (BuildConfig.VERSION_CODE + 1)).toString()
                                )
                            }
                            var verName by remember(appUpdateConfig) {
                                mutableStateOf(appUpdateConfig?.latestVersionName ?: "2.6.0")
                            }
                            var apkUrl by remember(appUpdateConfig) {
                                mutableStateOf(appUpdateConfig?.apkUrl.orEmpty())
                            }
                            var releaseNotes by remember(appUpdateConfig) {
                                mutableStateOf(appUpdateConfig?.releaseNotes.orEmpty())
                            }
                            var isMandatory by remember(appUpdateConfig) {
                                mutableStateOf(appUpdateConfig?.isMandatory ?: false)
                            }
                            var isPublishing by remember { mutableStateOf(false) }

                            val parsedCode = verCodeText.trim().toIntOrNull()
                            val isValidInput = parsedCode != null && parsedCode >= 1 &&
                                verName.trim().isNotEmpty() &&
                                (apkUrl.trim().startsWith("https://") || apkUrl.trim().startsWith("http://"))

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = InstaBlue)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Direct In-App APK Update", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                                            Text(
                                                text = "Running: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Text(
                                        text = "Publish a direct HTTPS APK download link (e.g., GitHub Releases or cloud host). Users with an older version code will get a prompt to download & install inside Squadify.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedTextField(
                                            value = verCodeText,
                                            onValueChange = { if (it.all { ch -> ch.isDigit() } && it.length <= 6) verCodeText = it },
                                            label = { Text("Version Code") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = verName,
                                            onValueChange = { if (it.length <= 40) verName = it },
                                            label = { Text("Version Name") },
                                            placeholder = { Text("2.6.0") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.weight(1.3f)
                                        )
                                    }

                                    OutlinedTextField(
                                        value = apkUrl,
                                        onValueChange = { if (it.length <= 1000) apkUrl = it },
                                        label = { Text("Direct APK Download URL") },
                                        placeholder = { Text("https://example.com/Squadify-v2.6.0.apk") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("admin_update_url_input")
                                    )

                                    OutlinedTextField(
                                        value = releaseNotes,
                                        onValueChange = { if (it.length <= 1000) releaseNotes = it },
                                        label = { Text("Release Notes") },
                                        placeholder = { Text("What's new in this Squadify release?") },
                                        minLines = 3,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isMandatory = !isMandatory },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Mandatory Update", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text(
                                                "Prevent dismissing the update prompt until installed",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = isMandatory,
                                            onCheckedChange = { isMandatory = it },
                                            modifier = Modifier.testTag("admin_update_mandatory_switch")
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            val code = parsedCode ?: return@Button
                                            isPublishing = true
                                            onPublishAppUpdate(
                                                code,
                                                verName.trim(),
                                                apkUrl.trim(),
                                                releaseNotes.trim(),
                                                isMandatory
                                            ) {
                                                isPublishing = false
                                            }
                                        },
                                        enabled = isValidInput && !isPublishing,
                                        colors = ButtonDefaults.buttonColors(containerColor = InstaBlue, contentColor = Color.White),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("admin_publish_update_button")
                                    ) {
                                        Text(
                                            if (isPublishing) "Publishing..." else "Publish In-App Update",
                                            fontWeight = FontWeight.Bold
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

    if (assigningClassGrade in 1..12) {
        val currentHolder = allUsers.firstOrNull { it.isClassRepresentative && it.classRepresentativeGrade == assigningClassGrade }
        val candidates = remember(allUsers, assigningClassGrade) {
            allUsers
                .filter { it.isApproved && !it.isSuspended }
                .sortedWith(
                    compareByDescending<CampusUser> {
                        it.grade?.contains("$assigningClassGrade") == true
                    }.thenByDescending { it.role == "student" }.thenBy { it.name }
                )
        }
        AlertDialog(
            onDismissRequest = { assigningClassGrade = -1 },
            title = { Text("Assign Class $assigningClassGrade Rep") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 340.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (currentHolder != null) {
                        item {
                            TextButton(onClick = {
                                onAssignClassRep(currentHolder.uid, assigningClassGrade, false)
                                assigningClassGrade = -1
                            }) {
                                Text("Revoke current Rep (${currentHolder.name})", color = InstaLikeRed)
                            }
                        }
                    }
                    items(candidates, key = { it.uid }) { candidate ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    onAssignClassRep(candidate.uid, assigningClassGrade, true)
                                    assigningClassGrade = -1
                                }
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(
                                src = candidate.avatar,
                                name = candidate.name,
                                role = candidate.role,
                                isVerified = candidate.isVerified,
                                isClassRepresentative = candidate.isClassRepresentative,
                                size = AvatarSize.SM
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(candidate.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    if (candidate.isVerified) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        VerifiedBadgeIcon(size = 13.dp)
                                    }
                                }
                                Text(
                                    "${candidate.role} • ${candidate.grade ?: "N/A"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { assigningClassGrade = -1 }) { Text("Close") } }
        )
    }
}

@Composable
private fun StatusPill(label: String, tint: Color) {
    Surface(
        shape = CircleShape,
        color = tint.copy(alpha = 0.14f)
    ) {
        Text(
            text = label,
            color = tint,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun AnalyticsKpiCard(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.widthIn(min = 140.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = InstaBlue)
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
