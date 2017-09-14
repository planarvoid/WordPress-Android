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

    public static ChartDetails create(ChartType type, Urn genre) {
        return new AutoValue_ChartDetails(type, genre);
    }
}
