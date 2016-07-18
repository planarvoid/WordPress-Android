package com.soundcloud.android.stations;

class StationInfoItem {

    public enum Kind {
        StationHeader,
        StationTracksBucket
    }

    private final Kind kind;

    StationInfoItem(Kind kind) {
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }
}
