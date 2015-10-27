package com.soundcloud.android.stations;

import com.soundcloud.android.api.model.StationRecord;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.objects.MoreObjects;

import java.util.ArrayList;
import java.util.List;

class Station implements StationRecord {

    private final String type;
    private final List<Urn> tracks;
    private final int lastPosition;
    private final Urn urn;
    private final String title;
    private final String permalink;

    public Station(Urn urn, String title, String type, List<Urn> tracks, String permalink, Integer lastPosition) {
        this.type = type;
        this.tracks = tracks;
        this.urn = urn;
        this.lastPosition = lastPosition;
        this.title = title;
        this.permalink = permalink;
    }

    static Station stationWithSeedTrack(StationRecord station, Urn seed) {
        final List<Urn> recommendations = station.getTracks();
        final List<Urn> tracks = new ArrayList<>(recommendations.size() + 1);
        tracks.add(seed);
        tracks.addAll(recommendations);
        return new Station(
                station.getUrn(),
                station.getTitle(),
                station.getType(),
                tracks,
                station.getPermalink(),
                station.getPreviousPosition()
        );
    }

    @Override
    public List<Urn> getTracks() {
        return tracks;
    }

    public Urn getUrn() {
        return urn;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String getPermalink() {
        return permalink;
    }

    public String getType() {
        return type;
    }

    @Override
    public int getPreviousPosition() {
        return lastPosition;
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
                MoreObjects.equal(lastPosition, that.lastPosition) &&
                MoreObjects.equal(tracks, that.tracks);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(urn, title, lastPosition, tracks);
    }
}
