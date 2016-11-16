package com.soundcloud.android.discovery.charts;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;

@AutoValue
public abstract class ChartSourceInfo {

    public abstract ChartType getChartType();

    public abstract Urn getGenre();

    public static ChartSourceInfo create(ChartType chartType, Urn genre) {
        return new AutoValue_ChartSourceInfo(chartType, genre);
    }
}
