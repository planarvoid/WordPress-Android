package com.soundcloud.android.ads

import android.content.Context
import android.content.res.Resources
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.TextureView
import android.view.View
import com.soundcloud.android.R
import com.soundcloud.android.presentation.CellRenderer
import com.soundcloud.android.stream.StreamItem
import com.soundcloud.java.optional.Optional
import io.reactivex.subjects.PublishSubject

abstract class AdItemRenderer : CellRenderer<StreamItem> {

    protected var listener = Optional.absent<Listener>()
    val adItemClick: PublishSubject<AdItemResult> = PublishSubject.create()

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
            adItemClick.onNext(AdItemResult.WhyAdsClicked(view.context))
            listener.ifPresent {
                it.onWhyAdsClicked(view.context)
            }
        }
    }

    fun getClickthroughListener(adData: AdData): View.OnClickListener {
        return View.OnClickListener {
            adItemClick.onNext(AdItemResult.AdItemClick(adData))
            listener.ifPresent {
                it.onAdItemClicked(adData)
            }
        }
    }

    fun getSponsoredHeaderText(resources: Resources, itemType: String): SpannableString {
        val headerText = resources.getString(R.string.stream_sponsored_item, itemType)
        val spannedString = SpannableString(headerText)

        spannedString.setSpan(ForegroundColorSpan(resources.getColor(R.color.list_secondary)),
                              0,
                              headerText.length - itemType.length,
                              Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        return spannedString
    }
}

sealed class AdItemResult {
    data class AdItemClick(val adData: AdData) : AdItemResult()
    data class VideoTextureBind(val textureView: TextureView, val viewabilityLayer: View, val videoAd: VideoAd) : AdItemResult()
    data class VideoFullscreenClick(val videoAd: VideoAd) : AdItemResult()
    data class WhyAdsClicked(val context: Context) : AdItemResult()
}
