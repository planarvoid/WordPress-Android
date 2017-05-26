package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AppInstallAd;
import com.soundcloud.android.ads.VideoAd;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackStateTransition;

import java.util.Date;

public abstract class AdPlaybackEvent {

    public enum Kind {
        OnScreen,
        ImageLoaded,
        NoVideoOnScreen,
        ToggleVolume,
        TogglePlayback,
        AdPlayStateTransition,
        AdProgressEvent
    }

    public abstract Kind kind();

    public abstract Date eventTime();

    public boolean forAppInstall() {
        return this instanceof InlayAdEvent && ((InlayAdEvent) this).getAd() instanceof AppInstallAd;
    }

    public boolean forVideoAd() {
        return (this instanceof InlayAdEvent && ((InlayAdEvent) this).getAd() instanceof VideoAd)
                || isKind(Kind.NoVideoOnScreen);
    }

    public boolean forStateTransition() {
        return isKind(Kind.AdPlayStateTransition);
    }

    public boolean forAdProgressEvent() {
        return isKind(Kind.AdProgressEvent);
    }

    public boolean isOnScreen() {
        return isKind(Kind.OnScreen);
    }

    public boolean isImageLoaded() {
        return isKind(Kind.ImageLoaded);
    }

    public boolean isToggleVolume() {
        return isKind(Kind.ToggleVolume);
    }

    public boolean isTogglePlayback() {
        return isKind(Kind.TogglePlayback);
    }

    private boolean isKind(Kind kind) {
       return kind() == kind;
    }

    @AutoValue
    public abstract static class InlayAdEvent extends AdPlaybackEvent {
        public abstract int getPosition();
        public abstract AdData getAd();

        private static InlayAdEvent create(Kind kind, int position, AdData ad, Date at) {
            return new AutoValue_AdPlaybackEvent_InlayAdEvent(kind, at, position, ad);
        }

        public static InlayAdEvent forOnScreen(int position, AdData ad, Date at) {
            return create(Kind.OnScreen, position, ad, at);
        }

        public static InlayAdEvent forImageLoaded(int position, AdData ad, Date at) {
            return create(Kind.ImageLoaded, position, ad, at);
        }

        public static InlayAdEvent forToggleVolume(int position, AdData ad, Date at) {
            return create(Kind.ToggleVolume, position, ad, at);
        }

        public static InlayAdEvent forTogglePlayback(int position, AdData ad, Date at) {
            return create(Kind.TogglePlayback, position, ad, at);
        }
    }

    @AutoValue
    public abstract static class NoVideoOnScreen extends AdPlaybackEvent {
        public abstract boolean shouldMute();

        public static NoVideoOnScreen create(Date at, boolean shouldMute) {
            return new AutoValue_AdPlaybackEvent_NoVideoOnScreen(Kind.NoVideoOnScreen, at, shouldMute);
        }
    }

    @AutoValue
    public abstract static class AdPlayStateTransition extends AdPlaybackEvent {
        public abstract VideoAd videoAd();
        public abstract PlaybackStateTransition stateTransition();
        public abstract boolean isMuted();

        public static AdPlayStateTransition create(VideoAd videoAd, PlaybackStateTransition transition, boolean isMuted, Date at) {
            return new AutoValue_AdPlaybackEvent_AdPlayStateTransition(Kind.AdPlayStateTransition, at, videoAd, transition, isMuted);
        }
    }

    @AutoValue
    public abstract static class AdProgressEvent extends AdPlaybackEvent {
        public abstract VideoAd videoAd();
        public abstract PlaybackProgress playbackProgress();

        public static AdProgressEvent create(VideoAd videoAd, PlaybackProgress progress, Date at) {
            return new AutoValue_AdPlaybackEvent_AdProgressEvent(Kind.AdProgressEvent, at, videoAd, progress);
        }
    }
}
