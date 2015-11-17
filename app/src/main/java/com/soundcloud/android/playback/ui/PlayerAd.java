package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.PlayerAdData;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.collections.PropertySet;

import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;

public class PlayerAd extends PlayerItem {

    private final PlayerAdData adData;

    PlayerAd(PlayerAdData adData, PropertySet source) {
        super(source);
        this.adData = adData;
    }

    String getAdUrn() {
        return adData.getAdUrn();
    }

    Uri getArtwork() {
        return adData.getVisualAd().getImageUrl();
    }

    String getAdTitle() {
        return source.get(PlayableProperty.TITLE);
    }

    String getPreviewTitle(Resources resources) {
        final String nextTrackTitle = adData.getMonetizableTitle();
        final String nextTrackCreator = adData.getMonetizableCreator();
        return resources.getString(R.string.ads_next_up_tracktitle_creatorname, nextTrackTitle, nextTrackCreator);
    }

    Urn getMonetizableTrack() {
        return adData.getMonetizableTrackUrn();
    }

    String getCallToActionButtonText(Resources resources) {
        return adData.getVisualAd().getCallToActionButtonText().or(
            resources.getString(R.string.ads_call_to_action)
        );
    }

    int getDefaultTextColor() {
        return Color.parseColor(adData.getVisualAd().getDefaultTextColor());
    }

    int getDefaultBackgroundColor() {
        return Color.parseColor(adData.getVisualAd().getDefaultBackgroundColor());
    }

    int getPressedTextColor() {
        return Color.parseColor(adData.getVisualAd().getPressedTextColor());
    }

    int getPressedBackgroundColor() {
        return Color.parseColor(adData.getVisualAd().getPressedBackgroundColor());
    }

    int getFocusedTextColor() {
        return Color.parseColor(adData.getVisualAd().getFocusedTextColor());
    }

    int getFocusedBackgroundColor() {
        return Color.parseColor(adData.getVisualAd().getFocusedBackgroundColor());
    }
}
