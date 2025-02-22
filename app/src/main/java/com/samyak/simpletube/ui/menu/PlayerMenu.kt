package com.samyak.simpletube.ui.menu

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.LibraryAddCheck
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastSumBy
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.offline.DownloadService
import androidx.navigation.NavController
import com.samyak.simpletube.LocalDatabase
import com.samyak.simpletube.LocalDownloadUtil
import com.samyak.simpletube.LocalPlayerConnection
import com.samyak.simpletube.R
import com.samyak.simpletube.constants.ListItemHeight
import com.samyak.simpletube.models.MediaMetadata
import com.samyak.simpletube.playback.ExoDownloadService
import com.samyak.simpletube.playback.PlayerConnection.Companion.queueBoard
import com.samyak.simpletube.ui.component.BigSeekBar
import com.samyak.simpletube.ui.component.BottomSheetState
import com.samyak.simpletube.ui.component.DetailsDialog
import com.samyak.simpletube.ui.component.DownloadGridMenu
import com.samyak.simpletube.ui.component.GridMenu
import com.samyak.simpletube.ui.component.GridMenuItem
import com.samyak.simpletube.ui.component.ListDialog
import com.samyak.simpletube.ui.component.SleepTimerGridMenu
import com.zionhuang.innertube.YouTube
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt
import android.graphics.Bitmap
import android.graphics.Color
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material3.Button
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.samyak.simpletube.playback.PlayerConnection

