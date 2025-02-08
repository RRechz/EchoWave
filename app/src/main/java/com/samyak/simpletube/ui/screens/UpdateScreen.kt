package com.samyak.simpletube.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samyak.simpletube.R

@Composable
fun UpdateScreen(
    onDismiss: () -> Unit, // Pop-up penceresini kapatmak için
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.getString(R.string.update_available_title)) },
        text = {
            Column {
                Text(context.getString(R.string.update_available_message))
                Spacer(modifier = Modifier.height(16.dp))
                Text(context.getString(R.string.changelog_text))
                Spacer(modifier = Modifier.height(4.dp))
                Text(context.getString(R.string.update_1))
                Text(context.getString(R.string.update_2))
                Text(context.getString(R.string.update_available_release_notes))
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

@Preview(showBackground = true)
@Composable
fun UpdateScreenPreview() {
    UpdateScreen(onDismiss = { }) // onDismiss fonksiyonu için boş bir lambda ifadesi
}