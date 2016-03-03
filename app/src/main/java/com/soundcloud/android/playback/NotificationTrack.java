package com.soundcloud.android.playback;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.java.collections.PropertySet;

import android.content.res.Resources;

public class NotificationTrack {

    private final Resources resources;
    private final PropertySet source;

    public NotificationTrack(Resources resources, PropertySet source) {
        this.resources = resources;
        this.source = source;
    }

    public String getTitle() {
        return source.get(PlayableProperty.TITLE);
    }

    public String getCreatorName() {
        if (isAnAdvertisement()) {
            return resources.getString(R.string.ads_advertisement);
        }
        return source.get(PlayableProperty.CREATOR_NAME);
    }

    public boolean isAnAdvertisement() {
        return source.get(AdProperty.IS_AUDIO_AD);
    }

    public long getDuration() {
        return Durations.getTrackPlayDuration(source);
    }
}
