package com.samyak.simpletube.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log // Log sınıfını import et
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    // GitHub API'sinden değişiklik günlüğünü çek
    LaunchedEffect(Unit) {
        changelog = getChangelogFromGitHub()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.update_available_title)) },
        text = {
            Column {
                Text(context.getString(R.string.update_available_message))
                Spacer(modifier = Modifier.height(16.dp))

                // Değişiklik günlüğünü göster
                Text(changelog)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Güncelle butonuna tıklandığında güncelleme linkini aç
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.github.com/RRechz/EchoWave/releases/latest/"))
                    context.startActivity(intent)
                    onDismiss() // Pop-up'ı kapat
                }
            ) {
                Text(context.getString(R.string.update))
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss // İptal butonuna tıklandığında pop-up'ı kapat
            ) {
                Text(context.getString(R.string.cancel))
            }
        }
    )
}

// GitHub API'sinden değişiklik günlüğünü çeken fonksiyon
private suspend fun getChangelogFromGitHub(): String {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/RRechz/EchoWave/releases/latest") // En son sürümün URL'si
            val connection = url.openConnection()
            connection.connect()
            val json = connection.getInputStream().bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(json)
            val body = jsonObject.getString("body") // Açıklama metnini al

            // Açıklama metnini satır satır oku
            val changelogLines = mutableListOf<String>()
            var changelogStarted = false
            body.lines().forEach { line ->
                if (line.startsWith("## Changelog") || line.startsWith("## Changelog | Source")) { // Changelog başlangıcı
                    changelogStarted = true
                } else if (line.startsWith("##") && changelogStarted) { // Başka bir bölümün başlangıcı
                    changelogStarted = false
                    return@forEach // Döngüyü sonlandır
                } else if (changelogStarted) {
                    changelogLines.add(line) // Changelog satırını ekle
                }
            }

            changelogLines.joinToString("\n").trim() // Changelog satırlarını birleştir ve döndür
        } catch (e: Exception) {
            Log.e("UpdateScreen", "Hata: ${e.message}") // Hatayı logla
            "Değişiklik günlüğü alınamadı: ${e.message}" // Hatayı ekrana yazdır
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UpdateScreenPreview() {
    UpdateScreen(onDismiss = { }) // onDismiss fonksiyonu için boş bir lambda ifadesi
}