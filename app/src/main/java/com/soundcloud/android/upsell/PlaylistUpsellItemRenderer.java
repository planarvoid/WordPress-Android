package com.soundcloud.android.upsell;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.playlists.PlaylistDetailItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;

public class PlaylistUpsellItemRenderer extends UpsellItemRenderer<PlaylistDetailItem> {

    private final FeatureFlags flags;

    @Inject
    public PlaylistUpsellItemRenderer(FeatureOperations featureOperations, FeatureFlags flags) {
        super(featureOperations, flags);
        this.flags = flags;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        super.createItemView(parent);
        final View layout = LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_upsell_item, parent, false);
        ((TextView) layout.findViewById(R.id.title)).setText(flags.isEnabled(Flag.MID_TIER_ROLLOUT)
                                                             ? R.string.upsell_playlist_upgrade_title
                                                             : R.string.upsell_playlist_upgrade_title_legacy);
        return layout;
    }

}
