package com.soundcloud.android.playback.ui;

import com.soundcloud.java.collections.PropertySet;

final class VideoPageData extends PlayerPageData {

    VideoPageData(int positionInPlayQueue,  PropertySet properties) {
        super(Kind.VIDEO, positionInPlayQueue, properties);
    }

    @Override
    public String toString() {
        return "VideoPageData {" +
                "positionInPlayQueue=" + positionInPlayQueue +
                ", properties=" + properties +
                '}';
    }
}
