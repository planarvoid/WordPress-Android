package com.soundcloud.android.stations;

class StationViewModel {
    private final StationRecord station;
    private boolean isPlaying;

    StationViewModel(StationRecord station, boolean isPlaying) {
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
}
