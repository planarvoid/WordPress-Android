package com.soundcloud.android.discovery;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

@AutoValue
public abstract class ChartSourceInfo {

    public abstract int getQueryPosition();

    public abstract Urn getQueryUrn();

    public static ChartSourceInfo create(int queryPosition, Urn queryUrn) {
        return new AutoValue_ChartSourceInfo(queryPosition, queryUrn);
    }
}
