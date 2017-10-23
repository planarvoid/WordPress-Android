package com.soundcloud.android.upsell

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.soundcloud.android.R
import com.soundcloud.android.configuration.FeatureOperations
import com.soundcloud.android.stream.StreamItem
import javax.inject.Inject

class StreamUpsellItemRenderer
@Inject
internal constructor(featureOperations: FeatureOperations) : UpsellItemRenderer<StreamItem>(featureOperations) {

    override fun createItemView(parent: ViewGroup): View {
        super.createItemView(parent)
        return LayoutInflater.from(parent.context).inflate(R.layout.upsell_card, parent, false)
    }

    override fun getTitle(context: Context): String = context.getString(R.string.upsell_stream_upgrade_title)

    override fun getDescription(context: Context): String = context.getString(R.string.upsell_stream_upgrade_description)

    override fun getTrialActionButtonText(context: Context, trialDays: Int): String = context.getString(R.string.conversion_buy_trial, trialDays)

}
