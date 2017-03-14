package com.soundcloud.android.upsell;

import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.DiscoveryGoUpsellConfig;

import javax.inject.Inject;

public class InlineUpsellOperations {

    private static final String ID_STREAM = "stream";
    private static final String ID_PLAYLIST = "playlist";
    private static final String ID_DISCOVERY = "discovery";

    private InlineUpsellStorage storage;
    private FeatureOperations featureOperations;
    private final DiscoveryGoUpsellConfig discoveryGoUpsellConfig;

    @Inject
    InlineUpsellOperations(InlineUpsellStorage storage, FeatureOperations featureOperations, DiscoveryGoUpsellConfig discoveryGoUpsellConfig) {
        this.storage = storage;
        this.featureOperations = featureOperations;
        this.discoveryGoUpsellConfig = discoveryGoUpsellConfig;
    }

    public boolean shouldDisplayInStream() {
        return canDisplay(ID_STREAM);
    }

    public void disableInStream() {
        storage.setUpsellDismissed(ID_STREAM);
    }

    public boolean shouldDisplayInDiscovery() {
        return canDisplay(ID_DISCOVERY) && discoveryGoUpsellConfig.isEnabled();
    }

    public void disableInDiscovery() {
        storage.setUpsellDismissed(ID_DISCOVERY);
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
