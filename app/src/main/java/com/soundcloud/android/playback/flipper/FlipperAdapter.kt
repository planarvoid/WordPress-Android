package com.soundcloud.android.playback.flipper

import com.soundcloud.android.accounts.AccountOperations
import com.soundcloud.android.crypto.CryptoOperations
import com.soundcloud.android.events.ConnectionType
import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.events.PlaybackErrorEvent
import com.soundcloud.android.events.PlayerType
import com.soundcloud.android.model.Urn
import com.soundcloud.android.playback.HlsStreamUrlBuilder
import com.soundcloud.android.playback.PlaybackItem
import com.soundcloud.android.playback.PlaybackStateTransition
import com.soundcloud.android.playback.PlaybackType
import com.soundcloud.android.playback.Player
import com.soundcloud.android.playback.PreloadItem
import com.soundcloud.android.playback.common.ProgressChangeHandler
import com.soundcloud.android.playback.common.StateChangeHandler
import com.soundcloud.android.utils.ConnectionHelper
import com.soundcloud.android.utils.CurrentDateProvider
import com.soundcloud.android.utils.ErrorUtils
import com.soundcloud.android.utils.LockUtil
import com.soundcloud.android.utils.Log
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.rx.eventbus.EventBusV2
import javax.inject.Inject

