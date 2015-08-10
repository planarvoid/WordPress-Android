package com.soundcloud.android.stations;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.objects.MoreObjects;

import java.util.List;

public class Station {
    // TODO : should it be a play queue ?
    private final List<Urn> tracks;
    private final int startPosition;
    private final Urn urn;
    private final String title;

    public Station(Urn urn, String title, List<Urn> tracks, Integer startPosition) {
        this.tracks = tracks;
        this.urn = urn;
        this.startPosition = startPosition;
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public Urn getUrn() {
        return urn;
    }

    public List<Urn> getTracks() {
        return tracks;
    }

    public int getStartPosition() {
        return startPosition;
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

        return MoreObjects.equal(urn, that.urn) &&
                MoreObjects.equal(title, that.title) &&
                MoreObjects.equal(startPosition, that.startPosition) &&
                MoreObjects.equal(tracks, that.tracks);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(urn, title, startPosition, tracks);
    }
}
