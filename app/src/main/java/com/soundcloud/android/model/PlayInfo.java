package com.soundcloud.android.model;

import android.net.Uri;

import java.util.Arrays;
import java.util.List;

public class PlayInfo {
    public List<PlayableHolder> playables;
    public int position;
    public Uri uri;

    public Track getTrack() {
        return playables.get(Math.max(0,Math.min(playables.size() -1 ,position))).getPlayable();
    }

    public static PlayInfo forTracks(Track... t) {
        PlayInfo info = new PlayInfo();
        info.playables = Arrays.<PlayableHolder>asList(t);
        return info;
    }
}
