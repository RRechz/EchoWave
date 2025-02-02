package com.samyak.simpletube.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SdCard
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.samyak.simpletube.BuildConfig
import com.samyak.simpletube.LocalPlayerAwareWindowInsets
import com.samyak.simpletube.R
import com.samyak.simpletube.constants.AccountNameKey
import com.samyak.simpletube.constants.InnerTubeCookieKey
import com.samyak.simpletube.ui.component.IconButton
import com.samyak.simpletube.ui.component.PreferenceEntry
import com.samyak.simpletube.ui.utils.backToMain
import com.samyak.simpletube.utils.rememberPreference
import com.zionhuang.innertube.utils.parseCookieString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

// Show version and update info
@Composable
fun VersionCard(uriHandler: UriHandler) {

    Spacer(Modifier.height(25.dp))
    ElevatedCard(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),
        modifier = Modifier
//           .clip(RoundedCornerShape(38.dp))
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(85.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,

            ),
        shape = RoundedCornerShape(38.dp),
        onClick = { uriHandler.openUri("https://github.com/RRechz/EchoWave/releases/latest") }
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(38.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = BuildConfig.VERSION_NAME,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.align(Alignment.CenterHorizontally),


                )
        }
    }
}

@Composable
fun UpdateCard(uriHandler: UriHandler) {
    var showUpdateCard by remember { mutableStateOf(false) }
    var latestVersion by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val newVersion = checkForUpdates()
        if (newVersion != null && isNewerVersion(newVersion, BuildConfig.VERSION_NAME)) {
            showUpdateCard = true
            latestVersion = newVersion
        } else {
            showUpdateCard = false
        }
    }

    if (showUpdateCard) {
        Spacer(Modifier.height(25.dp))
        ElevatedCard(
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            ),
            modifier = Modifier
//                .clip(RoundedCornerShape(28.dp))
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(120.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            shape = RoundedCornerShape(38.dp),
            onClick = {
                uriHandler.openUri("https://github.com/RRechz/EchoWave/releases/latest")
            }
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "${stringResource(R.string.newversion)} $latestVersion",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

suspend fun checkForUpdates(): String? = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://api.github.com/repos/RRechz/EchoWave/releases/latest")
        val connection = url.openConnection()
        connection.connect()
        val json = connection.getInputStream().bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(json)
        return@withContext jsonObject.getString("tag_name")
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext null
    }
}

fun isNewerVersion(remoteVersion: String, currentVersion: String): Boolean {
    val remote = remoteVersion.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
    val current = currentVersion.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }

    for (i in 0 until maxOf(remote.size, current.size)) {
        val r = remote.getOrNull(i) ?: 0
        val c = current.getOrNull(i) ?: 0
        if (r > c) return true
        if (r < c) return false
    }
    return false
}

// Settings content and button
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val uriHandler = LocalUriHandler.current

//    var isBetaFunEnabled by remember { mutableStateOf(false) }


    val backgroundImages = listOf(
        R.drawable.image,
        R.drawable.image2,
        // will be added...
    )
    var currentImageIndex by remember { mutableIntStateOf((0..backgroundImages.lastIndex).random()) }
    fun changeBackgroundImage() {
        currentImageIndex = (currentImageIndex + 1) % backgroundImages.size
    }
    val accountName by rememberPreference(AccountNameKey, "")
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn =
        remember(innerTubeCookie) {
            "SAPISID" in parseCookieString(innerTubeCookie)
        }

    Column(
        Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top)))
        if (isLoggedIn) {
            Box(
                modifier = Modifier
                    .height(220.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(color = Color.Transparent)
                    .clickable { changeBackgroundImage() } // Change background image on click
            ){
                Image(
                    painter = painterResource(id = backgroundImages[currentImageIndex]),
                    contentDescription = "background",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(0.5.dp)

                )
                PreferenceEntry(
                    title = {
                        Column(
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Text(
                                stringResource(R.string.hi),
                                color = Color.White,
                                fontSize = 20.sp,
                                style = MaterialTheme.typography.titleSmall,
                                fontFamily = FontFamily.SansSerif
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                accountName.replace("@", ""),
                                color = Color.White,
                                fontSize = 20.sp,
                                style = MaterialTheme.typography.titleSmall,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    },
                    description = null,
                    onClick = { changeBackgroundImage() },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        PreferenceEntry(
            title = { Text(stringResource(R.string.appearance)) },
            icon = { Icon(Icons.Rounded.Palette, null) },
            onClick = { navController.navigate("settings/appearance") }
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.content)) },
            icon = { Icon(Icons.Rounded.Language, null) },
            onClick = { navController.navigate("settings/content") }
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.player_and_audio)) },
            icon = { Icon(Icons.Rounded.PlayArrow, null) },
            onClick = { navController.navigate("settings/player") }
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.local_player_settings_title)) },
            icon = { Icon(Icons.Rounded.SdCard, null) },
            onClick = { navController.navigate("settings/local") }
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.storage)) },
            icon = { Icon(Icons.Rounded.Storage, null) },
            onClick = { navController.navigate("settings/storage") }
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.privacy)) },
            icon = { Icon(Icons.Rounded.Security, null) },
            onClick = { navController.navigate("settings/privacy") }
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.backup_restore)) },
            icon = { Icon(Icons.Rounded.Restore, null) },
            onClick = { navController.navigate("settings/backup_restore") }
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.experimental_settings_title)) },
            icon = { Icon(Icons.Rounded.WarningAmber, null) },
            onClick = { navController.navigate("settings/experimental") }
        )
        PreferenceEntry(
            title = { Text(stringResource(R.string.about)) },
            icon = { Icon(Icons.Rounded.Info, null) },
            onClick = { navController.navigate("settings/about") }
        )

        Spacer(Modifier.height(25.dp))
        UpdateCard(uriHandler)
        Spacer(Modifier.height(25.dp))
        VersionCard(uriHandler)
    }

    TopAppBar(
        title = { Text(stringResource(R.string.settings)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = null
                )
            }
        },
        scrollBehavior = scrollBehavior
    )
}