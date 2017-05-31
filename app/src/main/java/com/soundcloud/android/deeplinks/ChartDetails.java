package com.soundcloud.android.deeplinks;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class ChartDetails {
    public abstract ChartType type();
    public abstract Urn genre();
    public abstract ChartCategory category();
    public abstract Optional<String> title();

    public static ChartDetails create(ChartType type, Urn genre, ChartCategory category, Optional<String> title) {
        return new AutoValue_ChartDetails(type, genre, category, title);
    }
}
