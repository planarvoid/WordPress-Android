package com.soundcloud.android.playback.flipper

import android.support.annotation.UiThread
import com.soundcloud.android.accounts.AccountOperations
import com.soundcloud.android.crypto.CryptoOperations
import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.events.PlaybackErrorEvent
import com.soundcloud.android.events.PlayerType
import com.soundcloud.android.model.Urn
import com.soundcloud.android.playback.AudioPerformanceEvent
import com.soundcloud.android.playback.HlsStreamUrlBuilder
import com.soundcloud.android.playback.PlaybackItem
import com.soundcloud.android.playback.PlaybackStateTransition
import com.soundcloud.android.playback.PlaybackType
import com.soundcloud.android.playback.Player
import com.soundcloud.android.playback.PreloadItem
import com.soundcloud.android.utils.ConnectionHelper
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
                     private val callbackHandler: FlipperCallbackHandler,
                     private val eventBus: EventBusV2,
                     private val cryptoOperations: CryptoOperations,
                     private val performanceReporter: FlipperPerformanceReporter) : Player, FlipperCallbacks {

    private val flipperWrapper: FlipperWrapper = flipperWrapperFactory.create(this)

    @Volatile private var currentStreamUrl: String? = null
    private var currentPlaybackItem: PlaybackItem? = null
    private var playerListener: Player.PlayerListener? = null

    // Flipper may send past progress events when seeking, leading to UI glitches.
    // This boolean helps us to workaround this.
    private var isSeekPending: Boolean = false
    private var progress: Long = 0

    override fun preload(preloadItem: PreloadItem) = flipperWrapper.prefetch(hlsStreamUrlBuilder.buildStreamUrl(preloadItem))

    override fun play(playbackItem: PlaybackItem) {
        if (!accountOperations.isUserLoggedIn) throw IllegalStateException("Cannot play a track if no soundcloud account exists")

        ErrorUtils.log(android.util.Log.DEBUG, TAG, "play(): ${playbackItem.urn} in duration ${playbackItem.duration}]")
        callbackHandler.removeCallbacksAndMessages(0)
        currentPlaybackItem = playbackItem
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
        val streamUrl = currentStreamUrl
        if (streamUrl != null) {
            when (playbackItem.playbackType) {
                PlaybackType.AUDIO_DEFAULT, PlaybackType.AUDIO_SNIPPET -> flipperWrapper.open(streamUrl, playbackItem.startPosition)
                PlaybackType.AUDIO_OFFLINE -> {
                    val deviceSecret = cryptoOperations.checkAndGetDeviceKey()
                    flipperWrapper.openEncrypted(streamUrl, deviceSecret.key, deviceSecret.initVector, playbackItem.startPosition)
                }
                else -> throw IllegalStateException("Flipper does not accept playback type: ${playbackItem.playbackType}")
            }
        }
    }

    override fun resume(playbackItem: PlaybackItem) {
        Log.d(TAG, "resume() called with: playbackItem = [$playbackItem, ${playbackItem.urn} in duration ${playbackItem.duration}]")
        currentPlaybackItem = playbackItem
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

    override fun getVolume() = flipperWrapper.volume.toFloat()

    override fun setVolume(level: Float) {
        flipperWrapper.volume = level.toDouble()
    }

    override fun stop() = flipperWrapper.pause()

    override fun stopForTrackTransition() = stop()

    override fun destroy() = flipperWrapper.destroy()

    override fun setListener(playerListener: Player.PlayerListener) {
        this.playerListener = playerListener
    }

    override fun isSeekable() = true

    override fun getPlayerType() = PlayerType.FLIPPER

    override fun onProgressChanged(event: ProgressChange) {
        callbackThread {
            try {
                if (isCurrentStreamUrl(event.uri) && !isSeekPending) {
                    progress = event.position
                    playerListener?.onProgressEvent(event.position, event.duration)
                }
            } catch (t: Throwable) {
                ErrorUtils.handleThrowableOnMainThread(t, javaClass)
            }
        }
    }

    override fun onPerformanceEvent(event: AudioPerformanceEvent) {
        callbackThread {
            try {
                currentPlaybackItem?.let {
                    performanceReporter.report(it.playbackType, event, playerType, accountOperations.loggedInUserUrn, connectionHelper.currentConnectionType)
                }
            } catch (t: Throwable) {
                ErrorUtils.handleThrowableOnMainThread(t, javaClass)
            }
        }
    }

    private fun isCurrentStreamUrl(uri: String) = uri == currentStreamUrl

    override fun onStateChanged(event: StateChange) {
        callbackThread {
            ErrorUtils.log(android.util.Log.INFO, TAG, "onStateChanged() called in ${event.state} with: event = [$event]")
            handleStateChanged(event)
        }
    }

    override fun onBufferingChanged(event: StateChange) {
        callbackThread {
            Log.i(TAG, "onBufferingChanged() called in ${event.state} with: event = [$event]")
            handleStateChanged(event)
        }
    }

    @UiThread
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

    override fun onSeekingStatusChanged(seekingStatusChange: SeekingStatusChange) {
        callbackThread {
            try {
                if (isCurrentStreamUrl(seekingStatusChange.uri)) {
                    isSeekPending = seekingStatusChange.seekInProgress
                }
            } catch (t: Throwable) {
                ErrorUtils.handleThrowableOnMainThread(t, javaClass)
            }
        }
    }

    override fun onError(error: FlipperError) {
        callbackThread {
            try {
                if (!error.isNetworkError()) {
                    // Don't log network errors to Fabric as they are very common and noisy
                    ErrorUtils.handleSilentExceptionWithLog(FlipperException(error.category, error.line, error.sourceFile), error.message)
                }

                val event = PlaybackErrorEvent(error.category, error.streamingProtocol.playbackProtocol(), error.cdn, error.format, error.bitrate, playerType)
                eventBus.publish(EventQueue.PLAYBACK_ERROR, event)
            } catch (t: Throwable) {
                ErrorUtils.handleThrowableOnMainThread(t, javaClass)
            }
        }
    }

    @UiThread
    private fun reportStateTransition(event: StateChange, urn: Urn, progress: Long) {
        with(PlaybackStateTransition(event.playbackState(), event.playStateReason(), urn, progress, event.duration)) {
            addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYBACK_PROTOCOL, event.streamingProtocol.playbackProtocol().value)
            addExtraAttribute(PlaybackStateTransition.EXTRA_PLAYER_TYPE, playerType.value)
            addExtraAttribute(PlaybackStateTransition.EXTRA_CONNECTION_TYPE, connectionHelper.currentConnectionType.value)
            addExtraAttribute(PlaybackStateTransition.EXTRA_NETWORK_AND_WAKE_LOCKS_ACTIVE, true)
            addExtraAttribute(PlaybackStateTransition.EXTRA_URI, currentStreamUrl)

            playerListener?.onPlaystateChanged(this)

            if (isPlaying) wakelockUtil.lock() else wakelockUtil.unlock()
            if (playbackHasStopped()) {
                currentPlaybackItem = null
                currentStreamUrl = null
            }
        }
    }

    private fun callbackThread(function: () -> Unit) = callbackHandler.post(function)

    companion object {
        private val TAG = "FlipperAdapter"
    }
}
