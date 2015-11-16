package com.soundcloud.android.playback.ui;

import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.PlayerAdData;
import com.soundcloud.java.optional.Optional;

public abstract class PlayerPageData {
    enum Kind {TRACK, VIDEO}

    final int positionInPlayQueue;
    final Optional<AdData> adData;
    final Kind kind;

    PlayerPageData(Kind kind, int positionInPlayQueue, Optional<AdData> adData)  {
        this.kind = kind;
        this.positionInPlayQueue = positionInPlayQueue;
        this.adData = adData;
    }

    public int getPositionInPlayQueue() {
        return positionInPlayQueue;
    }

    public Optional<AdData> getAdData() {
        return adData;
    }

    boolean isAdPage(){
        return adData.isPresent() && adData.get() instanceof PlayerAdData;
    }

    boolean isTrackPage() {
        return this.kind == Kind.TRACK;
    }

    boolean isVideoPage() {
        return this.kind == Kind.VIDEO;
    }
}
