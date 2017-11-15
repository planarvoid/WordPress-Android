package com.soundcloud.android.events

sealed class PlayerType(val value: String) {
    object MediaPlayer : PlayerType("MediaPlayer")
    object Skippy : PlayerType("Skippy")
    object Flipper : PlayerType("Flipper")
}
