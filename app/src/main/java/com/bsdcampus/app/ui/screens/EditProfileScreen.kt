package com.bsdcampus.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bsdcampus.app.data.CampusMediaRepository
import com.bsdcampus.app.data.MediaUtils
import com.bsdcampus.app.model.CampusConstants
import com.bsdcampus.app.model.CampusUser
import com.bsdcampus.app.ui.auth.AuthHelper
import com.bsdcampus.app.ui.components.AvatarSize
import com.bsdcampus.app.ui.components.UserAvatar
import com.bsdcampus.app.ui.theme.Emerald500
import com.bsdcampus.app.ui.theme.Rose500
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun EditProfileScreen(
    currentUser: CampusUser,
    isAdmin: Boolean,
    googleEmail: String,
    onSaveAvatar: (avatar: String, onDone: (Result<Unit>) -> Unit) -> Unit,
    onSaveProfile: (name: String, grade: String?, avatar: String?, bio: String) -> Unit,
    onShowToast: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember(currentUser.uid) { mutableStateOf(currentUser.name) }
    var grade by remember(currentUser.uid) { mutableStateOf(currentUser.grade.orEmpty()) }
    var bio by remember(currentUser.uid) { mutableStateOf(currentUser.bio.orEmpty()) }
    var avatarUrl by remember(currentUser.uid) { mutableStateOf(currentUser.avatar) }

    var isUploadingAvatar by remember { mutableStateOf(false) }
    var avatarStatusMessage by remember { mutableStateOf<String?>(null) }
    var avatarErrorMessage by remember { mutableStateOf<String?>(null) }
    var showGradeMenu by remember { mutableStateOf(false) }
    var showPasteUrlDialog by remember { mutableStateOf(false) }
    var urlDraft by remember { mutableStateOf("") }

    val isGradeLockedForUser = currentUser.gradeLocked && !isAdmin
    val gradeOptions = if (currentUser.role == "teacher") CampusConstants.FACULTY_DEPARTMENTS else CampusConstants.SCHOOL_CLASSES

    val pickAvatar = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isUploadingAvatar = true
            avatarErrorMessage = null
            avatarStatusMessage = "Optimizing & uploading profile photo..."
            scope.launch {
                val compressed = MediaUtils.compressUriToDataUrl(context, uri, maxDimension = 512)
                val resolvedAvatar = compressed ?: CampusMediaRepository.uploadPickedMediaUri(
                    context = context,
                    uri = uri,
                    isAdmin = isAdmin
                ).getOrNull()

                if (resolvedAvatar.isNullOrBlank()) {
                    isUploadingAvatar = false
                    avatarStatusMessage = null
                    val err = "Could not process selected image. Try another photo."
                    avatarErrorMessage = err
                    onShowToast(err)
                    return@launch
                }

                avatarUrl = resolvedAvatar
                onSaveAvatar(resolvedAvatar) { res ->
                    isUploadingAvatar = false
                    if (res.isSuccess) {
                        avatarStatusMessage = "Profile photo updated ✓"
                        avatarErrorMessage = null
                    } else {
                        avatarStatusMessage = null
                        avatarErrorMessage = res.exceptionOrNull()?.localizedMessage
                            ?: "Could not save profile photo to Firestore."
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 660.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("Edit Campus Profile", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        UserAvatar(
                            src = avatarUrl,
                            name = name.ifBlank { currentUser.name },
                            role = currentUser.role,
                            isVerified = currentUser.isVerified,
                            isBlackSociety = currentUser.isBlackSociety,
                            blackSocietyNumber = currentUser.blackSocietyNumber,
                            isClassRepresentative = currentUser.isClassRepresentative,
                            size = AvatarSize.XL
                        )
                        if (isUploadingAvatar) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.55f),
                                modifier = Modifier.size(84.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    avatarStatusMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isUploadingAvatar) MaterialTheme.colorScheme.primary else Emerald500,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    avatarErrorMessage?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.labelMedium,
                            color = Rose500,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                pickAvatar.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            enabled = !isUploadingAvatar,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("edit_profile_upload_button")
                        ) {
                            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isUploadingAvatar) "Saving..." else "Upload", style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = {
                                val seed = "bsd_${UUID.randomUUID().toString().take(8)}"
                                avatarUrl = "https://api.dicebear.com/7.x/avataaars/svg?seed=$seed"
                                avatarStatusMessage = "Preview updated — tap Save to apply"
                                avatarErrorMessage = null
                            },
                            enabled = !isUploadingAvatar,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Random", style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = {
                                urlDraft = ""
                                showPasteUrlDialog = true
                            },
                            enabled = !isUploadingAvatar,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("URL", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 80) name = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_profile_name")
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = grade,
                            onValueChange = {},
                            readOnly = true,
                            enabled = !isGradeLockedForUser,
                            label = {
                                Text(
                                    if (isGradeLockedForUser) "Class / Department (Locked)"
                                    else if (currentUser.role == "teacher") "Faculty Department"
                                    else "Class / Grade"
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (isGradeLockedForUser) Icons.Filled.Lock else Icons.Filled.ArrowDropDown,
                                    contentDescription = null
                                )
                            },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (!isGradeLockedForUser && currentUser.role != "admin") {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showGradeMenu = true }
                            )
                            DropdownMenu(expanded = showGradeMenu, onDismissRequest = { showGradeMenu = false }) {
                                gradeOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            grade = option
                                            showGradeMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = bio,
                        onValueChange = { if (it.length <= 150) bio = it },
                        label = { Text("Bio (${bio.length}/150)") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = googleEmail.ifBlank { currentUser.email },
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Campus Email (Read-only)") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = currentUser.role.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Campus Role (Read-only)") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSaveProfile(name.trim(), grade.takeIf { it.isNotBlank() }, avatarUrl, bio.trim())
                            }
                        },
                        enabled = name.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_profile_button")
                    ) {
                        Text("Save Campus Profile", fontWeight = FontWeight.Black)
                    }

                    OutlinedButton(
                        onClick = { AuthHelper.performSignOut(context, scope) },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose500),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Logout, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Log Out")
                    }
                }
            }
        }
    }

    if (showPasteUrlDialog) {
        AlertDialog(
            onDismissRequest = { showPasteUrlDialog = false },
            title = { Text("Paste Avatar Image URL") },
            text = {
                OutlinedTextField(
                    value = urlDraft,
                    onValueChange = { urlDraft = it },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (urlDraft.startsWith("http://") || urlDraft.startsWith("https://")) {
                            avatarUrl = urlDraft.trim()
                        }
                        showPasteUrlDialog = false
                    }
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showPasteUrlDialog = false }) { Text("Cancel") }
            }
        )
    }
}
