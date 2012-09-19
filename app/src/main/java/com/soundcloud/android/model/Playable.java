package com.soundcloud.android.model;

import android.content.*;
import android.net.Uri;
import android.os.Parcelable;
import android.widget.*;

import java.util.Arrays;
import java.util.List;

public interface Playable extends Parcelable {
    Track getTrack();
    CharSequence getTimeSinceCreated(Context context);
    void refreshTimeSinceCreated(Context context);


    class PlayInfo {
        public List<Playable> playables;
        public int position;
        public Uri uri;

        public Track getTrack() {
            return playables.get(Math.max(0,Math.min(playables.size() -1 ,position))).getTrack();
        }

        public static PlayInfo forTracks(Track... t) {
            PlayInfo info = new PlayInfo();
            info.playables = Arrays.<Playable>asList(t);
            return info;
        }
    }
}
