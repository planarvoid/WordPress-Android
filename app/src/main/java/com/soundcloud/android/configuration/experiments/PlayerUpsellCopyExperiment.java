package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ActiveExperiments.LISTENING_LAYER;

import com.soundcloud.android.R;

import android.support.annotation.StringRes;

import javax.inject.Inject;
import java.util.Arrays;

public class PlayerUpsellCopyExperiment {

    private static final String NAME = "android_scgo_player_cta";
    private static final String VARIATION_HEAR_FULL = "hear_full_track";
    private static final String VARIATION_UNLOCK_PREVIEW = "unlock_preview";
    private static final String VARIATION_UNLOCK_FULL = "unlock_full_track";

    static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER, NAME, Arrays.asList(VARIATION_HEAR_FULL, VARIATION_UNLOCK_PREVIEW, VARIATION_UNLOCK_FULL));

    private final ExperimentOperations experimentOperations;

    @Inject
    public PlayerUpsellCopyExperiment(ExperimentOperations experimentOperations) {
        this.experimentOperations = experimentOperations;
    }

    @StringRes
    public int getUpsellCtaId() {
        switch (experimentOperations.getExperimentVariant(CONFIGURATION)) {
            case VARIATION_UNLOCK_PREVIEW:
                return R.string.playback_upsell_button_2;
            case VARIATION_UNLOCK_FULL:
                return R.string.playback_upsell_button_3;
            case VARIATION_HEAR_FULL:
            default:
                return R.string.playback_upsell_button_1;
        }
    }

}
