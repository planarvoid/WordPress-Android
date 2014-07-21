package com.soundcloud.android.playback.ui;

import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.propeller.PropertySet;

import android.net.Uri;

public class PlayerAd {

    private final PropertySet source;

    PlayerAd(PropertySet source) {
        this.source = source;
    }

    public Uri getArtwork() {
        return source.get(AdProperty.ARTWORK);
    }

    public String getAdvertiser() {
        return source.get(PlayableProperty.TITLE);
    }

    Uri getClickThroughLink() {
        return source.get(AdProperty.CLICK_THROUGH_LINK);
    }

}
