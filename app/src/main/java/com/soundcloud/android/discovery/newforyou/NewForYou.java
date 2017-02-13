package com.soundcloud.android.discovery.newforyou;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.Track;

import java.util.Date;
import java.util.List;

@AutoValue
public abstract class NewForYou {
    public abstract Date lastUpdate();
    public abstract Urn queryUrn();
    public abstract List<Track> tracks();

    public static NewForYou create(Date lastUpdated, Urn queryUrn, List<Track> tracks) {
        return new AutoValue_NewForYou(lastUpdated, queryUrn, tracks);
    }
}
