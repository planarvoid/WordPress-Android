package com.soundcloud.android.playback;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

public class VideoAdQueueItem extends PlayQueueItem {

    public VideoAdQueueItem(VideoAd adData) {
        this.adData = Optional.<AdData>of(adData);
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
        return Kind.VIDEO;
    }

    public boolean isVerticalVideo() {
        return ((VideoAd) this.adData.get()).isVerticalVideo();
    }
}
