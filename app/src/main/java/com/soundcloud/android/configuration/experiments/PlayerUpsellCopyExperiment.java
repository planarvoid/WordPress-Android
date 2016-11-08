package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ActiveExperiments.LISTENING_LAYER;

import com.soundcloud.android.R;

import android.support.annotation.StringRes;

import javax.inject.Inject;
import java.util.Arrays;

public class PlayerUpsellCopyExperiment {

    private static final String NAME = "android_scgo_player_cta2";
    private static final String VARIATION_CONTENT = "content_only";
    private static final String VARIATION_CONTENT_ADS_OFFLINE = "content_ads_and_offline";

    static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER, NAME, Arrays.asList(VARIATION_CONTENT, VARIATION_CONTENT_ADS_OFFLINE));

    private final ExperimentOperations experimentOperations;

    @Inject
    PlayerUpsellCopyExperiment(ExperimentOperations experimentOperations) {
        this.experimentOperations = experimentOperations;
    }

    @StringRes
    public int getUpsellCtaId() {
        switch (experimentOperations.getExperimentVariant(CONFIGURATION)) {
            case VARIATION_CONTENT:
                return R.string.playback_upsell_1;
            case VARIATION_CONTENT_ADS_OFFLINE:
                return R.string.playback_upsell_2;
            default:
                return R.string.playback_upsell_1;
        }
    }

}
