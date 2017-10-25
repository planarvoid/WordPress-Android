package com.soundcloud.android.image

import android.view.View
import android.widget.ImageView
import com.soundcloud.android.utils.Log
import com.soundcloud.android.utils.images.ImageUtils
import io.reactivex.functions.Consumer
import java.io.FileNotFoundException

internal class FallbackImageConsumer(val notFoundUris: MutableSet<String>) : Consumer<LoadingState> {
    override fun accept(loadingState: LoadingState) {
        when (loadingState) {
            is LoadingState.Complete -> onLoadingComplete(loadingState)
            is LoadingState.Fail -> onLoadingFailed(loadingState)
        }
    }

    fun onLoadingComplete(loadingState: LoadingState.Complete) {
        if (loadingState.loadedImage == null) {
            animatePlaceholder(loadingState.view)
        }
    }

    fun onLoadingFailed(loadingState: LoadingState.Fail) {
        if (loadingState.cause is FileNotFoundException) {
            loadingState.imageUrl?.let { notFoundUris.add(it) }
        } else {
            Log.e(TAG, "Failed loading " + loadingState.imageUrl + "; reason: " + loadingState.cause.message)
        }
        animatePlaceholder(loadingState.view)
    }

    private fun animatePlaceholder(view: View?) {
        if (view != null && view is ImageView) {
            (view.drawable as? OneShotTransitionDrawable)?.startTransition(ImageUtils.DEFAULT_TRANSITION_DURATION)
        }
    }

    companion object {
        private val TAG = "ImageLoader"
    }
}
