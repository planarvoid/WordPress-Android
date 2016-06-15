package com.soundcloud.android.discovery;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;

import java.util.Collections;
import java.util.List;

@AutoValue
public abstract class Chart {

    static Chart create(Long localId,
                        ChartType type,
                        ChartCategory category,
                        String displayName,
                        Urn genre,
                        ChartBucketType bucketType) {
        return create(localId, type, category, displayName, genre, bucketType, Collections.<ChartTrack>emptyList());
    }

    public static Chart create(Long localId,
                               ChartType type,
                               ChartCategory category,
                               String displayName,
                               Urn genre,
                               ChartBucketType bucketType,
                               List<ChartTrack> chartTracks) {
        return new AutoValue_Chart(localId, type, category, displayName, genre, chartTracks, bucketType);
    }

    Chart copyWithTracks(List<ChartTrack> tracks) {
        return create(localId(), type(), category(), displayName(), genre(), bucketType(), tracks);
    }

    public abstract Long localId();

    public abstract ChartType type();

    public abstract ChartCategory category();

    public abstract String displayName();

    public abstract Urn genre();

    public abstract List<ChartTrack> tracks();

    public abstract ChartBucketType bucketType();

}
