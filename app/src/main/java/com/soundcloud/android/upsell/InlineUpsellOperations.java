package com.soundcloud.android.upsell;

import com.soundcloud.android.configuration.FeatureOperations;

import javax.inject.Inject;

public class InlineUpsellOperations {

    private static final String ID_STREAM = "stream";
    private static final String ID_PLAYLIST = "playlist";

    private InlineUpsellStorage storage;
    private FeatureOperations featureOperations;

    @Inject
    InlineUpsellOperations(InlineUpsellStorage storage, FeatureOperations featureOperations) {
        this.storage = storage;
        this.featureOperations = featureOperations;
    }

    public boolean shouldDisplayInStream() {
        return canDisplay(ID_STREAM);
    }

    public void disableInStream() {
        storage.setUpsellDismissed(ID_STREAM);
    }

    public boolean shouldDisplayInPlaylist() {
        return canDisplay(ID_PLAYLIST);
    }

    public void disableInPlaylist() {
        storage.setUpsellDismissed(ID_PLAYLIST);
    }

    private boolean canDisplay(String id) {
        return featureOperations.upsellHighTier() && storage.canDisplayUpsell(id);
    }

    public void clearData() {
        storage.clearData();
    }
}
