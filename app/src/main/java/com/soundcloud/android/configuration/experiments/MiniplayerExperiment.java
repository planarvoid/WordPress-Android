package com.soundcloud.android.configuration.experiments;

import static com.soundcloud.android.configuration.experiments.ExperimentOperations.LISTENING_LAYER;
import static java.util.Arrays.asList;

import com.soundcloud.android.playback.MiniplayerStorage;
import com.soundcloud.android.playback.PlaySessionStateProvider;
import com.soundcloud.groupie.ActiveExperiment;
import com.soundcloud.groupie.ExperimentConfiguration;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

@ActiveExperiment
public class MiniplayerExperiment {
    static final long PLAY_SESSION_LENGTH = TimeUnit.HOURS.toMillis(1);

    private static final String NAME = "miniplayer_abc_test";
    static final String VARIANT_CONTROL = "statusquo";
    static final String VARIANT_INVERSE = "inverse";
    static final String VARIANT_HYBRID = "hybrid";

    public static final ExperimentConfiguration CONFIGURATION = ExperimentConfiguration
            .fromName(LISTENING_LAYER, NAME, asList(VARIANT_CONTROL, VARIANT_INVERSE, VARIANT_HYBRID));

    private final ExperimentOperations experimentOperations;
    private final MiniplayerStorage miniplayerStorage;
    private final PlaySessionStateProvider playSessionStateProvider;

    @Inject
    MiniplayerExperiment(ExperimentOperations experimentOperations, MiniplayerStorage miniplayerStorage, PlaySessionStateProvider playSessionStateProvider) {
        this.experimentOperations = experimentOperations;
        this.miniplayerStorage = miniplayerStorage;
        this.playSessionStateProvider = playSessionStateProvider;
    }

    public boolean canExpandPlayer() {
        switch (getVariant()) {
            case VARIANT_INVERSE:
                return false;
            case VARIANT_HYBRID:
                return !minimizedWithinPlaySessionPeriod();
            case VARIANT_CONTROL:
            default:
                return true;
        }
    }

    private boolean minimizedWithinPlaySessionPeriod() {
        boolean playedRecently = playSessionStateProvider.getMillisSinceLastPlaySession() < PLAY_SESSION_LENGTH;
        boolean hasMinimizedPlayer = miniplayerStorage.hasMinimizedPlayerManually();
        return hasMinimizedPlayer && playedRecently;
    }

    private String getVariant() {
        return experimentOperations.getExperimentVariant(CONFIGURATION);
    }
}
