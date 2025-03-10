package com.samyak.simpletube.playback

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.SQLException
import android.media.audiofx.AudioEffect
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Binder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.datastore.preferences.core.edit
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_POSITION_DISCONTINUITY
import androidx.media3.common.Player.EVENT_TIMELINE_CHANGED
import androidx.media3.common.Player.MEDIA_ITEM_TRANSITION_REASON_AUTO
import androidx.media3.common.Player.MEDIA_ITEM_TRANSITION_REASON_SEEK
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.Player.STATE_IDLE
import androidx.media3.common.Timeline
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.analytics.PlaybackStats
import androidx.media3.exoplayer.analytics.PlaybackStatsListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioOffloadSupportProvider
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaController
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.samyak.simpletube.MainActivity
import com.samyak.simpletube.R
import com.samyak.simpletube.constants.AudioNormalizationKey
import com.samyak.simpletube.constants.AudioOffload
import com.samyak.simpletube.constants.AudioQuality
import com.samyak.simpletube.constants.AudioQualityKey
import com.samyak.simpletube.constants.KeepAliveKey
import com.samyak.simpletube.constants.LastPosKey
import com.samyak.simpletube.constants.MediaSessionConstants.CommandToggleLike
import com.samyak.simpletube.constants.MediaSessionConstants.CommandToggleRepeatMode
import com.samyak.simpletube.constants.MediaSessionConstants.CommandToggleShuffle
import com.samyak.simpletube.constants.PauseListenHistoryKey
import com.samyak.simpletube.constants.PersistentQueueKey
import com.samyak.simpletube.constants.PlayerVolumeKey
import com.samyak.simpletube.constants.RepeatModeKey
import com.samyak.simpletube.constants.ShowLyricsKey
import com.samyak.simpletube.constants.SkipOnErrorKey
import com.samyak.simpletube.constants.SkipSilenceKey
import com.samyak.simpletube.constants.minPlaybackDurKey
import com.samyak.simpletube.db.MusicDatabase
import com.samyak.simpletube.db.entities.Event
import com.samyak.simpletube.db.entities.FormatEntity
import com.samyak.simpletube.db.entities.LyricsEntity
import com.samyak.simpletube.db.entities.RelatedSongMap
import com.samyak.simpletube.di.DownloadCache
import com.samyak.simpletube.extensions.SilentHandler
import com.samyak.simpletube.extensions.collect
import com.samyak.simpletube.extensions.collectLatest
import com.samyak.simpletube.extensions.currentMetadata
import com.samyak.simpletube.extensions.findNextMediaItemById
import com.samyak.simpletube.extensions.metadata
import com.samyak.simpletube.extensions.setOffloadEnabled
import com.samyak.simpletube.extensions.toMediaItem
import com.samyak.simpletube.lyrics.LyricsHelper
import com.samyak.simpletube.models.QueueBoard
import com.samyak.simpletube.models.isShuffleEnabled
import com.samyak.simpletube.models.toMediaMetadata
import com.samyak.simpletube.playback.PlayerConnection.Companion.queueBoard
import com.samyak.simpletube.playback.queues.ListQueue
import com.samyak.simpletube.playback.queues.Queue
import com.samyak.simpletube.playback.queues.YouTubeQueue
import com.samyak.simpletube.utils.CoilBitmapLoader
import com.samyak.simpletube.utils.dataStore
import com.samyak.simpletube.utils.enumPreference
import com.samyak.simpletube.utils.get
import com.samyak.simpletube.utils.reportException
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.models.SongItem
import com.zionhuang.innertube.models.WatchEndpoint
import com.zionhuang.innertube.models.response.PlayerResponse
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.pow

