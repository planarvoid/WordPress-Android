package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AppInstallAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.playback.PlaybackStateTransition;

import java.util.Date;

public abstract class InlayAdEvent {

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
        return this instanceof InlayPlayStateTransition;
    }

    @AutoValue
    public abstract static class OnScreen extends InlayAdEvent implements WithAdData {
        public static OnScreen create(int position, AdData ad, Date at) {
            return new AutoValue_InlayAdEvent_OnScreen(position, ad, at);
        }
    }

    @AutoValue
    public abstract static class ImageLoaded extends InlayAdEvent implements WithAdData {
        public static ImageLoaded create(int position, AppInstallAd ad, Date at) {
            return new AutoValue_InlayAdEvent_ImageLoaded(position, ad, at);
        }
    }

    @AutoValue
    public abstract static class NoVideoOnScreen extends InlayAdEvent {
        public abstract boolean shouldMute();

        public static NoVideoOnScreen create(Date at, boolean shouldMute) {
            return new AutoValue_InlayAdEvent_NoVideoOnScreen(at, shouldMute);
        }
    }

    @AutoValue
    public abstract static class ToggleVolume extends InlayAdEvent implements WithAdData {
        public static ToggleVolume create(int position, AdData ad, Date at) {
            return new AutoValue_InlayAdEvent_ToggleVolume(position, ad, at);
        }
    }

    @AutoValue
    public abstract static class TogglePlayback extends InlayAdEvent implements WithAdData {
        public static TogglePlayback create(int position, AdData ad, Date at) {
            return new AutoValue_InlayAdEvent_TogglePlayback(position, ad, at);
        }
    }

    @AutoValue
    public abstract static class InlayPlayStateTransition extends InlayAdEvent {
        public abstract VideoAd videoAd();
        public abstract PlaybackStateTransition stateTransition();
        public abstract boolean isMuted();

        public static InlayPlayStateTransition create(VideoAd videoAd, PlaybackStateTransition transition, boolean isMuted, Date at) {
            return new AutoValue_InlayAdEvent_InlayPlayStateTransition(at, videoAd, transition, isMuted);
        }
    }
}