@Composable
fun PlayerMenu(
    mediaMetadata: MediaMetadata?,
    navController: NavController,
    playerBottomSheetState: BottomSheetState,
    onDismiss: () -> Unit,
) {
    mediaMetadata ?: return
    val context = LocalContext.current
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val clipboardManager = LocalClipboardManager.current

    val playerConnection = LocalPlayerConnection.current ?: return
    val playerVolume = playerConnection.service.playerVolume.collectAsState()
    val currentFormat by playerConnection.currentFormat.collectAsState(initial = null)
    val librarySong by database.song(mediaMetadata.id).collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()

    val download by LocalDownloadUtil.current.getDownload(mediaMetadata.id).collectAsState(initial = null)

    var showChooseQueueDialog by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(librarySong?.song?.liked) {
        librarySong?.let {
            downloadUtil.autoDownloadIfLiked(it.song)
        }
    }

    AddToQueueDialog(
        isVisible = showChooseQueueDialog,
        onAdd = { queueName ->
            queueBoard.addQueue(queueName, listOf(mediaMetadata), playerConnection, forceInsert = true, delta = false)
            queueBoard.setCurrQueue(playerConnection)
        },
        onDismiss = {
            showChooseQueueDialog = false
            onDismiss() // here we dismiss since we switch to the queue anyways
        }
    )

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { playlist ->
            database.transaction {
                insert(mediaMetadata)
            }

            coroutineScope.launch(Dispatchers.IO) {
                playlist.playlist.browseId?.let { YouTube.addToPlaylist(it, mediaMetadata.id) }
            }

            listOf(mediaMetadata.id)
        },
        onDismiss = {
            showChoosePlaylistDialog = false
        }
    )

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false }
        ) {
            items(mediaMetadata.artists) { artist ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .height(ListItemHeight)
                        .clickable {
                            navController.navigate("artist/${artist.id}")
                            showSelectArtistDialog = false
                            playerBottomSheetState.collapseSoft()
                            onDismiss()
                        }
                        .padding(horizontal = 24.dp),
                ) {
                    Text(
                        text = artist.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    var showPitchTempoDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showPitchTempoDialog) {
        PitchTempoDialog(
            onDismiss = { showPitchTempoDialog = false }
        )
    }

    val sleepTimerEnabled = remember(playerConnection.service.sleepTimer.triggerTime, playerConnection.service.sleepTimer.pauseWhenSongEnd) {
        playerConnection.service.sleepTimer.isActive
    }

    var sleepTimerTimeLeft by remember {
        mutableLongStateOf(0L)
    }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft = if (playerConnection.service.sleepTimer.pauseWhenSongEnd) {
                    playerConnection.player.duration - playerConnection.player.currentPosition
                } else {
                    playerConnection.service.sleepTimer.triggerTime - System.currentTimeMillis()
                }
                delay(1000L)
            }
        }
    }

    var showSleepTimerDialog by remember {
        mutableStateOf(false)
    }

    var sleepTimerValue by remember {
        mutableStateOf(30f)
    }

    if (showSleepTimerDialog) {
        AlertDialog(
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { showSleepTimerDialog = false },
            icon = { Icon(imageVector = Icons.Rounded.Timer, contentDescription = null) },
            title = { Text(stringResource(R.string.sleep_timer)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSleepTimerDialog = false
                        playerConnection.service.sleepTimer.start(sleepTimerValue.roundToInt())
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showSleepTimerDialog = false }
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.minute,
                            sleepTimerValue.roundToInt(),
                            sleepTimerValue.roundToInt()
                        ),
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Slider(
                        value = sleepTimerValue,
                        onValueChange = { sleepTimerValue = it },
                        valueRange = 5f..120f,
                        steps = (120 - 5) / 5 - 1,
                    )

                    OutlinedButton(
                        onClick = {
                            showSleepTimerDialog = false
                            playerConnection.service.sleepTimer.start(-1)
                        }
                    ) {
                        Text(stringResource(R.string.end_of_song))
                    }
                }
            }
        )
    }

    var showDetailsDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showDetailsDialog) {
        DetailsDialog(
            mediaMetadata = mediaMetadata,
            currentFormat = currentFormat,
            currentPlayCount = librarySong?.playCount?.fastSumBy { it.count }?: 0,
            volume = playerConnection.player.volume,
            clipboardManager = clipboardManager,
            setVisibility = {showDetailsDialog = it }
        )
    }

    var showQrDialog by remember { mutableStateOf(false) }
    var qrDialogState by remember { mutableStateOf<QrDialogState>(QrDialogState.Selection) }
    var scannedSongId by remember { mutableStateOf<String?>(null) }

    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 24.dp, bottom = 6.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )

        BigSeekBar(
            progressProvider = playerVolume::value,
            onProgressChange = { playerConnection.service.playerVolume.value = it },
            modifier = Modifier.weight(1f)
        )
    }

    GridMenu(
        contentPadding = PaddingValues(
            start = 8.dp,
            top = 8.dp,
            end = 8.dp,
            bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
        )
    ) {
        if (mediaMetadata.isLocal != true)
            GridMenuItem(
                icon = Icons.Rounded.Radio,
                title = R.string.start_radio
            ) {
                playerConnection.service.startRadioSeamlessly()
                onDismiss()
            }
        GridMenuItem(
            icon = Icons.AutoMirrored.Rounded.QueueMusic,
            title = R.string.add_to_queue
        ) {
            showChooseQueueDialog = true
        }
        GridMenuItem(
            icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
            title = R.string.add_to_playlist
        ) {
            showChoosePlaylistDialog = true
        }
        if (mediaMetadata.isLocal != true)
            DownloadGridMenu(
                state = download?.state,
                onDownload = {
                    database.transaction {
                        insert(mediaMetadata)
                    }
                    downloadUtil.download(mediaMetadata)
                },
                onRemoveDownload = {
                    DownloadService.sendRemoveDownload(
                        context,
                        ExoDownloadService::class.java,
                        mediaMetadata.id,
                        false
                    )
                }
            )
        if (librarySong?.song?.inLibrary != null && !librarySong!!.song.isLocal) {
            GridMenuItem(
                icon = Icons.Rounded.LibraryAddCheck,
                title = R.string.remove_from_library,
            ) {
                database.query {
                    toggleInLibrary(mediaMetadata.id, null)
                }
            }
        } else {
            GridMenuItem(
                icon = Icons.Rounded.LibraryAdd,
                title = R.string.add_to_library,
            ) {
                database.transaction {
                    insert(mediaMetadata)
                    toggleInLibrary(mediaMetadata.id, LocalDateTime.now())
                }
            }
        }
        GridMenuItem(
            icon = R.drawable.artist,
            title = R.string.view_artist
        ) {
            if (mediaMetadata.artists.size == 1) {
                navController.navigate("artist/${mediaMetadata.artists[0].id}")
                playerBottomSheetState.collapseSoft()
                onDismiss()
            } else {
                showSelectArtistDialog = true
            }
        }
        if (mediaMetadata.album != null && !mediaMetadata.isLocal) {
            GridMenuItem(
                icon = R.drawable.album,
                title = R.string.view_album
            ) {
                navController.navigate("album/${mediaMetadata.album.id}")
                playerBottomSheetState.collapseSoft()
                onDismiss()
            }
        }

        if (mediaMetadata.isLocal != true)
            GridMenuItem(
                icon = Icons.Rounded.Share,
                title = R.string.share
            ) {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${mediaMetadata.id}")
                }
                context.startActivity(Intent.createChooser(intent, null))
                onDismiss()
            }
        GridMenuItem(
            icon = Icons.Rounded.Info,
            title = R.string.details
        ) {
            showDetailsDialog = true
        }
        SleepTimerGridMenu(
            sleepTimerTimeLeft = sleepTimerTimeLeft,
            enabled = sleepTimerEnabled
        ) {
            if (sleepTimerEnabled) playerConnection.service.sleepTimer.clear()
            else showSleepTimerDialog = true
        }
        GridMenuItem(
            icon = Icons.Rounded.Tune,
            title = R.string.advanced
        ) {
            showPitchTempoDialog = true
        }
        // ---------- QR Kod Butonu (BAŞLANGIÇ) ----------
        GridMenuItem(
            icon = Icons.Rounded.QrCode, // Uygun bir ikon ekleyin
            title = R.string.add_with_qr_code // Uygun bir string resource ekleyin
        ) {
            showQrDialog = true
            qrDialogState = QrDialogState.Selection // Başlangıçta seçim ekranını göster
        }
        // ---------- QR Kod Butonu (BİTŞİ) ----------
    }
    if (showQrDialog) {
        QrDialog(
            mediaMetadata = mediaMetadata,
            qrDialogState = qrDialogState,
            onQrDialogStateChange = { qrDialogState = it },
            onDismiss = { showQrDialog = false },
            onSongScanned = { scannedSongId = it;  showQrDialog = false },
            playerConnection = playerConnection
        )
    }

    // ---------- Tarama İşleminden Sonra Şarkıyı Oynat ----------
    LaunchedEffect(scannedSongId) {
        scannedSongId?.let { songId ->
            //Şarkıyı çalmak için PlayerConnection kullanımı:
            playerConnection.playSong(songId) //playSong playerConnection'da tanımlı olmalı.

        }
    }
}

