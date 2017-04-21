package com.soundcloud.android.olddiscovery;

import com.soundcloud.android.upsell.InlineUpsellOperations;

import javax.inject.Inject;

public class OldDiscoveryOperations {


    private final InlineUpsellOperations inlineUpsellOperations;

    @Inject
    OldDiscoveryOperations(InlineUpsellOperations inlineUpsellOperations) {
        this.inlineUpsellOperations = inlineUpsellOperations;
    }

    public void clearData() {
        inlineUpsellOperations.clearData();
    }

    public void disableUpsell() {
        inlineUpsellOperations.disableInDiscovery();
    }

}
