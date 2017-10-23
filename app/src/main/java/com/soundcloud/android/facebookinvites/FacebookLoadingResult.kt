package com.soundcloud.android.facebookinvites

sealed class FacebookLoadingResult {
    data class Load(val hasPictures: Boolean) : FacebookLoadingResult()
    data class Dismiss(val position: Int) : FacebookLoadingResult()
    data class Click(val position: Int) : FacebookLoadingResult()
}