// ---------- Durumları Temsil Eden enum class'ı ----------
sealed class QrDialogState {
    object Selection : QrDialogState()
    object Generate : QrDialogState()
    object Scan : QrDialogState()
}

@Composable
fun QrDialog(
    mediaMetadata: MediaMetadata,
    qrDialogState: QrDialogState,
    onQrDialogStateChange: (QrDialogState) -> Unit,
    onDismiss: () -> Unit,
    onSongScanned: (String) -> Unit,
    playerConnection: PlayerConnection // ---------- PlayerConnection'ı parametre olarak ekle ----------
) {

    // ---------- Kamera İzni ----------
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // ---------- İzin Verildi -> Tarama Durumuna Geç ----------
            onQrDialogStateChange(QrDialogState.Scan)
        } else {
            // ---------- İzin Verilmedi -> Kullanıcı Bilgilendir ----------
            Toast.makeText(context, R.string.camera_permission_denied, Toast.LENGTH_SHORT).show()

        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.share_song)) },
        text = {
            when (qrDialogState) {
                QrDialogState.Selection -> {
                    Column {
                        Button(onClick = { onQrDialogStateChange(QrDialogState.Generate) }) {
                            Text(stringResource(R.string.generate_qr_code))
                        }
                        Button(onClick = {
                            // ---------- Kamera İzni Kontrol ----------
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                                onQrDialogStateChange(QrDialogState.Scan)
                            } else {
                                // ---------- İzin İste ----------
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }

                        }) {
                            Text(stringResource(R.string.scan_qr_code))
                        }
                    }
                }
                QrDialogState.Generate -> {
                    val qrBitmap = rememberQrBitmap(content = "echowave:song:${mediaMetadata.id}") // ---------- Örnek Paylaşım Formatı ----------
                    qrBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = stringResource(R.string.qr_code), //String resource
                            modifier = Modifier.size(256.dp) // Boyutu ayarla
                        )
                    } ?: Text(stringResource(R.string.qr_code_generation_failed)) // Hata mesajı
                }
                QrDialogState.Scan -> {
                    QrCodeScanner { scannedContent ->
                        if (scannedContent != null) {

                            val prefix = "echowave:song:"
                            if(scannedContent.startsWith(prefix)){
                                val songId = scannedContent.removePrefix(prefix)
                                onSongScanned(songId)
                            } else {
                                // ---------- Hatalı QR Kod ----------
                                Toast.makeText(context, R.string.ınvalid_qr_code, Toast.LENGTH_SHORT).show()
                                onQrDialogStateChange(QrDialogState.Selection) //Seçim ekranına geri dön.
                            }

                        }
                        onDismiss() // Tarayıcıyı kapattıktan sonra dialog'u da kapat
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

@Composable
fun QrCodeScanner(onQrCodeScanned: (String?) -> Unit) {
    val context = LocalContext.current
    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            onQrCodeScanned(result.contents)
        } else {
            onQrCodeScanned(null) // Tarama iptal edildi veya başarısız oldu
        }
    }

    val options = ScanOptions().apply {
        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        setPrompt(context.getString(R.string.scan_qr_code_prompt)) // Tarama ekranındaki mesaj
        setCameraId(0)   // Kullanılacak kamera (0 genellikle arka kameradır)
        setBeepEnabled(true)  // Tarama başarılı olduğunda bip sesi
        setOrientationLocked(false) //Ekran yönünü kilitle
        setBarcodeImageEnabled(false)  //Barkodun resmini kaydet

    }

    LaunchedEffect(Unit) { // veya başka bir key, tarayıcıyı başlatmak istediğinizde
        scanLauncher.launch(options)
    }

}

