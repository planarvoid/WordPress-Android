package com.soundcloud.android.stations;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;

import java.util.ArrayList;
import java.util.List;

public class Station implements StationRecord {

    private final String type;
    private final List<StationTrack> tracks;
    private final int lastPosition;
    private final Urn urn;
    private final String title;
    private final String permalink;
    private final Optional<String> artworkUrlTemplate;

    public Station(Urn urn, String title, String type, List<StationTrack> tracks,
                   String permalink, Integer lastPosition, Optional<String> artworkUrlTemplate) {
        this.type = type;
        this.tracks = tracks;
        this.urn = urn;
        this.lastPosition = lastPosition;
        this.title = title;
        this.permalink = permalink;
        this.artworkUrlTemplate = artworkUrlTemplate;
    }

    static Station stationWithTracks(Station station, List<StationTrack> tracks) {
        return new Station(station.getUrn(),
                           station.getTitle(),
                           station.getType(),
                           tracks,
                           station.getPermalink(),
                           station.getPreviousPosition(),
                           station.getImageUrlTemplate());
    }

    static Station stationWithSeedTrack(StationRecord station, Urn seed) {
        final List<StationTrack> recommendations = station.getTracks();
        final List<StationTrack> tracks = new ArrayList<>(recommendations.size() + 1);
        tracks.add(StationTrack.create(seed, Urn.NOT_SET));
        tracks.addAll(recommendations);
        return new Station(
                station.getUrn(),
                station.getTitle(),
                station.getType(),
                tracks,
                station.getPermalink(),
                station.getPreviousPosition(),
                station.getImageUrlTemplate());
    }

    @Override
    public List<StationTrack> getTracks() {
        return tracks;
    }

    @Override
    public Urn getUrn() {
        return urn;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getPermalink() {
        return permalink;
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return artworkUrlTemplate;
    }

    @Override
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
