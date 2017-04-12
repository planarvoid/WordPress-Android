package com.soundcloud.android.playback.ui;

import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.java.optional.Optional;

import android.net.Uri;

class AudioPlayerAd extends PlayerAd {

    private final AudioAd audioData;

    AudioPlayerAd(AudioAd audioAd) {
        super(audioAd);
        this.audioData = audioAd;
    }

    Optional<Uri> getImage() {
        return audioData.companionImageUrl();
    }

    Optional<String> getClickThroughUrl() {
        return audioData.clickThroughUrl();
    }

    boolean hasCompanion() {
        return audioData.hasCompanion();
    }

}
