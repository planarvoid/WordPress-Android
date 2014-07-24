package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.propeller.PropertySet;

import android.content.res.Resources;
import android.net.Uri;

public class PlayerAd {

    private final PropertySet source;
    private final Resources resources;

    PlayerAd(PropertySet source, Resources resources) {
        this.source = source;
        this.resources = resources;
    }

    Uri getArtwork() {
        return source.get(AdProperty.ARTWORK);
    }

    String getAdvertiser() {
        return source.get(PlayableProperty.CREATOR_NAME);
    }

    String getPreviewTitle() {
        return String.format(resources.getString(R.string.next_up), "Monetizable track (creator name)");
    }

    TrackUrn getMonetizableTrack() {
        return source.get(AdProperty.MONETIZABLE_TRACK_URN);
    }
}
