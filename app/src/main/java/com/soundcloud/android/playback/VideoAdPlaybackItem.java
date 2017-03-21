package com.soundcloud.android.playback;

import auto.parcel.AutoParcel;
import com.soundcloud.android.ads.PlayableAdData;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.ads.VideoAdSource;
import com.soundcloud.android.model.Urn;

import android.os.Parcelable;

import java.util.List;

@AutoParcel
public abstract class VideoAdPlaybackItem implements PlaybackItem, Parcelable {

    private static final float INITIAL_VOLUME = 1.0f;

    public static VideoAdPlaybackItem create(VideoAd adData, long startPosition) {
        return create(adData, startPosition, INITIAL_VOLUME);
    }

    public static VideoAdPlaybackItem create(VideoAd adData, long startPosition, float initialVolume) {
        final boolean firstPlay = !adData.hasReportedEvent(PlayableAdData.ReportingEvent.START);
        return new AutoParcel_VideoAdPlaybackItem(adData.getAdUrn(),
                                                  adData.getVideoSources(),
                                                  startPosition,
                                                  initialVolume,
                                                  firstPlay,
                                                  adData.getUuid(),
                                                  adData.getMonetizationType().key(),
                                                  PlaybackType.VIDEO_AD,
                                                  adData.getDuration());
    }

    @Override
    public abstract Urn getUrn();

    public abstract List<VideoAdSource> getSources();

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
