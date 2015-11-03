package com.soundcloud.android.playback.ui;

import com.soundcloud.android.ads.AdProperty;
import com.soundcloud.java.collections.PropertySet;

public abstract class PlayerPageData {
    enum Kind {TRACK, VIDEO}

    final int positionInPlayQueue;
    final PropertySet properties;
    final Kind kind;

    PlayerPageData(Kind kind, int positionInPlayQueue, PropertySet properties)  {
        this.kind = kind;
        this.positionInPlayQueue = positionInPlayQueue;
        this.properties = properties;
    }

    public int getPositionInPlayQueue() {
        return positionInPlayQueue;
    }

    public PropertySet getProperties() {
        return properties;
    }

    boolean isAdPage(){
        return properties.contains(AdProperty.AD_URN);
    }

    boolean isTrackPage() {
        return this.kind == Kind.TRACK;
    }

    boolean isVideoPage() {
        return this.kind == Kind.VIDEO;
    }
}
