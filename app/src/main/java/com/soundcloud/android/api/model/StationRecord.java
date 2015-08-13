package com.soundcloud.android.api.model;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackRecord;

public interface StationRecord {
    ModelCollection<? extends TrackRecord> getTracks();

    Urn getUrn();

    String getType();

    String getTitle();

    String getPermalink();
}
