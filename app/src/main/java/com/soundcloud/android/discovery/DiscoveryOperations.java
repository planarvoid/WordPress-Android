package com.soundcloud.android.discovery;

import com.soundcloud.android.upsell.InlineUpsellOperations;

import javax.inject.Inject;

public class DiscoveryOperations {


    private final InlineUpsellOperations inlineUpsellOperations;

    @Inject
    DiscoveryOperations(InlineUpsellOperations inlineUpsellOperations) {
        this.inlineUpsellOperations = inlineUpsellOperations;
    }

    public void clearData() {
        inlineUpsellOperations.clearData();
    }

    public void disableUpsell() {
        inlineUpsellOperations.disableInDiscovery();
    }

}
