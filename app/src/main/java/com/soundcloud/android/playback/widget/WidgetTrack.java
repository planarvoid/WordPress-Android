package com.soundcloud.android.playback.widget;

import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;

class WidgetTrack {

    private final PropertySet source;

    WidgetTrack(PropertySet source) {
        this.source = source;
    }

    Urn getUrn() {
        return source.get(TrackProperty.URN);
    }

    String getTitle() {
        return source.get(PlayableProperty.TITLE);
    }

    String getUserName() {
        return source.get(PlayableProperty.CREATOR_NAME);
    }

    Urn getUserUrn() {
        return source.get(PlayableProperty.CREATOR_URN);
    }

    boolean isUserLike() {
        return source.get(PlayableProperty.IS_LIKED);
    }

    boolean isAudioAd() {
        return source.contains(AdProperty.AD_URN) &&
                source.contains(AdProperty.AD_TYPE) &&
                source.get(AdProperty.AD_TYPE).equals(AdProperty.AD_TYPE_AUDIO);
    }
}
