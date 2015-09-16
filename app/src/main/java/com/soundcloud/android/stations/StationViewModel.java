package com.soundcloud.android.stations;

class StationViewModel {
    private final Station station;
    private boolean isPlaying;

    StationViewModel(Station station, boolean isPlaying) {
        this.station = station;
        this.isPlaying = isPlaying;
    }

    Station getStation() {
        return station;
    }

    boolean isPlaying() {
        return isPlaying;
    }

    void setIsPlaying(boolean isPlaying) {
        this.isPlaying = isPlaying;
    }
}
