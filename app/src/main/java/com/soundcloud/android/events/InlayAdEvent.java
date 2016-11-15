package com.soundcloud.android.events;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.ads.AppInstallAd;

import java.util.Date;

import rx.functions.Func1;

public interface InlayAdEvent {
    int getPosition();
    AppInstallAd getAd();
    Date getEventTime();

    @AutoValue
    abstract class OnScreen implements InlayAdEvent {
        public static OnScreen create(int position, AppInstallAd ad, Date at) {
            return new AutoValue_InlayAdEvent_OnScreen(position, ad, at);
        }
    }

    @AutoValue
    abstract class ImageLoaded implements InlayAdEvent {
        public static ImageLoaded create(int position, AppInstallAd ad, Date at) {
            return new AutoValue_InlayAdEvent_ImageLoaded(position, ad, at);
        }
    }

    abstract class Func<Out> implements Func1<InlayAdEvent, Out> {
        @Override
        public Out call(InlayAdEvent event) {
            if (event instanceof OnScreen) {
                return whenOnScreen((OnScreen) event);
            } else if (event instanceof ImageLoaded) {
                return whenImageLoaded((ImageLoaded) event);
            } else {
                return otherwise(event);
            }
        }

        public abstract Out whenOnScreen(OnScreen event);
        public abstract Out whenImageLoaded(ImageLoaded event);

        public Out otherwise(InlayAdEvent event) {
            throw new IllegalArgumentException("Unhandled type for event: " + event);
        }
    }

    abstract class Predicate extends Func<Boolean> {
        public Boolean otherwise(InlayAdEvent event) {
            return false;
        }
    }
}
