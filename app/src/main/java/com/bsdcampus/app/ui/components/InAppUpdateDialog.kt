package com.bsdcampus.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bsdcampus.app.BuildConfig
import com.bsdcampus.app.data.AppUpdateDownloadState
import com.bsdcampus.app.data.CacheManager
import com.bsdcampus.app.model.AppUpdateConfig
import com.bsdcampus.app.ui.theme.Emerald500
import com.bsdcampus.app.ui.theme.InstaBlue
import com.bsdcampus.app.ui.theme.Rose500

@Composable
fun InAppUpdateDialog(
    config: AppUpdateConfig,
    downloadState: AppUpdateDownloadState,
    onStartDownload: () -> Unit,
    onInstallPending: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDownloading = downloadState is AppUpdateDownloadState.Downloading
    val canDismiss = !config.isMandatory && !isDownloading

    AlertDialog(
        onDismissRequest = {
            if (canDismiss) onDismiss()
        },
        icon = {
            Surface(
                shape = CircleShape,
                color = InstaBlue.copy(alpha = 0.14f),
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.SystemUpdate,
                    contentDescription = "App Update",
                    tint = InstaBlue,
                    modifier = Modifier.padding(12.dp)
                )
            }
        },
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Squadify Update Available",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )
                Surface(
                    shape = CircleShape,
                    color = Emerald500.copy(alpha = 0.15f),
                    border = BorderStroke(0.75.dp, Emerald500.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME} → v${config.latestVersionName} (Build ${config.latestVersionCode})",
                        color = Emerald500,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (config.releaseNotes.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "What's New",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = config.releaseNotes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(
                        text = "A newer version of Squadify is ready. Install directly inside the app without downloading manually.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                when (downloadState) {
                    is AppUpdateDownloadState.Downloading -> {
                        val downloadedText = CacheManager.formatBytes(downloadState.downloadedBytes)
                        val totalText = if (downloadState.totalBytes > 0L) {
                            " / ${CacheManager.formatBytes(downloadState.totalBytes)}"
                        } else ""
                        val pctText = if (downloadState.progress >= 0f) {
                            " (${(downloadState.progress * 100).toInt()}%)"
                        } else ""
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (downloadState.progress >= 0f) {
                                LinearProgressIndicator(
                                    progress = { downloadState.progress },
                                    color = InstaBlue,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                LinearProgressIndicator(
                                    color = InstaBlue,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(CircleShape)
                                )
                            }
                            Text(
                                text = "Downloading update: $downloadedText$totalText$pctText",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    is AppUpdateDownloadState.NeedsInstallPermission -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = InstaBlue.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Security, contentDescription = null, tint = InstaBlue, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Update downloaded! Allow Squadify to install app packages once, and installation will begin right away.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    is AppUpdateDownloadState.ReadyToInstall -> {
                        Text(
                            text = "Update APK is ready to install on your device.",
                            style = MaterialTheme.typography.labelMedium,
                            color = Emerald500,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    is AppUpdateDownloadState.Error -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Rose500.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = downloadState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = Rose500,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    AppUpdateDownloadState.Idle -> Unit
                }
            }
        },
        confirmButton = {
            when (downloadState) {
                is AppUpdateDownloadState.Downloading -> {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.testTag("update_downloading_button")
                    ) {
                        Text("Downloading...")
                    }
                }

                is AppUpdateDownloadState.NeedsInstallPermission -> {
                    Button(
                        onClick = onInstallPending,
                        colors = ButtonDefaults.buttonColors(containerColor = InstaBlue, contentColor = Color.White),
                        modifier = Modifier.testTag("update_permission_button")
                    ) {
                        Icon(Icons.Filled.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Allow & Install")
                    }
                }

                is AppUpdateDownloadState.ReadyToInstall -> {
                    Button(
                        onClick = onInstallPending,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Color.White),
                        modifier = Modifier.testTag("update_install_now_button")
                    ) {
                        Icon(Icons.Filled.InstallMobile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Install Now")
                    }
                }

                is AppUpdateDownloadState.Error, AppUpdateDownloadState.Idle -> {
                    Button(
                        onClick = onStartDownload,
                        colors = ButtonDefaults.buttonColors(containerColor = InstaBlue, contentColor = Color.White),
                        modifier = Modifier.testTag("update_download_button")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (downloadState is AppUpdateDownloadState.Error) "Retry Update" else "Update Now")
                    }
                }
            }
        },
        dismissButton = {
            if (canDismiss) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("update_dismiss_button")
                ) {
                    Text("Later")
                }
            }
        }
    )
}
