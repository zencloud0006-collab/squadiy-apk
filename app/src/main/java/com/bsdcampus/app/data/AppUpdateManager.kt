package com.bsdcampus.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.bsdcampus.app.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed interface AppUpdateDownloadState {
    data object Idle : AppUpdateDownloadState
    data class Downloading(
        val progress: Float,
        val downloadedBytes: Long,
        val totalBytes: Long
    ) : AppUpdateDownloadState

    data class ReadyToInstall(val apkFile: File) : AppUpdateDownloadState
    data class NeedsInstallPermission(val apkFile: File) : AppUpdateDownloadState
    data class Error(val message: String) : AppUpdateDownloadState
}

object AppUpdateManager {
    private const val UPDATES_DIR = "app_updates"
    private const val UPDATE_APK_NAME = "squadify_update.apk"

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
        }
    }

    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val cleanUrl = apkUrl.trim()
        if (!cleanUrl.startsWith("https://", ignoreCase = true) &&
            !cleanUrl.startsWith("http://", ignoreCase = true)
        ) {
            return@withContext Result.failure(IllegalArgumentException("Invalid APK download URL."))
        }

        val updatesDir = File(context.cacheDir, UPDATES_DIR).apply { mkdirs() }
        val tmpFile = File(updatesDir, "$UPDATE_APK_NAME.tmp")
        val targetFile = File(updatesDir, UPDATE_APK_NAME)
        if (tmpFile.exists()) tmpFile.delete()

        var connection: HttpURLConnection? = null
        try {
            var currentUrl = cleanUrl
            var redirects = 0
            while (redirects < 5) {
                val conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    setRequestProperty("User-Agent", "Squadify-Android/${BuildConfig.VERSION_NAME}")
                }
                val code = conn.responseCode
                if (code in 300..399) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (location.isNullOrBlank()) {
                        return@withContext Result.failure(IllegalStateException("Broken download redirect."))
                    }
                    currentUrl = URL(URL(currentUrl), location).toString()
                    redirects++
                } else {
                    connection = conn
                    break
                }
            }

            val conn = connection ?: return@withContext Result.failure(
                IllegalStateException("Too many redirects while downloading update.")
            )
            val status = conn.responseCode
            if (status !in 200..299) {
                return@withContext Result.failure(
                    IllegalStateException("Download failed (HTTP $status). Check the APK link.")
                )
            }

            val totalBytes = conn.contentLengthLong.takeIf { it > 0L } ?: -1L
            var downloaded = 0L
            var verifiedZipHeader = false

            conn.inputStream.use { input ->
                FileOutputStream(tmpFile).use { output ->
                    val buffer = ByteArray(16_384)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        if (!verifiedZipHeader) {
                            if (read < 2 || buffer[0] != 'P'.code.toByte() || buffer[1] != 'K'.code.toByte()) {
                                return@withContext Result.failure(
                                    IllegalStateException("Downloaded file is not a valid Android APK package.")
                                )
                            }
                            verifiedZipHeader = true
                        }
                        output.write(buffer, 0, read)
                        downloaded += read
                        val progress = if (totalBytes > 0L) {
                            (downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        } else {
                            -1f
                        }
                        onProgress(progress, downloaded, totalBytes)
                    }
                    output.flush()
                }
            }

            if (!tmpFile.exists() || tmpFile.length() < 1024L) {
                tmpFile.delete()
                return@withContext Result.failure(IllegalStateException("Downloaded APK is incomplete."))
            }

            if (targetFile.exists()) targetFile.delete()
            if (!tmpFile.renameTo(targetFile)) {
                tmpFile.copyTo(targetFile, overwrite = true)
                tmpFile.delete()
            }
            Result.success(targetFile)
        } catch (e: CancellationException) {
            tmpFile.delete()
            throw e
        } catch (e: Exception) {
            tmpFile.delete()
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    fun launchApkInstaller(context: Context, apkFile: File): Boolean {
        if (!canInstallPackages(context)) {
            openInstallPermissionSettings(context)
            return false
        }
        val apkUri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
        return true
    }
}
