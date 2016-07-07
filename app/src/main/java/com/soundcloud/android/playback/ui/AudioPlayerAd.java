package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

import android.content.res.Resources;
import android.net.Uri;

public class AudioPlayerAd extends PlayerAd {

    private final AudioAd audioData;

    AudioPlayerAd(AudioAd audioAd, PropertySet source) {
        super(audioAd, source);
        this.audioData = audioAd;
    }

    @Override
    String getCallToActionButtonText(Resources resources) {
        return audioData.getCallToActionButtonText().or(resources.getString(R.string.ads_call_to_action));
    }

    Optional<Uri> getImage() {
        return audioData.getCompanionImageUrl();
    }

    Optional<String> getClickThroughUrl() {
        return audioData.getClickThroughUrl();
    }

    boolean hasCompanion() {
        return audioData.hasCompanion();
    }

}
