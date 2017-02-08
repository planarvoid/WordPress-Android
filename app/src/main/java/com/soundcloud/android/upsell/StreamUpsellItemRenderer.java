package com.soundcloud.android.upsell;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.stream.StreamItem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;

public class StreamUpsellItemRenderer extends UpsellItemRenderer<StreamItem> {

    private final FeatureFlags flags;

    @Inject
    StreamUpsellItemRenderer(FeatureOperations featureOperations, FeatureFlags flags) {
        super(featureOperations, flags);
        this.flags = flags;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        super.createItemView(parent);
        final View layout = LayoutInflater.from(parent.getContext()).inflate(R.layout.stream_upsell_card, parent, false);
        ((TextView) layout.findViewById(R.id.title)).setText(flags.isEnabled(Flag.MID_TIER)
                                                             ? R.string.upsell_stream_upgrade_title
                                                             : R.string.upsell_stream_upgrade_title_legacy);
        return layout;
    }
}
