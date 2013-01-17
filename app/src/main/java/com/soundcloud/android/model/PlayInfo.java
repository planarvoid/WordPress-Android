package com.soundcloud.android.model;

import android.net.Uri;

import java.util.Arrays;
import java.util.List;

public class PlayInfo {
    public List<Playable> playables;
    public int position;
    public Uri uri;
    public Track initialTrack;

    public static PlayInfo forTracks(Playable... p) {
        PlayInfo info = new PlayInfo();
        info.playables = Arrays.asList(p);
        return info;
    }
}
