package com.soundcloud.android.configuration.experiments;

import javax.inject.Inject;

public class ShareButtonExperiment {
    public static final String NAME = "player_share_button";

    private static final String VARIATION_LIKED = "show_liked";
    private static final String VARIATION_ALWAYS = "show_always";
    private static final String VARIATION_NEVER = "show_never";

    private final ExperimentOperations experimentOperations;

    @Inject
    public ShareButtonExperiment(ExperimentOperations experimentOperations) {
        this.experimentOperations = experimentOperations;
    }

    public boolean isVisibleOnLoad(boolean isLiked) {
        switch (experimentOperations.getExperimentVariant(NAME)) {
            case VARIATION_LIKED:
                return isLiked;
            case VARIATION_ALWAYS:
                return true;
            case VARIATION_NEVER:
            default:
                return false;
        }
    }
}
