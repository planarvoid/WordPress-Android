package com.soundcloud.android.upsell;


import com.soundcloud.android.R;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.discovery.DiscoveryItem;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class DiscoveryUpsellItemRenderer extends UpsellItemRenderer<DiscoveryItem> {

    @Inject
    DiscoveryUpsellItemRenderer(FeatureOperations featureOperations) {
        super(featureOperations);
    }

    @Override
    public View createItemView(ViewGroup parent) {
        super.createItemView(parent);
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.upsell_card, parent, false);
    }

    @Override
    protected String getTitle(Context context) {
        return context.getString(R.string.upsell_discovery_upgrade_title);
    }

    @Override
    protected String getDescription(Context context) {
        return context.getString(R.string.upsell_discovery_upgrade_description);
    }

    @Override
    protected String getTrialActionButtonText(Context context, int trialDays) {
        return context.getString(R.string.upsell_discovery_button_trial, trialDays);
    }

    @Override
    protected String getUpsellActionButtonText(Context context) {
        return context.getString(R.string.upsell_discovery_button_get_it);
    }
}
