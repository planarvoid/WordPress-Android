package com.soundcloud.android.stream;

import com.soundcloud.android.configuration.FeatureOperations;

import javax.inject.Inject;

class UpsellOperations {

    private UpsellStorage storage;
    private FeatureOperations featureOperations;

    @Inject
    UpsellOperations(UpsellStorage storage, FeatureOperations featureOperations) {
        this.storage = storage;
        this.featureOperations = featureOperations;
    }

    public boolean canDisplayUpsellInStream() {
        return featureOperations.upsellMidTier() && !storage.wasUpsellDismissed();
    }

    public void disableUpsell() {
        storage.setUpsellDismissed();
    }

    public void clearData() {
        storage.clearData();
    }
}
