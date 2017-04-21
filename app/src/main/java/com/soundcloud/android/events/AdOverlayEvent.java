package com.soundcloud.android.events;

import com.soundcloud.android.ads.VisualAdData;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;

import android.support.annotation.Nullable;

public class AdOverlayEvent {

    public static final int SHOWN = 0;
    public static final int HIDDEN = 1;

    private static final AdOverlayEvent HIDDEN_EVENT = new AdOverlayEvent(HIDDEN, Urn.NOT_SET, null, null);

    private final int kind;
    private final Urn currentPlayingUrn;
    private final VisualAdData adData;
    private final TrackSourceInfo trackSourceInfo;

    public static AdOverlayEvent shown(Urn playingUrn, VisualAdData adData, TrackSourceInfo trackSourceInfo) {
        return new AdOverlayEvent(SHOWN, playingUrn, adData, trackSourceInfo);
    }

    public static AdOverlayEvent hidden() {
        return HIDDEN_EVENT;
    }

    public AdOverlayEvent(int kind, Urn playingUrn, VisualAdData adData, TrackSourceInfo trackSourceInfo) {
        this.kind = kind;
        this.currentPlayingUrn = playingUrn;
        this.adData = adData;
        this.trackSourceInfo = trackSourceInfo;
    }

    public int getKind() {
        return kind;
    }

    @Override
    public String toString() {
        return "AdOverlayEvent: " + kind;
    }

    @Nullable
    public VisualAdData getAdData() {
        return adData;
    }

    @Nullable
    public TrackSourceInfo getTrackSourceInfo() {
        return trackSourceInfo;
    }

    @Nullable
    public Urn getCurrentPlayingUrn() {
        return currentPlayingUrn;
    }
}
