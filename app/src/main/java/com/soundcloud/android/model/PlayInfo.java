package com.soundcloud.android.model;

import com.soundcloud.android.tracking.eventlogger.PlaySourceInfo;

import android.net.Uri;

import java.util.Arrays;
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
        this.iniitalTracklist = Arrays.asList(new Track[]{track});
        this.fetchRelated = fetchRelated;
        this.sourceInfo = playSourceInfo;
    }

    public static PlayInfo fromTrack(Track track){
        return new PlayInfo(track, false, new PlaySourceInfo.Builder(track.getId()).build());
    }

    public static PlayInfo fromExploreTrack(Track track, String exploreTag){
        final PlaySourceInfo playSourceInfo = new PlaySourceInfo.Builder(track.getId()).exploreTag(exploreTag).build();
        return new PlayInfo(track, true, playSourceInfo);
    }

    private static PlayInfo fromUri(Uri uri, int startPosition){
        PlayInfo playInfo = new PlayInfo();
        playInfo.uri = uri;
        playInfo.position = startPosition;
        return playInfo;
    }

    public static PlayInfo fromUri(Uri uri, int startPosition, Track initialTrack){
        PlayInfo playInfo = fromUri(uri, startPosition);
        playInfo.initialTrack = initialTrack;
        playInfo.sourceInfo = new PlaySourceInfo.Builder(initialTrack.getId()).build();
        return playInfo;
    }
    public static PlayInfo fromUri(Uri uri, int startPosition, Track initialTrack, List<Track> initialTrackList){
        PlayInfo playInfo = fromUri(uri, startPosition, initialTrack);
        playInfo.iniitalTracklist = initialTrackList;
        return playInfo;
    }
}
