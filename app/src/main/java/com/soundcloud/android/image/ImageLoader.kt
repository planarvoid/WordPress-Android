package com.soundcloud.android.image

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.AbsListView
import android.widget.ImageView
import io.reactivex.Observable

interface ImageLoader {
    fun displayImage(imageUrl: String?,
                     imageView: ImageView,
                     circular: Boolean = false,
                     placeholderDrawable: Drawable? = null,
                     displayType: DisplayType = DisplayType.DEFAULT,
                     apiImageSize: ApiImageSize = ApiImageSize.Unknown,
                     isHighPriority: Boolean = false): Observable<LoadingState>

    fun clearDiskCache()
    fun loadImage(imageUrl: String, loadType: LoadType = LoadType.NONE): Observable<LoadingState>
    fun getCachedBitmap(imageUrl: String, targetWidth: Int, targetHeight: Int): Bitmap?
    fun createScrollPauseListener(pauseOnScroll: Boolean, pauseOnFling: Boolean, customListener: AbsListView.OnScrollListener?): AbsListView.OnScrollListener? = customListener
    fun resume()
    fun pause()
}

enum class DisplayType {
    PLACEHOLDER, PLAYER, DEFAULT, AD, FULL_IMAGE_DIALOG, STREAM_AD_IMAGE
}

enum class LoadType {
    AD, PREFETCH, NONE
}

sealed class LoadingState {
    data class Start(val imageUrl: String?, val view: View?) : LoadingState()
    data class Cancel(val imageUrl: String?, val view: View?) : LoadingState()
    data class Fail(val imageUrl: String?, val view: View?, val cause: Throwable) : LoadingState()
    data class Complete(val imageUrl: String?, val view: View?, val loadedImage: Bitmap?) : LoadingState()
}
