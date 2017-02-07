package com.soundcloud.android.ads;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.InlayAdEvent;
import com.soundcloud.android.events.InlayAdImpressionEvent;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;
import java.lang.ref.WeakReference;
import java.util.Date;

class InlayAdOperations {

    private final EventBus eventBus;

    final Func1<InlayAdEvent, InlayAdImpressionEvent> TO_IMPRESSION = event -> {
        event.getAd().setImpressionReported();
        return InlayAdImpressionEvent.create(event.getAd(), event.getPosition(), event.getEventTime().getTime());
    };

    @Inject
    InlayAdOperations(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    Observable<InlayAdImpressionEvent> trackImpressions(InlayAdHelper helper) {
        return eventBus.queue(EventQueue.INLAY_AD)
                       .filter(new OnScreenAndImageLoaded(helper))
                       .map(TO_IMPRESSION);
    }

    static class OnScreenAndImageLoaded extends InlayAdEvent.Predicate {
        final WeakReference<InlayAdHelper> helperRef;

        OnScreenAndImageLoaded(InlayAdHelper helper) {
            this.helperRef = new WeakReference<>(helper);
        }

        @Override
        public Boolean call(InlayAdEvent event) {
            return !event.getAd().hasReportedImpression() && super.call(event);
        }

        @Override
        public Boolean whenOnScreen(InlayAdEvent.OnScreen event) {
            final Optional<Date> imageLoaded = event.getAd().getImageLoadTime();
            return imageLoaded.isPresent() && imageLoaded.get().before(event.getEventTime());
        }

        @Override
        public Boolean whenImageLoaded(InlayAdEvent.ImageLoaded event) {
            final InlayAdHelper helper = helperRef.get();
            return helper != null && helper.isOnScreen(event.getPosition());
        }
    }
}
