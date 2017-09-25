package com.soundcloud.android.presentation

data class ItemMenuOptions(val displayGoToArtistProfile: Boolean = true) {
    companion object {
        fun forGoToProfileDisabled() = ItemMenuOptions(false)
        fun forGoToProfileEnabled() = ItemMenuOptions(true)
        fun createDefault() = ItemMenuOptions(true)
    }
}
