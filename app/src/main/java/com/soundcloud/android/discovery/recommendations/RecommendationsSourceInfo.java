package com.soundcloud.android.discovery.recommendations;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.strings.Strings;

/**
 * Tracking specs:
 * https://github.com/soundcloud/eventgateway-schemas/blob/v0/doc/personalized-recommender-tracking.md
 */
@AutoValue
public abstract class RecommendationsSourceInfo {

    private static final String RECOMMENDATIONS_SOURCE = "personal-recommended";

    public abstract int getQueryPosition();

    public abstract Urn getQueryUrn();

    public static RecommendationsSourceInfo create(int queryPosition, Urn queryUrn) {
        return new AutoValue_RecommendationsSourceInfo(queryPosition, queryUrn);
    }

    public String getSource() {
        return RECOMMENDATIONS_SOURCE;
    }

    public String getSourceVersion() {
        return Strings.EMPTY;
    }
}
