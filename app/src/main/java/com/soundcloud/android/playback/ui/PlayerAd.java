package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.PlayerAdData;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;

import android.content.res.Resources;
import android.graphics.Color;

abstract class PlayerAd extends PlayerItem {

    private final PlayerAdData adData;

    PlayerAd(PlayerAdData adData, PropertySet source) {
        super(source);
        this.adData = adData;
    }

    abstract String getCallToActionButtonText(Resources resources);

    Urn getAdUrn() {
        return adData.getAdUrn();
    }

    String getPreviewTitle(Resources resources) {
        final String nextTrackTitle = adData.getMonetizableTitle();
        final String nextTrackCreator = adData.getMonetizableCreator();
        return resources.getString(R.string.ads_next_up_tracktitle_creatorname, nextTrackTitle, nextTrackCreator);
    }

    Urn getMonetizableTrack() {
        return adData.getMonetizableTrackUrn();
    }

    int getDefaultTextColor() {
        return Color.parseColor(adData.getDisplayProperties().getDefaultTextColor());
    }

    int getDefaultBackgroundColor() {
        return Color.parseColor(adData.getDisplayProperties().getDefaultBackgroundColor());
    }

    int getPressedTextColor() {
        return Color.parseColor(adData.getDisplayProperties().getPressedTextColor());
    }

    int getPressedBackgroundColor() {
        return Color.parseColor(adData.getDisplayProperties().getPressedBackgroundColor());
    }

    int getFocusedTextColor() {
        return Color.parseColor(adData.getDisplayProperties().getFocusedTextColor());
    }

    int getFocusedBackgroundColor() {
        return Color.parseColor(adData.getDisplayProperties().getFocusedBackgroundColor());
    }

    PlayerAdData getAdData() {
        return adData;
    }
}
