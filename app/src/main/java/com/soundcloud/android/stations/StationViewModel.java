package com.soundcloud.android.stations;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class StationViewModel {

    public static StationViewModel create(StationRecord stationRecord, boolean isPlaying) {
        return new AutoValue_StationViewModel(stationRecord, isPlaying);
    }

    abstract StationRecord getStation();

    abstract boolean isPlaying();
}
