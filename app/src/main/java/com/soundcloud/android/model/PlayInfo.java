package com.soundcloud.android.model;

import android.net.Uri;

import java.util.Arrays;
import java.util.List;

public class PlayInfo {
    public List<Track> tracks;
    public int position;
    public Uri uri;

    // TODO, playlists

    public Track getTrack() {
        return tracks.get(Math.max(0,Math.min(tracks.size() -1 ,position))).getPlayable();
    }

    public static PlayInfo forTracks(Track... t) {
        PlayInfo info = new PlayInfo();
        info.tracks = Arrays.asList(t);
        return info;
    }
}
