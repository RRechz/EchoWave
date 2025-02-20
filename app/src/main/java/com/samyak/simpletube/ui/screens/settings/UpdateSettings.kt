package com.samyak.simpletube.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.samyak.simpletube.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateSettings(navController: NavController) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    var showUpdateNotification by remember { mutableStateOf(sharedPreferences.getBoolean("show_update_notification", true)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.update_settings)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Geri")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(Modifier.padding(paddingValues)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = showUpdateNotification,
                        onValueChange = {
                            showUpdateNotification = it
                            sharedPreferences.edit().putBoolean("show_update_notification", it).apply()
                        }
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = showUpdateNotification,
                    onCheckedChange = {
                        showUpdateNotification = it
                        sharedPreferences.edit().putBoolean("show_update_notification", it).apply()
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.show_update_notification))
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            showUpdateNotification = sharedPreferences.getBoolean("show_update_notification", true)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun UpdateSettingsPreview() {
    UpdateSettings(navController = rememberNavController())
}