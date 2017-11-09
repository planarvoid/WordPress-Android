package com.soundcloud.android.facebookinvites

import com.soundcloud.android.stream.StreamItem

sealed class FacebookNotificationCallback<T : StreamItem> {
    data class Load<T : StreamItem>(val hasPictures: Boolean) : FacebookNotificationCallback<T>()
    data class Dismiss<T : StreamItem>(val position: Int, val streamItem: T) : FacebookNotificationCallback<T>()
    data class Click<T : StreamItem>(val position: Int, val streamItem: T) : FacebookNotificationCallback<T>()
}
