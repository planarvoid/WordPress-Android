package com.soundcloud.android.discovery;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.Collections;
import java.util.List;

@AutoValue
public abstract class Chart {

    static Chart create(Long localId,
                        ChartType type,
                        ChartCategory category,
                        String title,
                        String page,
                        Optional<Urn> genre) {
        return create(localId, type, category, title, page, genre, Collections.<ChartTrack>emptyList());
    }

    public static Chart create(Long localId,
                               ChartType type,
                               ChartCategory category,
                               String title,
                               String page,
                               Optional<Urn> genre,
                               List<ChartTrack> chartTracks) {
        return new AutoValue_Chart(localId, type, category, title, page, genre, chartTracks);
    }

    Chart copyWithTracks(List<ChartTrack> tracks) {
        return create(localId(), type(), category(), title(), page(), genre(), tracks);
    }

    public abstract Long localId();

    public abstract ChartType type();

    public abstract ChartCategory category();

    public abstract String title();

    public abstract String page();

    public abstract Optional<Urn> genre();

    public abstract List<ChartTrack> chartTracks();

}
