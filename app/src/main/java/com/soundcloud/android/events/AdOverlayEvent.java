package com.soundcloud.android.events;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.java.collections.PropertySet;

import android.support.annotation.Nullable;

public class AdOverlayEvent {

    public static final int SHOWN = 0;
    public static final int HIDDEN = 1;

    private static final AdOverlayEvent HIDDEN_EVENT = new AdOverlayEvent(HIDDEN, Urn.NOT_SET, null, null);

    private final int kind;
    private final Urn currentPlayingUrn;
    private final PropertySet adMetaData;
    private final TrackSourceInfo trackSourceInfo;

    public static AdOverlayEvent shown(Urn playingUrn, PropertySet adMetaData, TrackSourceInfo trackSourceInfo) {
        return new AdOverlayEvent(SHOWN, playingUrn, adMetaData, trackSourceInfo);
    }
    public static AdOverlayEvent hidden() {
        return HIDDEN_EVENT;
    }

    public AdOverlayEvent(int kind, Urn playingUrn, PropertySet adMetaData, TrackSourceInfo trackSourceInfo) {
        this.kind = kind;
        this.currentPlayingUrn = playingUrn;
        this.adMetaData = adMetaData;
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
    public PropertySet getAdMetaData() {
        return adMetaData;
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