const val MAX_CONSECUTIVE_ERR = 3

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@AndroidEntryPoint
class MusicService : MediaLibraryService(),
    Player.Listener,
    PlaybackStatsListener.Callback {
    @Inject
    lateinit var database: MusicDatabase

    @Inject
    lateinit var downloadUtil: DownloadUtil

    @Inject
    lateinit var lyricsHelper: LyricsHelper

    @Inject
    lateinit var mediaLibrarySessionCallback: MediaLibrarySessionCallback

    private val scope = CoroutineScope(Dispatchers.Main) + Job()
    private val binder = MusicBinder()

    private lateinit var connectivityManager: ConnectivityManager

    private val audioQuality by enumPreference(this, AudioQualityKey, AudioQuality.AUTO)

    var queueTitle: String? = null
    var queuePlaylistId: String? = null
    private var lastMediaItemIndex = -1

    val currentMediaMetadata = MutableStateFlow<com.samyak.simpletube.models.MediaMetadata?>(null)

    private val currentSong = currentMediaMetadata.flatMapLatest { mediaMetadata ->
        database.song(mediaMetadata?.id)
    }.stateIn(scope, SharingStarted.Lazily, null)

    private val currentFormat = currentMediaMetadata.flatMapLatest { mediaMetadata ->
        database.format(mediaMetadata?.id)
    }

    private val normalizeFactor = MutableStateFlow(1f)
    val playerVolume = MutableStateFlow(dataStore.get(PlayerVolumeKey, 1f).coerceIn(0f, 1f))

    lateinit var sleepTimer: SleepTimer

    @Inject
    @DownloadCache
    lateinit var downloadCache: SimpleCache

    lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaLibrarySession

    private var isAudioEffectSessionOpened = false

    var consecutivePlaybackErr = 0

    override fun onCreate() {
        super.onCreate()
        val notificationBuilder = NotificationCompat.Builder(this, KEEP_ALIVE_CHANNEL_ID)
            .setContentTitle(getString(R.string.music_player))
            .setSmallIcon(R.drawable.music)
            .setOngoing(true) // Ensures notification stays until service stops

        val notification = notificationBuilder.build()

        // FG notification
        if (dataStore.get(KeepAliveKey, false)) {
            try {
                startService(Intent(this, KeepAlive::class.java))
            } catch (e: Exception) {
                reportException(e)
            }
        } else {
            try {
                stopService(Intent(this, KeepAlive::class.java))
            } catch (e: Exception) {
                reportException(e)
            }
        }

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(createDataSourceFactory()))
            .setRenderersFactory(createRenderersFactory())
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(), true
            )
            .setSeekBackIncrementMs(5000)
            .setSeekForwardIncrementMs(5000)
            .build()
            .apply {
                addListener(this@MusicService)

                setOffloadEnabled(dataStore.get(AudioOffload, false))

                // skip on error
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        super.onPlayerError(error)
                        if (!dataStore.get(SkipOnErrorKey, true)) {
                            return
                        }

                        consecutivePlaybackErr += 2

                        Toast.makeText(
                            this@MusicService,
                            "Playback error: ${error.message} (${error.errorCode}): ${error.cause?.message?: "No further errors."} ",
                            Toast.LENGTH_SHORT
                        ).show()

                        /**
                         * Auto skip to the next media item on error.
                         *
                         * To prevent a "runaway diesel engine" scenario, force the user to take action after
                         * too many errors come up too quickly. Pause to show player "stopped" state
                         */
                        val nextWindowIndex = player.nextMediaItemIndex
                        if (consecutivePlaybackErr <= MAX_CONSECUTIVE_ERR && nextWindowIndex != C.INDEX_UNSET) {
                            player.seekTo(nextWindowIndex, C.TIME_UNSET)
                            player.prepare()
                            player.play()
                        } else {
                            player.pause()
                            Toast.makeText(
                                this@MusicService,
                                "Playback stopped due to too many errors",
                                Toast.LENGTH_SHORT
                            ).show()
                            consecutivePlaybackErr = 0
                        }
                    }

                    // start playback again on seek
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                        super.onMediaItemTransition(mediaItem, reason)
                        // +2 when and error happens, and -1 when transition. Thus when error, number increments by 1, else doesn't change
                        if (consecutivePlaybackErr > 0) {
                            consecutivePlaybackErr --
                        }

                        if (player.isPlaying && reason == MEDIA_ITEM_TRANSITION_REASON_SEEK) {
                            player.prepare()
                            player.play()
                        }

                        // this absolute eye sore detects if we loop back to the beginning of queue, when shuffle AND repeat all
                        // no, when repeat mode is on, player does not "STATE_ENDED"
                        if (player.currentMediaItemIndex == 0 && lastMediaItemIndex == player.mediaItemCount - 1 &&
                            (reason == MEDIA_ITEM_TRANSITION_REASON_AUTO || reason == MEDIA_ITEM_TRANSITION_REASON_SEEK) &&
                            isShuffleEnabled.value && player.repeatMode == REPEAT_MODE_ALL) {
                            queueBoard.shuffleCurrent(this@MusicService, false) // reshuffle queue
                            queueBoard.setCurrQueue(this@MusicService)
                        }
                        lastMediaItemIndex = player.currentMediaItemIndex

                        updateNotification() // also updates when queue changes

                        queueBoard.setCurrQueuePosIndex(player.currentMediaItemIndex, this@MusicService)
                        queueTitle = queueBoard.getCurrentQueue()?.title
                    }
                })
                sleepTimer = SleepTimer(scope, this)
                addListener(sleepTimer)
                addAnalyticsListener(PlaybackStatsListener(false, this@MusicService))
            }

        mediaLibrarySessionCallback.apply {
            toggleLike = ::toggleLike
            toggleLibrary = ::toggleLibrary
        }

        mediaSession = MediaLibrarySession.Builder(this, player, mediaLibrarySessionCallback)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setBitmapLoader(CoilBitmapLoader(this, scope))
            .build()

        player.repeatMode = dataStore.get(RepeatModeKey, REPEAT_MODE_OFF)

        // Keep a connected controller so that notification works
        val sessionToken = SessionToken(this, ComponentName(this, MusicService::class.java))
        val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener({ controllerFuture.get() }, MoreExecutors.directExecutor())

        connectivityManager = getSystemService()!!

        combine(playerVolume, normalizeFactor) { playerVolume, normalizeFactor ->
            playerVolume * normalizeFactor
        }.collectLatest(scope) {
            player.volume = it
        }

        playerVolume.debounce(1000).collect(scope) { volume ->
            dataStore.edit { settings ->
                settings[PlayerVolumeKey] = volume
            }
        }

        currentSong.collect(scope) {
            updateNotification()
        }

        combine(
            currentMediaMetadata.distinctUntilChangedBy { it?.id },
            dataStore.data.map { it[ShowLyricsKey] ?: false }.distinctUntilChanged()
        ) { mediaMetadata, showLyrics ->
            mediaMetadata to showLyrics
        }.collectLatest(scope) { (mediaMetadata, showLyrics) ->
            if (showLyrics && mediaMetadata != null && database.lyrics(mediaMetadata.id).first() == null) {
                val lyrics = lyricsHelper.getLyrics(mediaMetadata)
                database.query {
                    upsert(
                        LyricsEntity(
                            id = mediaMetadata.id,
                            lyrics = lyrics
                        )
                    )
                }
            }
        }

        dataStore.data
            .map { it[SkipSilenceKey] ?: false }
            .distinctUntilChanged()
            .collectLatest(scope) {
                player.skipSilenceEnabled = it
            }

        combine(
            currentFormat,
            dataStore.data
                .map { it[AudioNormalizationKey] ?: true }
                .distinctUntilChanged()
        ) { format, normalizeAudio ->
            format to normalizeAudio
        }.collectLatest(scope) { (format, normalizeAudio) ->
            normalizeFactor.value = if (normalizeAudio && format?.loudnessDb != null) {
                min(10f.pow(-format.loudnessDb.toFloat() / 20), 1f)
            } else {
                1f
            }
        }

        if (dataStore.get(PersistentQueueKey, true)) {
            queueBoard = QueueBoard(database.readQueue().toMutableList())
            isShuffleEnabled.value = queueBoard.getCurrentQueue()?.shuffled ?: false
            if (queueBoard.getAllQueues().isNotEmpty()) {
                val queue = queueBoard.getCurrentQueue()
                if (queue != null) {
                    isShuffleEnabled.value = queue.shuffled
                    CoroutineScope(Dispatchers.Main).launch {
                        val queuePos = queueBoard.setCurrQueue(this@MusicService, false)
                        if (queuePos != null) {
                            player.seekTo(queuePos, dataStore.get(LastPosKey, C.TIME_UNSET))
                            dataStore.edit { settings ->
                                settings[LastPosKey] = C.TIME_UNSET
                            }
                        }
                    }
                }
            }
        }
    }

    fun updateNotification() {
        mediaSession.setCustomLayout(
            listOf(
                CommandButton.Builder()
                    .setDisplayName(getString(if (queueBoard.getCurrentQueue()?.shuffled == true) R.string.action_shuffle_off else R.string.action_shuffle_on))
                    .setIconResId(if (queueBoard.getCurrentQueue()?.shuffled == true) R.drawable.shuffle_on else R.drawable.shuffle)
                    .setSessionCommand(CommandToggleShuffle)
                    .build(),
                CommandButton.Builder()
                    .setDisplayName(
                        getString(
                            when (player.repeatMode) {
                                REPEAT_MODE_OFF -> R.string.repeat_mode_off
                                REPEAT_MODE_ONE -> R.string.repeat_mode_one
                                REPEAT_MODE_ALL -> R.string.repeat_mode_all
                                else -> throw IllegalStateException()
                            }
                        )
                    )
                    .setIconResId(
                        when (player.repeatMode) {
                            REPEAT_MODE_OFF -> R.drawable.repeat
                            REPEAT_MODE_ONE -> R.drawable.repeat_one_on
                            REPEAT_MODE_ALL -> R.drawable.repeat_on
                            else -> throw IllegalStateException()
                        }
                    )
                    .setSessionCommand(CommandToggleRepeatMode)
                    .build(),
                CommandButton.Builder()
                    .setDisplayName(getString(if (currentSong.value?.song?.liked == true) R.string.action_remove_like else R.string.action_like))
                    .setIconResId(if (currentSong.value?.song?.liked == true) R.drawable.favorite else R.drawable.favorite_border)
                    .setSessionCommand(CommandToggleLike)
                    .setEnabled(currentSong.value != null)
                    .build()
            )
        )
    }

    private suspend fun recoverSong(mediaId: String, playerResponse: PlayerResponse? = null) {
        var playbackUrl = database.format(mediaId).first()?.playbackUrl

        if (playbackUrl == null) {
            playbackUrl = if (playerResponse?.playbackTracking?.videostatsPlaybackUrl?.baseUrl == null)
                YouTube.player(mediaId, registerPlayback = false).getOrNull()?.playbackTracking
                    ?.videostatsPlaybackUrl?.baseUrl!!
            else
                playerResponse.playbackTracking?.videostatsPlaybackUrl?.baseUrl
        }

        playbackUrl?.let {
            try { YouTube.registerPlayback(queuePlaylistId, playbackUrl) }
            catch (exception: UnknownHostException) {
                reportException(exception)
            }
        }

        val song = database.song(mediaId).first()
        val mediaMetadata = withContext(Dispatchers.Main) {
            player.findNextMediaItemById(mediaId)?.metadata
        } ?: return
        val duration = song?.song?.duration?.takeIf { it != -1 }
            ?: mediaMetadata.duration.takeIf { it != -1 }
            ?: (playerResponse ?: YouTube.player(mediaId, registerPlayback = false).getOrNull())?.videoDetails?.lengthSeconds?.toInt()
            ?: -1
        database.query {
            if (song == null) insert(mediaMetadata.copy(duration = duration))
            else if (song.song.duration == -1) update(song.song.copy(duration = duration))
        }
        if (!database.hasRelatedSongs(mediaId)) {
            val relatedEndpoint = YouTube.next(WatchEndpoint(videoId = mediaId)).getOrNull()?.relatedEndpoint ?: return
            val relatedPage = YouTube.related(relatedEndpoint).getOrNull() ?: return
            database.query {
                relatedPage.songs
                    .map(SongItem::toMediaMetadata)
                    .onEach(::insert)
                    .map {
                        RelatedSongMap(
                            songId = mediaId,
                            relatedSongId = it.id
                        )
                    }
                    .forEach(::insert)
            }
        }
    }

    /**
     * Play a queue.
     *
     * @param queue Queue to play.
     * @param playWhenReady
     * @param replace Replace media items instead of the underlying logic
     * @param title Title override for the queue. If this value us unspecified, this method takes the value from queue.
     * If both are unspecified, the title will default to "Queue".
     */
    fun playQueue(queue: Queue, playWhenReady: Boolean = true, replace: Boolean = false, title: String? = null) {
        queueTitle = title
        queuePlaylistId = queue.playlistId

        CoroutineScope(Dispatchers.Main).launch {
            val initialStatus = withContext(Dispatchers.IO) { queue.getInitialStatus() }
            if (queueTitle == null && initialStatus.title != null) { // do not find a title if an override is provided
                queueTitle = initialStatus.title
            }

            // print out queue
//            println("-----------------------------")
//            initialStatus.items.map { println(it.title) }
            if (initialStatus.items.isEmpty()) return@launch
            queueBoard.addQueue(
                queueTitle?: "Queue",
                initialStatus.items,
                player = this@MusicService,
                startIndex = if (initialStatus.mediaItemIndex > 0) initialStatus.mediaItemIndex else 0,
                replace = replace
            )
            queueBoard.setCurrQueue(this@MusicService)

            player.prepare()
            player.playWhenReady = playWhenReady
        }
    }

    fun startRadioSeamlessly() {
        val currentMediaMetadata = player.currentMetadata ?: return
        if (player.currentMediaItemIndex > 0) player.removeMediaItems(0, player.currentMediaItemIndex)
        if (player.currentMediaItemIndex < player.mediaItemCount - 1) player.removeMediaItems(player.currentMediaItemIndex + 1, player.mediaItemCount)
        scope.launch(SilentHandler) {
            val radioQueue = YouTubeQueue(endpoint = WatchEndpoint(videoId = currentMediaMetadata.id))
            val initialStatus = radioQueue.getInitialStatus()
            if (initialStatus.title != null) {
                queueTitle = initialStatus.title
            }
            player.addMediaItems(initialStatus.items.drop(1).map { it.toMediaItem() })
        }
    }

    /**
     * Add items to queue, right after current playing item
     */
    fun enqueueNext(items: List<MediaItem>) {
        val currentQueue = queueBoard.getCurrentQueue()
        if (currentQueue == null) {
            if (items.isNotEmpty()) {
                playQueue(
                    ListQueue(
                        title = items.first().mediaMetadata.title.toString(),
                        items = items.mapNotNull { it.metadata }
                    )
                )
            }
        } else {
            queueBoard.addSongsToQueue(currentQueue, player.currentMediaItemIndex + 1, items.mapNotNull { it.metadata }, this)
        }
    }


    /**
     * Add items to end of current queue
     */
    fun enqueueEnd(items: List<MediaItem>) {
        queueBoard.enqueueEnd(items.mapNotNull { it.metadata }, this)
    }

    fun toggleLibrary() {
        database.query {
            currentSong.value?.let {
                update(it.song.toggleLibrary())
            }
        }
    }

    fun toggleLike() {
        database.query {
            currentSong.value?.let {
                val song = it.song.toggleLike()
                update(song)
                downloadUtil.autoDownloadIfLiked(song)
            }
        }
    }

    private fun openAudioEffectSession() {
        if (isAudioEffectSessionOpened) return
        isAudioEffectSessionOpened = true
        sendBroadcast(
            Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
        )
    }

    private fun closeAudioEffectSession() {
        if (!isAudioEffectSessionOpened) return
        isAudioEffectSessionOpened = false
        sendBroadcast(
            Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, player.audioSessionId)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
            }
        )
    }

    override fun onPlaybackStateChanged(@Player.State playbackState: Int) {
        if (playbackState == STATE_IDLE) {
            queuePlaylistId = null
            queueTitle = null
        }
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(Player.EVENT_PLAYBACK_STATE_CHANGED, Player.EVENT_PLAY_WHEN_READY_CHANGED)) {
            val isBufferingOrReady = player.playbackState == Player.STATE_BUFFERING || player.playbackState == Player.STATE_READY
            if (isBufferingOrReady && player.playWhenReady) {
                openAudioEffectSession()
            } else {
                closeAudioEffectSession()
            }
        }
        if (events.containsAny(EVENT_TIMELINE_CHANGED, EVENT_POSITION_DISCONTINUITY)) {
            currentMediaMetadata.value = player.currentMetadata
        }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        updateNotification()
        scope.launch {
            dataStore.edit { settings ->
                settings[RepeatModeKey] = repeatMode
            }
        }
    }

    private fun createCacheDataSource(): CacheDataSource.Factory =
        CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(
                DefaultDataSource.Factory(
                    this,
                    OkHttpDataSource.Factory(
                        OkHttpClient.Builder()
                            .proxy(YouTube.proxy)
                            .build()
                    )
                )
            )
            .setCacheWriteDataSinkFactory(null)
            .setFlags(FLAG_IGNORE_CACHE_ON_ERROR)

    private fun createDataSourceFactory(): DataSource.Factory {
        val songUrlCache = HashMap<String, Pair<String, Long>>()
        return ResolvingDataSource.Factory(createCacheDataSource()) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")

            // find a better way to detect local files later...
            if (mediaId.startsWith("LA")) {
                val songPath = runBlocking(Dispatchers.IO) {
                    database.song(mediaId).firstOrNull()?.song?.localPath
                }
                if (songPath == null) {
                    throw PlaybackException(getString(R.string.file_size), Throwable(), PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND)
                }

                return@Factory dataSpec.withUri(Uri.fromFile(File(songPath)))
            }

            if (downloadCache.isCached(mediaId, dataSpec.position, if (dataSpec.length >= 0) dataSpec.length else 1)) {
                scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                return@Factory dataSpec
            }

            songUrlCache[mediaId]?.takeIf { it.second < System.currentTimeMillis() }?.let {
                scope.launch(Dispatchers.IO) { recoverSong(mediaId) }
                return@Factory dataSpec.withUri(it.first.toUri())
            }

            // Check whether format exists so that users from older version can view format details
            // There may be inconsistent between the downloaded file and the displayed info if user change audio quality frequently
            val playedFormat = runBlocking(Dispatchers.IO) { database.format(mediaId).first() }
            val playerResponse = runBlocking(Dispatchers.IO) {
                YouTube.player(mediaId, registerPlayback = false)
            }.getOrElse { throwable ->
                when (throwable) {
                    is ConnectException, is UnknownHostException -> {
                        throw PlaybackException(getString(R.string.error_no_internet), throwable, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED)
                    }

                    is SocketTimeoutException -> {
                        throw PlaybackException(getString(R.string.error_timeout), throwable, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT)
                    }

                    else -> throw PlaybackException(getString(R.string.error_unknown), throwable, PlaybackException.ERROR_CODE_REMOTE_ERROR)
                }
            }
            if (playerResponse.playabilityStatus.status != "OK") {
                throw PlaybackException(playerResponse.playabilityStatus.reason, null, PlaybackException.ERROR_CODE_REMOTE_ERROR)
            }

            val format =
                if (playedFormat != null) {
                    playerResponse.streamingData?.adaptiveFormats?.find {
                        // Use itag to identify previously played format
                        it.itag == playedFormat.itag
                    }
                } else {
                    playerResponse.streamingData?.adaptiveFormats
                        ?.filter { it.isAudio }
                        ?.maxByOrNull {
                            it.bitrate * when (audioQuality) {
                                AudioQuality.AUTO -> if (connectivityManager.isActiveNetworkMetered) -1 else 50
                                AudioQuality.EXTREME -> 300
                                AudioQuality.MAX -> 250
                                AudioQuality.HIGH -> 150
                                AudioQuality.LOW -> 50
                            } + (if (it.mimeType.startsWith("audio/webm")) 5120 else 0) // prefer opus stream
                        }
                } ?: throw PlaybackException(getString(R.string.error_no_stream), null, ERROR_CODE_NO_STREAM)

            database.query {
                upsert(
                    FormatEntity(
                        id = mediaId,
                        itag = format.itag,
                        mimeType = format.mimeType.split(";")[0],
                        codecs = format.mimeType.split("codecs=")[1].removeSurrounding("\""),
                        bitrate = format.bitrate,
                        sampleRate = format.audioSampleRate,
                        contentLength = format.contentLength!!,
                        loudnessDb = playerResponse.playerConfig?.audioConfig?.loudnessDb,
                        playbackUrl = playerResponse.playbackTracking?.videostatsPlaybackUrl?.baseUrl!!
                    )
                )
            }
            scope.launch(Dispatchers.IO) { recoverSong(mediaId, playerResponse) }

            songUrlCache[mediaId] = format.url!! to playerResponse.streamingData!!.expiresInSeconds * 1000L
            dataSpec.withUri(format.url!!.toUri()).subrange(dataSpec.uriPositionOffset, CHUNK_LENGTH)
        }
    }

    private fun createRenderersFactory() = object : DefaultRenderersFactory(this) {
        override fun buildAudioSink(
            context: Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean
        ): AudioSink {
            return DefaultAudioSink.Builder(this@MusicService)
                .setEnableFloatOutput(enableFloatOutput)

                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessorChain(
                    DefaultAudioSink.DefaultAudioProcessorChain(
                        emptyArray(),
                        SilenceSkippingAudioProcessor(),
                        SonicAudioProcessor()
                    )
                )
                .setAudioOffloadSupportProvider(DefaultAudioOffloadSupportProvider(context))
                .build()
        }
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        if (player.shuffleModeEnabled) {
            triggerShuffle()
            player.shuffleModeEnabled = false
        }
    }

    /**
     * Shuffles the queue
     */
    fun triggerShuffle() {
        val oldIndex = player.currentMediaItemIndex
        queueBoard.setCurrQueuePosIndex(oldIndex, this)
        val currentQueue = queueBoard.getCurrentQueue() ?: return

        // shuffle and update player playlist
        if (!currentQueue.shuffled) {
            queueBoard.shuffleCurrent(this)
            player.moveMediaItem(oldIndex, 0)
            val newItems = currentQueue.getCurrentQueueShuffled()
            player.replaceMediaItems(1, Int.MAX_VALUE,
                newItems.subList(1, newItems.size).map { it.toMediaItem() })
        } else {
            val unshuffledPos = queueBoard.unShuffleCurrent(this)
            player.moveMediaItem(oldIndex, unshuffledPos)
            val newItems = currentQueue.getCurrentQueueShuffled()
            // replace items up to current playing, then replace items after current
            player.replaceMediaItems(0, unshuffledPos,
                newItems.subList(0, unshuffledPos).map { it.toMediaItem() })
            player.replaceMediaItems(unshuffledPos + 1, Int.MAX_VALUE,
                newItems.subList(unshuffledPos + 1, newItems.size).map { it.toMediaItem() })
        }

        updateNotification()
    }

    override fun onPlaybackStatsReady(eventTime: AnalyticsListener.EventTime, playbackStats: PlaybackStats) {
        val mediaItem = eventTime.timeline.getWindow(eventTime.windowIndex, Timeline.Window()).mediaItem
        var minPlaybackDur = (dataStore.get(minPlaybackDurKey, 30).toFloat() / 100)
        // ensure within bounds
        if (minPlaybackDur >= 1f) {
            minPlaybackDur = 0.99f // Ehhh 99 is good enough to avoid any rounding errors
        } else if (minPlaybackDur < 0.01f) {
            minPlaybackDur = 0.01f // Still want "spam skipping" to not count as plays
        }

//        println("Playback ratio: ${playbackStats.totalPlayTimeMs.toFloat() / ((mediaItem.metadata?.duration?.times(1000)) ?: -1)} Min threshold: $minPlaybackDur")
        if (playbackStats.totalPlayTimeMs.toFloat() / ((mediaItem.metadata?.duration?.times(1000)) ?: -1) >= minPlaybackDur
            && !dataStore.get(PauseListenHistoryKey, false)) {
            database.query {
                incrementPlayCount(mediaItem.mediaId)
                incrementTotalPlayTime(mediaItem.mediaId, playbackStats.totalPlayTimeMs)
                try {
                    insert(
                        Event(
                            songId = mediaItem.mediaId,
                            timestamp = LocalDateTime.now(),
                            playTime = playbackStats.totalPlayTimeMs
                        )
                    )
                } catch (_: SQLException) {
                }
            }
        }
    }

    @Deprecated("This nukes the entire db and adds everything again. Saving queues is to be done inside QueueBoard, incrementally.")
    private fun saveQueueToDisk() {
        // TODO: get rid of this. Yeah I'd want to update individual queues instead of nuking the entire db and writing everything but ehhhhh that's for later
        CoroutineScope(Dispatchers.IO).launch {
            database.rewriteAllQueues(queueBoard.getAllQueues())
        }
    }

    override fun onDestroy() {
        if (dataStore.get(PersistentQueueKey, true)) {
            saveQueueToDisk()
            scope.launch {
                dataStore.edit { settings ->
                    settings[LastPosKey] = player.currentPosition
                }
            }
        }
        mediaSession.release()
        player.removeListener(this)
        player.removeListener(sleepTimer)
        player.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = super.onBind(intent) ?: binder

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (dataStore.get(PersistentQueueKey, true)) {
            saveQueueToDisk()
            scope.launch {
                dataStore.edit { settings ->
                    settings[LastPosKey] = player.currentPosition
                }
            }
        }

        super.onTaskRemoved(rootIntent)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    inner class MusicBinder : Binder() {
        val service: MusicService
            get() = this@MusicService
    }

    companion object {
        const val ROOT = "root"
        const val SONG = "song"
        const val ARTIST = "artist"
        const val ALBUM = "album"
        const val PLAYLIST = "playlist"

        const val CHANNEL_ID = "music_channel_01"
        const val KEEP_ALIVE_CHANNEL_ID = "outertune_keep_alive"
        const val ERROR_CODE_NO_STREAM = 1000001
        const val CHUNK_LENGTH = 512 * 1024L
    }

    fun playSongById(songId: String) {
        scope.launch(Dispatchers.IO) {
            val song = database.song(songId).firstOrNull()
            song?.let { songEntity ->
                val mediaItem = songEntity.toMediaItem()

                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()

            } ?: run {
                Toast.makeText(this@MusicService, "Song not found", Toast.LENGTH_SHORT).show()
            }
        }
    }
}