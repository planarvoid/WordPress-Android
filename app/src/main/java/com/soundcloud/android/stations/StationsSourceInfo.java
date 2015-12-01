package com.soundcloud.android.stations;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;

@AutoValue
public abstract class StationsSourceInfo {
    public abstract Urn getQueryUrn();
    public static StationsSourceInfo create(Urn queryUrn) {
        return new AutoValue_StationsSourceInfo(queryUrn);
    }
}
