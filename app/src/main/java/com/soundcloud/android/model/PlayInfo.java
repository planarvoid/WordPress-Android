package com.soundcloud.android.model;

import com.google.common.collect.Lists;
import com.soundcloud.android.tracking.eventlogger.PlaySourceInfo;

import android.net.Uri;

import java.util.List;

public class PlayInfo {
    public int position;
    public Uri uri;
    public Track initialTrack;
    public List<Track> iniitalTracklist;
    public boolean fetchRelated;
    public PlaySourceInfo sourceInfo;

    private PlayInfo() {
    }

    private PlayInfo(Track track, boolean fetchRelated, PlaySourceInfo playSourceInfo) {
        this.initialTrack = track;
        this.iniitalTracklist = Lists.newArrayList(track);
        this.fetchRelated = fetchRelated;
        this.sourceInfo = playSourceInfo;
    }

    /**
     * Single play, the tracklist will be of length 1
     */
    public static PlayInfo fromTrack(Track track) {
        final PlaySourceInfo playSourceInfo = new PlaySourceInfo.Builder(track.getId()).build();
        return new PlayInfo(track, false, playSourceInfo);
    }

    /**
     * Created by anything played from the {@link com.soundcloud.android.activity.landing.ExploreActivity} section.
     */
    public static PlayInfo fromExploreTrack(Track track, String exploreTag) {
        final PlaySourceInfo playSourceInfo = new PlaySourceInfo.Builder(track.getId()).exploreTag(exploreTag).build();
        return new PlayInfo(track, true, playSourceInfo);
    }

    /**
     * From a uri with an initial track to show while loading.
     * Use {@link this#fromUriWithInitialTracklist(android.net.Uri, int, Track, java.util.List)} instead if a tracklist is available
     */
    public static PlayInfo fromUriWithTrack(Uri uri, int startPosition, Track initialTrack) {
        PlayInfo playInfo = fromUri(uri, startPosition);
        playInfo.initialTrack = initialTrack;
        playInfo.sourceInfo = new PlaySourceInfo.Builder(initialTrack.getId()).build();
        return playInfo;
    }

    /**
     * Play from a uri, providing an initial tracklist to present while loading.
     */
    public static PlayInfo fromUriWithInitialTracklist(Uri uri, int startPosition, Track initialTrack, List<Track> initialTrackList) {
        PlayInfo playInfo = fromUriWithTrack(uri, startPosition, initialTrack);
        playInfo.iniitalTracklist = initialTrackList;
        return playInfo;
    }

    private static PlayInfo fromUri(Uri uri, int startPosition) {
        PlayInfo playInfo = new PlayInfo();
        playInfo.uri = uri;
        playInfo.position = startPosition;
        return playInfo;
    }
}
