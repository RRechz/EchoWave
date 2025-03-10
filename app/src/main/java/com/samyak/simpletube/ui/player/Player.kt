package com.samyak.simpletube.ui.player

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.BitmapDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.PowerManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.media3.common.C
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_ENDED
import androidx.media3.common.Player.STATE_READY
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.samyak.simpletube.LocalPlayerConnection
import com.samyak.simpletube.R
import com.samyak.simpletube.constants.DarkModeKey
import com.samyak.simpletube.constants.PlayerBackgroundStyleKey
import com.samyak.simpletube.constants.PlayerHorizontalPadding
import com.samyak.simpletube.constants.QueuePeekHeight
import com.samyak.simpletube.constants.ShowLyricsKey
import com.samyak.simpletube.extensions.togglePlayPause
import com.samyak.simpletube.extensions.toggleRepeatMode
import com.samyak.simpletube.models.MediaMetadata
import com.samyak.simpletube.models.isShuffleEnabled
import com.samyak.simpletube.playback.PlayerConnection
import com.samyak.simpletube.ui.component.AsyncLocalImage
import com.samyak.simpletube.ui.component.BottomSheet
import com.samyak.simpletube.ui.component.BottomSheetState
import com.samyak.simpletube.ui.component.LocalMenuState
import com.samyak.simpletube.ui.component.PlayerSliderTrack
import com.samyak.simpletube.ui.component.ResizableIconButton
import com.samyak.simpletube.ui.component.rememberBottomSheetState
import com.samyak.simpletube.ui.menu.PlayerMenu
import com.samyak.simpletube.ui.screens.settings.DarkMode
import com.samyak.simpletube.ui.screens.settings.PlayerBackgroundStyle
import com.samyak.simpletube.ui.theme.extractGradientColors
import com.samyak.simpletube.ui.utils.getLocalThumbnail
import com.samyak.simpletube.utils.makeTimeString
import com.samyak.simpletube.utils.rememberEnumPreference
import com.samyak.simpletube.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetPlayer(
    state: BottomSheetState,
    navController: NavController,
    context: Context, // Bağlamı iletmek için yeni parametre
    modifier: Modifier = Modifier,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val menuState = LocalMenuState.current
    val context = LocalContext.current

    val playbackState by playerConnection.playbackState.collectAsState()
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val repeatMode by playerConnection.repeatMode.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)

    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsState()

    val playerBackground by rememberEnumPreference(key = PlayerBackgroundStyleKey, defaultValue = PlayerBackgroundStyle.DEFAULT)

    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val useDarkTheme = remember(darkTheme, isSystemInDarkTheme) {
        if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
    }

    val onBackgroundColor = when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.secondary
        else ->
            if (useDarkTheme)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onPrimary
    }

    val showLyrics by rememberPreference(ShowLyricsKey, defaultValue = false)

    var position by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.currentPosition)
    }
    var duration by rememberSaveable(playbackState) {
        mutableLongStateOf(playerConnection.player.duration)
    }
    var sliderPosition by remember {
        mutableStateOf<Long?>(null)
    }

    var gradientColors by remember {
        mutableStateOf<List<Color>>(emptyList())
    }

    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager


    // gradient colours
    LaunchedEffect(mediaMetadata) {
        if (playerBackground != PlayerBackgroundStyle.GRADIENT || powerManager.isPowerSaveMode) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            if (mediaMetadata?.isLocal == true) {
                getLocalThumbnail(mediaMetadata?.localPath)?.extractGradientColors()?.let {
                    gradientColors = it
                }
            } else {
                val result = (ImageLoader(context).execute(
                    ImageRequest.Builder(context)
                        .data(mediaMetadata?.thumbnailUrl)
                        .allowHardware(false)
                        .build()
                ).drawable as? BitmapDrawable)?.bitmap?.extractGradientColors()

                result?.let {
                    gradientColors = it
                }
            }
        }
    }

    LaunchedEffect(playbackState) {
        if (playbackState == STATE_READY) {
            while (isActive) {
                delay(500)
                position = playerConnection.player.currentPosition
                duration = playerConnection.player.duration
            }
        }
    }

    val queueSheetState = rememberBottomSheetState(
        dismissedBound = QueuePeekHeight + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
        expandedBound = state.expandedBound,
    )


    BottomSheet(
        state = state,
        modifier = modifier,
        backgroundColor = if (useDarkTheme || playerBackground == PlayerBackgroundStyle.DEFAULT) {
            MaterialTheme.colorScheme.surfaceColorAtElevation(NavigationBarDefaults.Elevation)
        } else MaterialTheme.colorScheme.onSurfaceVariant,
        collapsedBackgroundColor = MaterialTheme.colorScheme.surfaceColorAtElevation(NavigationBarDefaults.Elevation),
        onDismiss = {
            playerConnection.player.stop()
            playerConnection.player.clearMediaItems()
        },
        collapsedContent = {
            MiniPlayer(
                position = position,
                duration = duration
            )
        }
    ) {
        val actionButtons: @Composable RowScope.() -> Unit = {
            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .offset(y = 5.dp)
                    .size(36.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                ResizableIconButton(
                    icon = if (currentSong?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(24.dp),
                    onClick = playerConnection::toggleLike
                )
            }

            Spacer(modifier = Modifier.width(7.dp))

            Box(
                modifier = Modifier
                    .offset(y = 5.dp)
                    .size(36.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                ResizableIconButton(
                    icon = Icons.Rounded.MoreVert,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center),
                    onClick = {
                        menuState.show {
                            PlayerMenu(
                                mediaMetadata = mediaMetadata,
                                navController = navController,
                                playerBottomSheetState = state,
                                onDismiss = menuState::dismiss
                            )
                        }
                    }
                )
            }
        }

        val controlsContent: @Composable ColumnScope.(MediaMetadata) -> Unit = { mediaMetadata ->
            val playPauseRoundness by animateDpAsState(
                targetValue = if (isPlaying) 24.dp else 36.dp,
                animationSpec = tween(durationMillis = 100, easing = LinearEasing),
                label = "playPauseRoundness"
            )

            // action buttons for landscape (above title)
            if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                Row (
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = PlayerHorizontalPadding, end = PlayerHorizontalPadding, bottom = 16.dp)
                ) {
                    actionButtons()
                }
            }

            Row(
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding)
            ) {
                Row {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = mediaMetadata.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = onBackgroundColor,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .basicMarquee(
                                    iterations = 1,
                                    initialDelayMillis = 3000
                                )
                                .clickable(enabled = mediaMetadata.album != null) {
                                    navController.navigate("album/${mediaMetadata.album!!.id}")
                                    state.collapseSoft()
                                }
                        )

                        Row {
                            mediaMetadata.artists.fastForEachIndexed { index, artist ->
                                Text(
                                    text = artist.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = onBackgroundColor,
                                    maxLines = 1,
                                    modifier = Modifier
                                        .basicMarquee(
                                            iterations = 1,
                                            initialDelayMillis = 5000
                                        )
                                        .clickable(enabled = artist.id != null) {
                                        navController.navigate("artist/${artist.id}")
                                        state.collapseSoft()
                                    }
                                )

                                if (index != mediaMetadata.artists.lastIndex) {
                                    Text(
                                        text = ", ",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = onBackgroundColor
                                    )
                                }
                            }
                        }
                    }

                    // action buttons for portrait (inline with title)
                    if (LocalConfiguration.current.orientation != Configuration.ORIENTATION_LANDSCAPE) {
                        actionButtons()
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Slider(
                value = (sliderPosition ?: position).toFloat(),
                valueRange = 0f..(if (duration == C.TIME_UNSET) 0f else duration.toFloat()),
                onValueChange = {
                    sliderPosition = it.toLong()
                },
                onValueChangeFinished = {
                    sliderPosition?.let {
                        playerConnection.player.seekTo(it)
                        position = it
                    }
                    sliderPosition = null
                },
                thumb = { Spacer(modifier = Modifier.size(0.dp)) },
                track = { sliderState ->
                    PlayerSliderTrack(
                        sliderState = sliderState,
                        colors = SliderDefaults.colors()
                    )
                },
                modifier = Modifier.padding(horizontal = PlayerHorizontalPadding)
            )

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding + 4.dp)
            ) {
                Text(
                    text = makeTimeString(sliderPosition ?: position),
                    style = MaterialTheme.typography.labelMedium,
                    color = onBackgroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                Text(
                    text = if (duration != C.TIME_UNSET) makeTimeString(duration) else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = onBackgroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Bağlantı durumunu kontrol edin ve uygun ikonu ve metni görüntüleyin
            if (isConnectedToWifi(context)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_wifi),
                        contentDescription = "Wi-Fi Connect",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(id = R.string.wifi_connected)) // String değerini kullanın
                }
            } else if (isConnectedToMobileData(context)) {
                val signalStrength = getMobileDataSignalStrength(context)
                val iconResId = when (signalStrength) {
                    "E" -> R.drawable.ic_mobile_data_2g
                    "H" -> R.drawable.ic_mobile_data_3g
                    "H+" -> R.drawable.ic_mobile_data_3g_plus
                    "4G/LTE" -> R.drawable.ic_mobile_data_4g
                    "4.5G/LTE+" -> R.drawable.ic_mobile_data_4g_plus
                    else -> R.drawable.ic_mobile_data_4g_plus
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = iconResId),
                        contentDescription = "Mobile Data Connect",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(id = R.string.mobile_data_connected)) // String değerini kullanın
                }
            }
            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = PlayerHorizontalPadding)
            ) {
                val shuffleModeEnabled by isShuffleEnabled.collectAsState()

                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = Icons.Rounded.Shuffle,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(4.dp)
                            .align(Alignment.Center)
                            .alpha(if (shuffleModeEnabled) 1f else 0.5f),
                        color = onBackgroundColor,
                        onClick = { playerConnection.triggerShuffle() }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = Icons.Rounded.SkipPrevious,
                        enabled = canSkipPrevious,
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.Center),
                        color = onBackgroundColor,
                        onClick = playerConnection.player::seekToPrevious
                    )
                }

                Spacer(Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(if (showLyrics) 56.dp else 72.dp)
                        .animateContentSize()
                        .clip(RoundedCornerShape(playPauseRoundness))
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            if (playbackState == STATE_ENDED) {
                                playerConnection.player.seekTo(0, 0)
                                playerConnection.player.playWhenReady = true
                            } else {
                                playerConnection.player.togglePlayPause()
                            }
                        }
                ) {
                    Image(
                        imageVector = if(playbackState == STATE_ENDED) Icons.Rounded.Replay else if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimary),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(36.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = Icons.Rounded.SkipNext,
                        enabled = canSkipNext,
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.Center),
                        color = onBackgroundColor,
                        onClick = playerConnection.player::seekToNext
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    ResizableIconButton(
                        icon = when (repeatMode) {
                            REPEAT_MODE_OFF, REPEAT_MODE_ALL -> Icons.Rounded.Repeat
                            REPEAT_MODE_ONE -> Icons.Rounded.RepeatOne
                            else -> throw IllegalStateException()
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .padding(4.dp)
                            .align(Alignment.Center)
                            .alpha(if (repeatMode == REPEAT_MODE_OFF) 0.5f else 1f),
                        color = onBackgroundColor,
                        onClick = playerConnection.player::toggleRepeatMode
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !powerManager.isPowerSaveMode && state.isExpanded,
            enter = fadeIn(tween(1000)),
            exit = fadeOut()
        ) {
            if (playerBackground == PlayerBackgroundStyle.BLUR) {
                if (mediaMetadata?.isLocal == true) {
                    mediaMetadata?.let {
                        AsyncLocalImage(
                            image = { getLocalThumbnail(it.localPath) },
                            contentDescription = null,
                            contentScale = ContentScale.FillBounds,
                            modifier = Modifier
                                .fillMaxSize()
                                .blur(200.dp)
                        )
                    }
                } else {
                    AsyncImage(
                        model = mediaMetadata?.thumbnailUrl,
                        contentDescription = null,
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(200.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
            } else if (playerBackground == PlayerBackgroundStyle.GRADIENT && gradientColors.size >= 2) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(gradientColors), alpha = 0.8f)
                )
            }

            if (playerBackground != PlayerBackgroundStyle.DEFAULT && showLyrics) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
            }
        }

        when (LocalConfiguration.current.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                Row(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                        .padding(bottom = queueSheetState.collapsedBound)
                        .fillMaxSize()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(if (showLyrics) 0.6f else 1f, false)
                            .animateContentSize()
                    ) {
                        Thumbnail(
                            sliderPositionProvider = { sliderPosition },
                            modifier = Modifier
                                .nestedScroll(state.preUpPostDownNestedScrollConnection),
                            showLyricsOnClick = true
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(if (showLyrics) 0.4f else 1f, false)
                            .animateContentSize()
                            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
                    ) {
                        Spacer(Modifier.weight(1f))

                        mediaMetadata?.let {
                            controlsContent(it)
                        }

                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                        .padding(bottom = queueSheetState.collapsedBound)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.weight(1f)
                    ) {
                        Thumbnail(
                            sliderPositionProvider = { sliderPosition },
                            modifier = Modifier
                                .nestedScroll(state.preUpPostDownNestedScrollConnection),
                            showLyricsOnClick = true
                        )
                    }

                    mediaMetadata?.let {
                        controlsContent(it)
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        Queue(
            state = queueSheetState,
            playerBottomSheetState = state,
            onTerminate = {
                state.dismiss()
                PlayerConnection.queueBoard.detachedHead = false
            },
            onBackgroundColor = onBackgroundColor,
            navController = navController
        )
    }
}

fun getMobileDataSignalStrength(context: Context): String? {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) {
        return when {
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_MMS) -> "2G" // MMS desteği varsa 2G olduğunu varsayıyoruz
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_SUPL) -> "3G" // SUPL desteği varsa 3G olduğunu varsayıyoruz
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_DUN) -> "3G+" // DUN desteği varsa 3G+ olduğunu varsayıyoruz
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && capabilities.linkDownstreamBandwidthKbps >= 10000 -> "4G/LTE" // İnternet hızı 10 Mbps'den yüksekse 4G/LTE olduğunu varsayıyoruz
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && capabilities.linkDownstreamBandwidthKbps >= 50000 -> "4.5G/LTE+" // İnternet hızı 50 Mbps'den yüksekse 4.5G/LTE+ olduğunu varsayıyoruz
            else -> "Unknown" // Diğer durumlarda bilinmiyor olarak işaretleyin
        }
    } else {
        return null
    }
}

fun isConnectedToWifi(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
}

fun isConnectedToMobileData(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
    return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
}