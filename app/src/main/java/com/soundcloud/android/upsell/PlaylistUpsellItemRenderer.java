package com.soundcloud.android.upsell;

import com.soundcloud.android.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class PlaylistUpsellItemRenderer extends UpsellItemRenderer {

    @Inject
    public PlaylistUpsellItemRenderer() {}

    @Override
    public View createItemView(ViewGroup parent) {
        super.createItemView(parent);
        return LayoutInflater.from(parent.getContext())
                .inflate(R.layout.playlist_upsell_item, parent, false);
    }

}
