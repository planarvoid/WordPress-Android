package com.soundcloud.android.stations;

import static java.lang.Math.min;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

class StationWithTracks {

    private static final int MAX_NUMBER_OF_MOST_PLAYED_ARTIST = 3;

    private static final Comparator<StationInfoTrack> STATION_INFO_TRACK_COMPARATOR = (trackA, trackB) -> trackB.getTrack().playCount() - trackA.getTrack().playCount();

    private static final Function<StationInfoTrack, String> TO_CREATOR_NAME = input -> input.getTrack().creatorName();

    private Urn urn;
    private String type;
    private String title;
    private final String permalink;
    private List<StationInfoTrack> tracks;
    private Optional<String> imageUrlTemplate;
    private final int lastPlayedTrackPosition;
    private boolean liked;

    public static StationWithTracks from(StationWithTrackUrns entity, List<StationInfoTrack> tracks) {
        return new StationWithTracks(
                entity.urn(),
                entity.title(),
                entity.type(),
                entity.imageUrlTemplate(),
                entity.permalink().orNull(),
                tracks,
                Stations.NEVER_PLAYED,
                entity.liked());
    }

    StationWithTracks(Urn urn,
                      String title,
                      String type,
                      Optional<String> imageUrlTemplate,
                      String permalink,
                      List<StationInfoTrack> tracks,
                      int lastPlayedTrackPosition,
                      boolean liked) {
        this.urn = urn;
        this.type = type;
        this.title = title;
        this.permalink = permalink;
        this.tracks = tracks;
        this.imageUrlTemplate = imageUrlTemplate;
        this.lastPlayedTrackPosition = lastPlayedTrackPosition;
        this.liked = liked;
    }

    public List<StationInfoTrack> getStationInfoTracks() {
        return tracks;
    }

    public Urn getUrn() {
        return urn;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getPermalink() {
        return permalink;
    }

    public int getPreviousPosition() {
        return lastPlayedTrackPosition;
    }

    public void setTracks(List<StationInfoTrack> tracks) {
        this.tracks = tracks;
    }

    List<String> getMostPlayedArtists() {
        final List<StationInfoTrack> sortedList = new ArrayList<>(getStationInfoTracks());
        Collections.sort(sortedList, STATION_INFO_TRACK_COMPARATOR);
        final Collection<String> artistNames = MoreCollections.transform(sortedList, TO_CREATOR_NAME);
        final LinkedHashSet<String> uniqueArtists = new LinkedHashSet<>(artistNames);

        return new ArrayList<>(uniqueArtists)
                .subList(0, min(MAX_NUMBER_OF_MOST_PLAYED_ARTIST, uniqueArtists.size()));
    }

    public Optional<String> getImageUrlTemplate() {
        return imageUrlTemplate;
    }

    public boolean isLiked() {
        return liked;
    }
}
