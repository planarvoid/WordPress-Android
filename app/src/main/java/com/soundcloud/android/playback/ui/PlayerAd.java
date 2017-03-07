package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.PlayableAdData;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.strings.Strings;

import android.content.res.Resources;
import android.graphics.Color;

abstract class PlayerAd extends PlayerItem {

    private final PlayableAdData adData;

    PlayerAd(PlayableAdData adData, TrackItem trackItem) {
        super(trackItem);
        this.adData = adData;
    }

    PlayerAd(PlayableAdData adData) {
        this.adData = adData;
    }

    String getCallToActionButtonText(Resources resources) {
        return adData.getCallToActionButtonText().or(resources.getString(R.string.ads_call_to_action));
    }

    Urn getAdUrn() {
        return adData.getAdUrn();
    }

    String getPreviewTitle(Resources resources) {
        if (adData.hasMonetizableTitleAndCreator()) {
            final String nextTrackTitle = adData.getMonetizableTitle();
            final String nextTrackCreator = adData.getMonetizableCreator();
            return resources.getString(R.string.ads_next_up_tracktitle_creatorname, nextTrackTitle, nextTrackCreator);
        } else {
            return Strings.EMPTY;
        }
    }

    Urn getMonetizableTrack() {
        return adData.getMonetizableTrackUrn();
    }

    boolean hasVisualAdProperties() {
        return adData.getDisplayProperties().isPresent();
    }

    int getDefaultTextColor() {
        return Color.parseColor(adData.getDisplayProperties().get().getDefaultTextColor());
    }

    int getDefaultBackgroundColor() {
        return Color.parseColor(adData.getDisplayProperties().get().getDefaultBackgroundColor());
    }

    int getPressedTextColor() {
        return Color.parseColor(adData.getDisplayProperties().get().getPressedTextColor());
    }

    int getPressedBackgroundColor() {
        return Color.parseColor(adData.getDisplayProperties().get().getPressedBackgroundColor());
    }

    int getFocusedTextColor() {
        return Color.parseColor(adData.getDisplayProperties().get().getFocusedTextColor());
    }

    int getFocusedBackgroundColor() {
        return Color.parseColor(adData.getDisplayProperties().get().getFocusedBackgroundColor());
    }

    PlayableAdData getAdData() {
        return adData;
    }
}
