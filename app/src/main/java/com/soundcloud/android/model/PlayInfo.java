package com.soundcloud.android.model;

import android.net.Uri;

import java.util.Arrays;
import java.util.List;

public class PlayInfo {
    public List<Track> playables;
    public int position;
    public Uri uri;
    public Track initialTrack;
    public boolean fetchRelated;

    public PlayInfo() { }

    public PlayInfo(Uri uri, int position, Track initialTrack) {
        this.position = position;
        this.uri = uri;
        this.initialTrack = initialTrack;
    }

    public PlayInfo(Track track) {
        this(track, false);
    }

    public PlayInfo(Track track, boolean fetchRelated) {
        this.playables = Arrays.asList(new Track[]{track});
        this.initialTrack = track;
        this.fetchRelated = fetchRelated;
    }
}
