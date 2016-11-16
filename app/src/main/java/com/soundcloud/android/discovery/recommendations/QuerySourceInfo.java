package com.soundcloud.android.discovery.recommendations;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

/**
 * Tracking specs:
 * https://github.com/soundcloud/eventgateway-schemas/blob/v0/doc/personalized-recommender-tracking.md
 */
@AutoValue
public abstract class QuerySourceInfo {

    public abstract int getQueryPosition();

    public abstract Urn getQueryUrn();

    public static QuerySourceInfo create(int queryPosition, Urn queryUrn) {
        return new AutoValue_QuerySourceInfo(queryPosition, queryUrn);
    }
}
