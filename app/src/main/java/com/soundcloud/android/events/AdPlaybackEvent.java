package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AppInstallAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.playback.PlaybackStateTransition;

import java.util.Date;

public abstract class AdPlaybackEvent {

    public interface WithAdData {
        int getPosition();
        AdData getAd();
    }

    public abstract Date getEventTime();

    public boolean forAppInstall() {
        return this instanceof WithAdData && ((WithAdData) this).getAd() instanceof AppInstallAd;
    }

    public boolean forVideoAd() {
        return (this instanceof WithAdData && ((WithAdData) this).getAd() instanceof VideoAd)
                || this instanceof NoVideoOnScreen
                || this instanceof ToggleVolume
                || this instanceof TogglePlayback;
    }

    public boolean forStateTransition() {
        return this instanceof AdPlayStateTransition;
    }

    @AutoValue
    public abstract static class OnScreen extends AdPlaybackEvent implements WithAdData {
        public static OnScreen create(int position, AdData ad, Date at) {
            return new AutoValue_AdPlaybackEvent_OnScreen(position, ad, at);
        }
    }

    @AutoValue
    public abstract static class ImageLoaded extends AdPlaybackEvent implements WithAdData {
        public static ImageLoaded create(int position, AppInstallAd ad, Date at) {
            return new AutoValue_AdPlaybackEvent_ImageLoaded(position, ad, at);
        }
    }

    @AutoValue
    public abstract static class NoVideoOnScreen extends AdPlaybackEvent {
        public abstract boolean shouldMute();

        public static NoVideoOnScreen create(Date at, boolean shouldMute) {
            return new AutoValue_AdPlaybackEvent_NoVideoOnScreen(at, shouldMute);
        }
    }

    @AutoValue
    public abstract static class ToggleVolume extends AdPlaybackEvent implements WithAdData {
        public static ToggleVolume create(int position, AdData ad, Date at) {
            return new AutoValue_AdPlaybackEvent_ToggleVolume(position, ad, at);
        }
    }

    @AutoValue
    public abstract static class TogglePlayback extends AdPlaybackEvent implements WithAdData {
        public static TogglePlayback create(int position, AdData ad, Date at) {
            return new AutoValue_AdPlaybackEvent_TogglePlayback(position, ad, at);
        }
    }

    @AutoValue
    public abstract static class AdPlayStateTransition extends AdPlaybackEvent {
        public abstract VideoAd videoAd();
        public abstract PlaybackStateTransition stateTransition();
        public abstract boolean isMuted();

        public static AdPlayStateTransition create(VideoAd videoAd, PlaybackStateTransition transition, boolean isMuted, Date at) {
            return new AutoValue_AdPlaybackEvent_AdPlayStateTransition(at, videoAd, transition, isMuted);
        }
    }
}
