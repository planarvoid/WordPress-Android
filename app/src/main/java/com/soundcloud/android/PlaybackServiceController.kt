package com.soundcloud.android

import android.content.Context
import android.content.Intent
import com.soundcloud.android.playback.PlaybackItem
import com.soundcloud.android.playback.PlaybackService
import com.soundcloud.android.playback.PreloadItem
import com.soundcloud.android.utils.OpenForTesting
import javax.inject.Inject

@OpenForTesting
class PlaybackServiceController
@Inject
constructor(private val context: Context) {

    fun stopPlaybackService() = playerAction(PlaybackService.Action.STOP)

    fun resetPlaybackService() = playerAction(PlaybackService.Action.RESET_ALL)

    fun togglePlayback() = playerAction(PlaybackService.Action.TOGGLE_PLAYBACK)

    fun resume() = playerAction(PlaybackService.Action.RESUME)

    fun pause() = playerAction(PlaybackService.Action.PAUSE)

    fun fadeAndPause() = playerAction(PlaybackService.Action.FADE_AND_PAUSE)

    fun play(playbackItem: PlaybackItem) = with(createExplicitServiceIntent(PlaybackService.Action.PLAY)) {
        putExtra(PlaybackService.ActionExtras.PLAYBACK_ITEM, playbackItem)
        context.startService(this)
    }

    fun preload(preloadItem: PreloadItem) = with(createExplicitServiceIntent(PlaybackService.Action.PRELOAD)) {
        putExtra(PlaybackService.ActionExtras.PRELOAD_ITEM, preloadItem)
        context.startService(this)
    }

    fun seek(position: Long) = with(createExplicitServiceIntent(PlaybackService.Action.SEEK)) {
        putExtra(PlaybackService.ActionExtras.POSITION, position)
        context.startService(this)
    }

    private fun playerAction(action: String) = context.startService(createExplicitServiceIntent(action))

    private fun createExplicitServiceIntent(action: String) = Intent(context, PlaybackService::class.java).setAction(action)
}
