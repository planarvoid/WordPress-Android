package com.soundcloud.android.upsell;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.playlists.PlaylistDetailItem;

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

}
