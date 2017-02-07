package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.ads.AdData;
import com.soundcloud.android.ads.AppInstallAd;
import com.soundcloud.android.ads.VideoAd;

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
        return (this instanceof WithAdData && ((WithAdData) this).getAd() instanceof VideoAd) || this instanceof NoVideoOnScreen;
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
        public static NoVideoOnScreen create(Date at) {
            return new AutoValue_InlayAdEvent_NoVideoOnScreen(at);
        }
    }
}
