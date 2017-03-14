package com.soundcloud.android.configuration.experiments;

import static java.util.Arrays.asList;

import com.soundcloud.android.properties.ApplicationProperties;

import javax.inject.Inject;
import java.util.List;

public class ActiveExperiments {
    static final String LISTENING_LAYER = "android_listening";

    public static final List<ExperimentConfiguration> ACTIVE_EXPERIMENTS = asList(OpusExperiment.CONFIGURATION,
                                                                                  SuggestedCreatorsExperiment.CONFIGURATION,
                                                                                  PlaylistDiscoveryConfig.CONFIGURATION,
                                                                                  AutocompleteConfig.CONFIGURATION,
                                                                                  MiniplayerExperiment.CONFIGURATION,
                                                                                  FlipperConfiguration.CONFIGURATION,
                                                                                  FlipperPreloadConfiguration.CONFIGURATION,
                                                                                  SuggestedStationsExperiment.CONFIGURATION,
                                                                                  DynamicLinkSharingConfig.CONFIGURATION,
                                                                                  OtherPlaylistsByUserConfig.CONFIGURATION,
                                                                                  SearchPlayRelatedTracksConfig.CONFIGURATION,
                                                                                  DiscoveryGoUpsellConfig.CONFIGURATION,
                                                                                  NewForYouConfig.CONFIGURATION);

    private static final String[] ACTIVE_LAYERS = {LISTENING_LAYER};

    private final ApplicationProperties applicationProperties;

    @Inject
    ActiveExperiments(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    String[] getRequestLayers() {
        return ACTIVE_LAYERS;
    }

    public boolean isActive(Layer layer) {
        if (!applicationProperties.isDevelopmentMode()) {
            for (ExperimentConfiguration experiment : ACTIVE_EXPERIMENTS) {
                if (experiment.matches(layer)) {
                    return true;
                }
            }
        }
        return false;
    }
}
