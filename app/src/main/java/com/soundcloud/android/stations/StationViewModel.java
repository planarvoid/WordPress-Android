package com.soundcloud.android.stations;

import com.soundcloud.java.objects.MoreObjects;

public class StationViewModel {
    private final StationRecord station;
    private boolean isPlaying;

    public StationViewModel(StationRecord station, boolean isPlaying) {
        this.station = station;
        this.isPlaying = isPlaying;
    }

    StationRecord getStation() {
        return station;
    }

    boolean isPlaying() {
        return isPlaying;
    }

    void setIsPlaying(boolean isPlaying) {
        this.isPlaying = isPlaying;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StationViewModel that = (StationViewModel) o;
        return isPlaying == that.isPlaying &&
                MoreObjects.equal(station, that.station);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(station, isPlaying);
    }
}
