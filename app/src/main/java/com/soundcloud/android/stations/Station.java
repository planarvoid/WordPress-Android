package com.soundcloud.android.stations;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;

import java.util.List;

public class Station {
    private final PropertySet info;
    // TODO : should it be a play queue ?
    private final List<Urn> tracks;

    public Station(PropertySet info, List<Urn> tracks) {
        this.info = info;
        this.tracks = tracks;
    }

    public String getTitle() {
        return info.get(StationProperty.TITLE);
    }

    public Urn getUrn() {
        return info.get(StationProperty.URN);
    }

    public List<Urn> getTracks() {
        return tracks;
    }

    public int getStartPosition() {
        return info.get(StationProperty.LAST_PLAYED_TRACK_POSITION);
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Station that = (Station) o;

        return MoreObjects.equal(info, that.info) &&
                MoreObjects.equal(tracks, that.tracks);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(info, tracks);
    }
}
