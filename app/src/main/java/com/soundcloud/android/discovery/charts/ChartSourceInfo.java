package com.soundcloud.android.discovery.charts;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.model.Urn;

@AutoValue
public abstract class ChartSourceInfo {

    public abstract int getQueryPosition();

    public abstract Urn getQueryUrn();

    public abstract ChartType getChartType();

    public abstract ChartCategory getChartCategory();

    public abstract Urn getGenre();

    public static ChartSourceInfo create(int queryPosition,
                                         Urn queryUrn,
                                         ChartType chartType,
                                         ChartCategory chartCategory, Urn genre) {
        return new AutoValue_ChartSourceInfo(queryPosition, queryUrn, chartType, chartCategory, genre);
    }
}
