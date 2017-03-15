package com.soundcloud.android.upsell;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.playlists.PlaylistDetailItem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class PlaylistUpsellItemRenderer extends UpsellItemRenderer<PlaylistDetailItem> {

    @Inject
    public PlaylistUpsellItemRenderer(FeatureOperations featureOperations) {
        super(featureOperations);
    }

    @Override
    public View createItemView(ViewGroup parent) {
        super.createItemView(parent);
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_upsell_item, parent, false);
    }

    @Override
    protected String getTitle(Context context) {
        return context.getString(R.string.upsell_playlist_upgrade_title);
    }

    @Override
    protected String getDescription(Context context) {
        return context.getString(R.string.upsell_playlist_upgrade_description);
    }

    @Override
    protected String getTrialActionButtonText(Context context, int trialDays) {
        return context.getString(R.string.conversion_buy_trial, trialDays);
    }

}
