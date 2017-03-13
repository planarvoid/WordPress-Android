package com.soundcloud.android.playback;

import com.soundcloud.android.ads.AudioAd;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

public class AudioAdQueueItem extends PlayQueueItem {

    public AudioAdQueueItem(AudioAd adData) {
        this.adData = Optional.of(adData);
    }

    @Override
    public Urn getUrn() {
        return getAdData().get().getAdUrn();
    }

    @Override
    public boolean shouldPersist() {
        return false;
    }

    @Override
    public Kind getKind() {
        return Kind.AUDIO_AD;
    }

}
