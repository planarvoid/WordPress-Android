package com.soundcloud.android.playback.ui;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.java.optional.Optional;

final class VideoPageData extends PlayerPageData {

    VideoPageData(int positionInPlayQueue, Optional<AdData> adData) {
        super(Kind.VIDEO, adData.get().getAdUrn(), positionInPlayQueue, adData);
    }

    @Override
    public String toString() {
        return "VideoPageData {" +
                "positionInPlayQueue=" + positionInPlayQueue +
                ", properties=" + adData +
                '}';
    }
}
