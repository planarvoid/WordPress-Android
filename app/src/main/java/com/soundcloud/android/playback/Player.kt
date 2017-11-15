package com.soundcloud.android.playback

interface Player {
    fun preload(preloadItem: PreloadItem)
    fun play(playbackItem: PlaybackItem)
    fun resume(playbackItem: PlaybackItem)
    fun pause()
    fun seek(ms: Long)
    fun stop()
    fun destroy()
    fun getProgress(): Long
    fun getPlayerType(): String
    fun getVolume(): Float
    fun setVolume(volume: Float)
    fun setListener(playerListener: PlayerListener)

    interface PlayerListener {
        fun onPlaystateChanged(stateTransition: PlaybackStateTransition)
        fun onProgressEvent(progress: Long, duration: Long)
    }
}
