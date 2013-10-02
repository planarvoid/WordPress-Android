package com.soundcloud.android.model;

import com.soundcloud.android.tracking.eventlogger.PlaySourceInfo;

import android.net.Uri;

import java.util.Arrays;
import java.util.List;

public class PlayInfo {
    public List<Track> playables;
    public int position;
    public Uri uri;
    public Track initialTrack;
    public boolean fetchRelated;
    public PlaySourceInfo trackingInfo;

    public PlayInfo() { }

    public PlayInfo(Uri uri, int position, Track initialTrack) {
        this.position = position;
        this.uri = uri;
        this.initialTrack = initialTrack;
        this.trackingInfo = new PlaySourceInfo.Builder(initialTrack.getId()).build();
    }

    public PlayInfo(Track track) {
        this(track, false,  new PlaySourceInfo.Builder(track.getId()).build());
    }

    public PlayInfo(Track track, boolean fetchRelated, PlaySourceInfo playSourceInfo) {
        this.playables = Arrays.asList(new Track[]{track});
        this.initialTrack = track;
        this.fetchRelated = fetchRelated;
    }
}
