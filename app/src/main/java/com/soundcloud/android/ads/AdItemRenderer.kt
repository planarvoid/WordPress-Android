package com.soundcloud.android.ads

import android.content.Context
import android.view.TextureView
import android.view.View
import com.soundcloud.android.presentation.CellRenderer
import com.soundcloud.android.stream.StreamItem
import com.soundcloud.java.optional.Optional
import io.reactivex.subjects.PublishSubject

abstract class AdItemRenderer : CellRenderer<StreamItem> {

    protected var listener = Optional.absent<Listener>()
    val adItemCallback: PublishSubject<AdItemCallback> = PublishSubject.create()

    interface Listener {
        fun onAdItemClicked(adData: AdData)
        fun onVideoTextureBind(textureView: TextureView, viewabilityLayer: View, videoAd: VideoAd)
        fun onVideoFullscreenClicked(videoAd: VideoAd)
        fun onWhyAdsClicked(context: Context)
    }

    fun setListener(listener: Listener) {
        this.listener = Optional.of(listener)
    }

    fun bindWhyAdsListener(whyAdsButton: View) {
        whyAdsButton.setOnClickListener { view ->
            adItemCallback.onNext(AdItemCallback.WhyAdsClicked(view.context))
            listener.ifPresent {
                it.onWhyAdsClicked(view.context)
            }
        }
    }

    fun getClickthroughListener(adData: AdData): View.OnClickListener {
        return View.OnClickListener {
            adItemCallback.onNext(AdItemCallback.AdItemClick(adData))
            listener.ifPresent {
                it.onAdItemClicked(adData)
            }
        }
    }

}

sealed class AdItemCallback {
    data class AdItemClick(val adData: AdData) : AdItemCallback()
    data class VideoTextureBind(val textureView: TextureView, val viewabilityLayer: View, val videoAd: VideoAd) : AdItemCallback()
    data class VideoFullscreenClick(val videoAd: VideoAd) : AdItemCallback()
    data class WhyAdsClicked(val context: Context) : AdItemCallback()
}
