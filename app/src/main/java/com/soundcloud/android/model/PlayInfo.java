package com.soundcloud.android.model;

import android.net.Uri;

import java.util.Arrays;
import java.util.List;

public class PlayInfo {
    public List<Track> playables;
    public int position;
    public Uri uri;
    public Track initialTrack;

    public PlayInfo() { }

    public PlayInfo(Uri uri, int position, Track initialTrack) {
        this.position = position;
        this.uri = uri;
        this.initialTrack = initialTrack;
    }

    public PlayInfo(Track track) {
        this.playables = Arrays.asList(new Track[]{track});
        this.initialTrack = track;
    }
}
