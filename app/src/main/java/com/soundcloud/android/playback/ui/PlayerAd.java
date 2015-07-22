package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;

import android.content.res.Resources;
import android.graphics.Color;
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

    String getAdTitle() {
        return source.get(PlayableProperty.TITLE);
    }

    String getPreviewTitle() {
        final String nextTrackTitle = source.get(AdProperty.MONETIZABLE_TRACK_TITLE);
        final String nextTrackCreator = source.get(AdProperty.MONETIZABLE_TRACK_CREATOR);
        return resources.getString(R.string.next_up, nextTrackTitle, nextTrackCreator);
    }

    Urn getMonetizableTrack() {
        return source.get(AdProperty.MONETIZABLE_TRACK_URN);
    }

    int getDefaultTextColor() {
        return Color.parseColor(source.get(AdProperty.DEFAULT_TEXT_COLOR));
    }

    int getDefaultBackgroundColor() {
        return Color.parseColor(source.get(AdProperty.DEFAULT_BACKGROUND_COLOR));
    }

    int getPressedTextColor() {
        return Color.parseColor(source.get(AdProperty.PRESSED_TEXT_COLOR));
    }

    int getPressedBackgroundColor() {
        return Color.parseColor(source.get(AdProperty.PRESSED_BACKGROUND_COLOR));
    }

    int getFocusedTextColor() {
        return Color.parseColor(source.get(AdProperty.FOCUSED_TEXT_COLOR));
    }

    int getFocusedBackgroundColor() {
        return Color.parseColor(source.get(AdProperty.FOCUSED_BACKGROUND_COLOR));
    }
}
