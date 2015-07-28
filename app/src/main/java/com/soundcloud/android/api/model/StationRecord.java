package com.soundcloud.android.api.model;

import com.soundcloud.android.tracks.TrackRecord;

public interface StationRecord {
    ApiStationInfo getInfo();

    ModelCollection<? extends TrackRecord> getTracks();
}
