package com.soundcloud.android.playback.ui;

import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.optional.Optional;

import android.net.Uri;

class AudioPlayerAd extends PlayerAd {

    private final AudioAd audioData;

    AudioPlayerAd(AudioAd audioAd) {
        super(audioAd, TrackItem.EMPTY);
        this.audioData = audioAd;
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
