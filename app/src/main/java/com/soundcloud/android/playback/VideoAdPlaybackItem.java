package com.soundcloud.android.playback;

import auto.parcel.AutoParcel;
import com.soundcloud.android.ads.PlayableAdData;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.ads.VideoAdSource;
import com.soundcloud.android.model.Urn;

import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@AutoParcel
public abstract class VideoAdPlaybackItem implements PlaybackItem, Parcelable {

    private static final float INITIAL_VOLUME = 1.0f;

    public static VideoAdPlaybackItem create(VideoAd adData, long startPosition) {
        return create(adData, startPosition, INITIAL_VOLUME);
    }

    public static VideoAdPlaybackItem create(VideoAd adData, long startPosition, float initialVolume) {
        final boolean firstPlay = !adData.hasReportedEvent(PlayableAdData.ReportingEvent.START);
        return new AutoParcel_VideoAdPlaybackItem(adData.adUrn(),
                                                  adData.videoSources(),
                                                  startPosition,
                                                  initialVolume,
                                                  firstPlay,
                                                  adData.uuid(),
                                                  adData.monetizationType().key(),
                                                  PlaybackType.VIDEO_AD,
                                                  adData.duration());
    }

    @Override
    public abstract Urn getUrn();

    public abstract List<VideoAdSource> getSources();

    List<VideoAdSource> getSortedSources() {
        final List<VideoAdSource> sources = new ArrayList<>(getSources());
        Collections.sort(sources, VideoAdSource.BITRATE_COMPARATOR);
        return sources;
    }

    @Override
    public abstract long getStartPosition();

    public abstract float getInitialVolume();

    public abstract boolean isFirstPlay();

    public abstract String getUuid();

    public abstract String getMonetizationType();

    @Override
    public abstract PlaybackType getPlaybackType();

    @Override
    public abstract long getDuration();
}
