package com.soundcloud.android.playback.ui;

import android.content.res.Resources;
import android.net.Uri;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.java.collections.PropertySet;

public class AudioPlayerAd extends PlayerAd {

    private final AudioAd audioData;

    AudioPlayerAd(AudioAd audioAd, PropertySet source) {
        super(audioAd, source);
        this.audioData = audioAd;
    }

    @Override
    String getCallToActionButtonText(Resources resources) {
        return audioData.getVisualAd().getCallToActionButtonText().or(
                resources.getString(R.string.ads_call_to_action)
        );
    }

    Uri getArtwork() {
        return audioData.getVisualAd().getImageUrl();
    }

    String getAdTitle() {
        return source.get(PlayableProperty.TITLE);
    }

}