@Suppress("TooManyFunctions", "CatchThrowable")
@OpenForTesting
class FlipperAdapter
@Inject
internal constructor(flipperWrapperFactory: FlipperWrapperFactory,
                     private val accountOperations: AccountOperations,
                     private val hlsStreamUrlBuilder: HlsStreamUrlBuilder,
                     private val connectionHelper: ConnectionHelper,
                     private val wakelockUtil: LockUtil,
                     private val stateHandler: StateChangeHandler,
                     private val progressChangeHandler: ProgressChangeHandler,
                     private val dateProvider: CurrentDateProvider,
                     private val eventBus: EventBusV2,
                     private val cryptoOperations: CryptoOperations,
                     private val performanceReporter: PerformanceReporter) : Player {

    private val flipperWrapper: FlipperWrapper = flipperWrapperFactory.create(this)

    @Volatile private var currentStreamUrl: String? = null
    private var currentPlaybackItem: PlaybackItem? = null

    // Flipper may send past progress events when seeking, leading to UI glitches.
    // This boolean helps us to workaround this.
    private var isSeekPending: Boolean = false
    private var progress: Long = 0

    override fun preload(preloadItem: PreloadItem) = flipperWrapper.prefetch(hlsStreamUrlBuilder.buildStreamUrl(preloadItem))

    override fun play(playbackItem: PlaybackItem) {
        if (!accountOperations.isUserLoggedIn) throw IllegalStateException("Cannot play a track if no soundcloud account exists")

        currentPlaybackItem = playbackItem
        currentPlaybackItem?.let {
            ErrorUtils.log(android.util.Log.DEBUG, TAG, "play(): ${it.urn} in duration ${it.duration}]")
        }
        stateHandler.removeMessages(0)
        isSeekPending = false
        progress = 0

        if (isCurrentStreamUrl(hlsStreamUrlBuilder.buildStreamUrl(playbackItem))) {
            seek(playbackItem.startPosition)
            startPlayback()
        } else {
            openStream(playbackItem)
            startPlayback()
        }
    }

    private fun openStream(playbackItem: PlaybackItem) {
        currentStreamUrl = hlsStreamUrlBuilder.buildStreamUrl(playbackItem)

        when (playbackItem.playbackType) {
            PlaybackType.AUDIO_DEFAULT, PlaybackType.AUDIO_SNIPPET -> flipperWrapper.open(currentStreamUrl, playbackItem.startPosition)
            PlaybackType.AUDIO_OFFLINE -> {
                val deviceSecret = cryptoOperations.checkAndGetDeviceKey()
                flipperWrapper.openEncrypted(currentStreamUrl, deviceSecret.key, deviceSecret.initVector, playbackItem.startPosition)
            }
            else -> throw IllegalStateException("Flipper does not accept playback type: ${playbackItem.playbackType}")
        }
    }

    override fun resume(playbackItem: PlaybackItem) {
        currentPlaybackItem = playbackItem
        currentPlaybackItem?.let {
            Log.d(TAG, "resume() called with: playbackItem = [$it, ${it.urn} in duration ${it.duration}]")
        }
        startPlayback()
    }

    private fun startPlayback() = flipperWrapper.play()

    override fun pause() = flipperWrapper.pause()

    override fun seek(position: Long) {
        Log.d(TAG, "seek() called with: position = [$position]")
        isSeekPending = true
        progress = position
        flipperWrapper.seek(position)
    }

    override fun getProgress() = progress

    override fun getVolume() = flipperWrapper.volume

    override fun setVolume(level: Float) {
        flipperWrapper.volume = level
    }

    override fun stop() = flipperWrapper.pause()

    override fun stopForTrackTransition() = stop()

    override fun destroy() = flipperWrapper.destroy()

    override fun setListener(playerListener: Player.PlayerListener) {
        this.stateHandler.setPlayerListener(playerListener)
        this.progressChangeHandler.setPlayerListener(playerListener)
    }

    override fun isSeekable() = true

    override fun getPlayerType() = PlayerType.FLIPPER

    fun onProgressChanged(event: ProgressChange) {
        try {
            if (isCurrentStreamUrl(event.uri) && !isSeekPending) {
                progress = event.position
                progressChangeHandler.report(event.position, event.duration)
            }
        } catch (t: Throwable) {
            ErrorUtils.handleThrowableOnMainThread(t, javaClass)
        }
    }

    fun onPerformanceEvent(event: AudioPerformanceEvent) {
        try {
            currentPlaybackItem?.let { performanceReporter.report(it, event, playerType) }
        } catch (t: Throwable) {
            ErrorUtils.handleThrowableOnMainThread(t, javaClass)
        }
    }

    private fun isCurrentStreamUrl(uri: String) = uri == currentStreamUrl

    fun onStateChanged(event: StateChange) {
        ErrorUtils.log(android.util.Log.INFO, TAG, "onStateChanged() called in ${event.state} with: event = [$event]")
        handleStateChanged(event)
    }

    fun onBufferingChanged(event: StateChange) {
        Log.i(TAG, "onBufferingChanged() called in ${event.state} with: event = [$event]")
        handleStateChanged(event)
    }

    private fun handleStateChanged(event: StateChange) {
        try {
            val currentPlaybackItemUrn = currentPlaybackItem?.urn
            if (currentPlaybackItemUrn != null) {
                if (isCurrentStreamUrl(event.uri)) reportStateTransition(event, currentPlaybackItemUrn, if (isSeekPending) progress else event.position)
            } else {
                ErrorUtils.handleSilentException(IllegalStateException("State reported with no playback item. State is $event"))
            }
        } catch (t: Throwable) {
            ErrorUtils.handleThrowableOnMainThread(t, javaClass)
        }
    }

    fun onSeekingStatusChanged(seekingStatusChange: SeekingStatusChange) {
        try {
            if (isCurrentStreamUrl(seekingStatusChange.uri)) {
                isSeekPending = seekingStatusChange.seekInProgress
            }
        } catch (t: Throwable) {
            ErrorUtils.handleThrowableOnMainThread(t, javaClass)
        }
    }

    fun onError(error: FlipperError) {
        try {
            val currentConnectionType = connectionHelper.currentConnectionType
            // TODO : remove this check, as Skippy should filter out timeouts. Leaving it for this release as a precaution - JS
            if (ConnectionType.OFFLINE != currentConnectionType) {
                // Use Log as Skippy dumps can be rather large
                ErrorUtils.handleSilentExceptionWithLog(FlipperException(error.category, error.line, error.sourceFile), error.message)
            }

            val event = PlaybackErrorEvent(error.category, error.streamingProtocol.playbackProtocol(), error.cdn, error.format, error.bitrate, currentConnectionType, playerType)
            eventBus.publish(EventQueue.PLAYBACK_ERROR, event)
        } catch (t: Throwable) {
            ErrorUtils.handleThrowableOnMainThread(t, javaClass)
        }

    }

    private fun reportStateTransition(event: StateChange, urn: Urn, progress: Long) {
        with(PlaybackStateTransition(event.playbackState(), event.playStateReason(), urn, progress, event.duration, dateProvider)) {
            addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL, event.streamingProtocol.playbackProtocol().value)
            addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYER_TYPE, playerType.value)
            addExtraAttribute(PlaybackStateTransition.EXTRA_CONNECTION_TYPE, connectionHelper.currentConnectionType.value)
            addExtraAttribute(PlaybackStateTransition.EXTRA_NETWORK_AND_WAKE_LOCKS_ACTIVE, true)
            addExtraAttribute(PlaybackStateTransition.EXTRA_URI, currentStreamUrl)

            stateHandler.report(currentPlaybackItem, this)

            if (isPlaying) wakelockUtil.lock() else wakelockUtil.unlock()
            if (playbackHasStopped()) {
                currentPlaybackItem = null
                currentStreamUrl = null
            }
        }
    }

    companion object {
        private val TAG = "FlipperAdapter"
    }
}
