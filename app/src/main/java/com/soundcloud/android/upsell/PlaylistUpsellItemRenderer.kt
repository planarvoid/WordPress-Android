package com.soundcloud.android.upsell

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.soundcloud.android.R
import com.soundcloud.android.configuration.FeatureOperations
import com.soundcloud.android.playlists.PlaylistDetailUpsellItem
import javax.inject.Inject

class PlaylistUpsellItemRenderer
@Inject
constructor(featureOperations: FeatureOperations) : UpsellItemRenderer<PlaylistDetailUpsellItem>(featureOperations) {

    override fun createItemView(parent: ViewGroup): View {
        super.createItemView(parent)
        return LayoutInflater.from(parent.context).inflate(R.layout.playlist_upsell_item, parent, false)
    }

    override fun getTitle(context: Context): String = context.getString(R.string.upsell_playlist_upgrade_title)

    override fun getDescription(context: Context): String = context.getString(R.string.upsell_playlist_upgrade_description)

    override fun getTrialActionButtonText(context: Context, trialDays: Int): String = context.getString(R.string.conversion_buy_trial, trialDays)

}