@Composable
fun rememberQrBitmap(content: String, size: Int = 512): Bitmap? {
    return remember(content) {
        if (content.isBlank()) {
            null
        } else {
            generateQrCode(content, size)
        }
    }
}

fun generateQrCode(content: String, size: Int): Bitmap {
    val qrCodeWriter = QRCodeWriter()
    val bitMatrix: BitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, size, size)
    val width = bitMatrix.width
    val height = bitMatrix.height
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
        }
    }
    return bitmap
}

@Composable
fun PitchTempoDialog(
    onDismiss: () -> Unit,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    var tempo by remember {
        mutableStateOf(playerConnection.player.playbackParameters.speed)
    }
    var transposeValue by remember {
        mutableStateOf(round(12 * log2(playerConnection.player.playbackParameters.pitch)).toInt())
    }
    val updatePlaybackParameters = {
        playerConnection.player.playbackParameters = PlaybackParameters(tempo, 2f.pow(transposeValue.toFloat() / 12))
    }

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(
                onClick = {
                    tempo = 1f
                    transposeValue = 0
                    updatePlaybackParameters()
                }
            ) {
                Text(stringResource(R.string.reset))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        text = {
            Column {
                ValueAdjuster(
                    icon = Icons.Rounded.SlowMotionVideo,
                    currentValue = tempo,
                    values = (0..35).map { round((0.25f + it * 0.05f) * 100) / 100 },
                    onValueUpdate = {
                        tempo = it
                        updatePlaybackParameters()
                    },
                    valueText = { "x$it" },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                ValueAdjuster(
                    icon = Icons.Rounded.Tune,
                    currentValue = transposeValue,
                    values = (-12..12).toList(),
                    onValueUpdate = {
                        transposeValue = it
                        updatePlaybackParameters()
                    },
                    valueText = { "${if (it > 0) "+" else ""}$it" }
                )
            }
        }
    )
}

@Composable
fun <T> ValueAdjuster(
    icon: ImageVector,
    currentValue: T,
    values: List<T>,
    onValueUpdate: (T) -> Unit,
    valueText: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(28.dp)
        )

        IconButton(
            enabled = currentValue != values.first(),
            onClick = {
                onValueUpdate(values[values.indexOf(currentValue) - 1])
            }
        ) {
            Icon(
                imageVector = Icons.Rounded.RemoveCircleOutline,
                contentDescription = null
            )
        }

        Text(
            text = valueText(currentValue),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(80.dp)
        )

        IconButton(
            enabled = currentValue != values.last(),
            onClick = {
                onValueUpdate(values[values.indexOf(currentValue) + 1])
            }
        ) {
            Icon(
                imageVector = Icons.Rounded.AddCircleOutline,
                contentDescription = null
            )
        }
    }
}