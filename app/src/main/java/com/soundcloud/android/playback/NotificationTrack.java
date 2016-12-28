package com.soundcloud.android.playback;

import com.soundcloud.android.R;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.strings.Strings;

import android.content.res.Resources;

public class NotificationTrack {

    private final Resources resources;
    private final TrackItem source;

    public NotificationTrack(Resources resources, TrackItem source) {
        this.resources = resources;
        this.source = source;
    }

    public String getTitle() {
        return isAudioAd()
               ? resources.getString(R.string.ads_advertisement)
               : source.getTitle();
    }

    public String getCreatorName() {
        return isAudioAd()
               ? Strings.EMPTY
               : source.getCreatorName();
    }

    public boolean isAudioAd() {
        return source.isAd();
    }

    public long getDuration() {
        return Durations.getTrackPlayDuration(source);
    }
}
