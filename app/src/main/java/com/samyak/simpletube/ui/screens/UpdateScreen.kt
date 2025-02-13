package com.samyak.simpletube.ui.screens

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import timber.log.Timber
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samyak.simpletube.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

@Composable
fun UpdateScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var changelog by remember { mutableStateOf("") }
    var currentVersion by remember { mutableStateOf("") }
    var latestVersion by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        changelog = getChangelogFromGitHub()
        latestVersion = getLatestVersionFromGitHub()
        currentVersion = getCurrentVersion(context)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.update_available_title)) },
        text = {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                Text(changelog)
                if (currentVersion.isNotBlank() && latestVersion.isNotBlank()) {
                    Text(text = "v$currentVersion \u2192 v$latestVersion")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.github.com/RRechz/EchoWave/releases/latest/")
                    )
                    context.startActivity(intent)
                    onDismiss()
                }
            ) {
                Text(context.getString(R.string.update))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(context.getString(R.string.cancel))
            }
        }
    )
}

private suspend fun getChangelogFromGitHub(): String {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/RRechz/EchoWave/releases/latest")
            val connection = url.openConnection()
            connection.connect()
            val json = connection.getInputStream().bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(json)
            val body = jsonObject.optString("body", "")

            val changelogLines = mutableListOf<String>()
            var changelogStarted = false
            body.lines().forEach { line ->
                if (line.startsWith("## Changelog") || line.startsWith("## Changelog | Source")) {
                    changelogStarted = true
                } else if (line.startsWith("##") && changelogStarted) {
                    changelogStarted = false
                    return@forEach
                } else if (changelogStarted) {
                    changelogLines.add(line)
                }
            }
            changelogLines.joinToString("\n").trim()
        } catch (e: Exception) {
            Timber.e(e, "Hata: %s", e.message)
            "Değişiklik günlüğü alınamadı: ${e.message}"
        }
    }
}

private suspend fun getLatestVersionFromGitHub(): String {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/RRechz/EchoWave/releases/latest")
            val connection = url.openConnection()
            connection.connect()
            val json = connection.getInputStream().bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(json)
            jsonObject.optString("tag_name", "N/A")
        } catch (e: Exception) {
            Timber.e(e, "Hata: %s", e.message)
            "N/A"
        }
    }
}

private fun getCurrentVersion(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "N/A"
    } catch (e: PackageManager.NameNotFoundException) {
        Timber.e(e, "Hata: %s", e.message)
        "N/A"
    }
}

@Preview(showBackground = true)
@Composable
fun UpdateScreenPreview() {
    UpdateScreen(onDismiss = { })
}
