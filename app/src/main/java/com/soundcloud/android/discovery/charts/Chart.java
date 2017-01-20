package com.soundcloud.android.discovery.charts;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackArtwork;

import java.util.Collections;
import java.util.List;

@AutoValue
public abstract class Chart {
    public static final Urn GLOBAL_GENRE = new Urn("soundcloud:genres:all-music");

    static Chart create(Long localId,
                        ChartType type,
                        ChartCategory category,
                        String displayName,
                        Urn genre,
                        ChartBucketType bucketType) {
        return create(localId, type, category, displayName, genre, bucketType, Collections.emptyList());
    }

    public static Chart create(Long localId,
                               ChartType type,
                               ChartCategory category,
                               String displayName,
                               Urn genre,
                               ChartBucketType bucketType,
                               List<TrackArtwork> trackArtworks) {
        return new AutoValue_Chart(localId, type, category, displayName, genre, trackArtworks, bucketType);
    }

    Chart copyWithTrackArtworks(List<TrackArtwork> trackArtworks) {
        return create(localId(), type(), category(), displayName(), genre(), bucketType(), trackArtworks);
    }

    public abstract Long localId();

    public abstract ChartType type();

    public abstract ChartCategory category();

    public abstract String displayName();

    public abstract Urn genre();

    public abstract List<TrackArtwork> trackArtworks();

    public abstract ChartBucketType bucketType();

}
