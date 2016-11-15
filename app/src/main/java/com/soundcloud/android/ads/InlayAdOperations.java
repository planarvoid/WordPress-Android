package com.soundcloud.android.ads;

import android.support.v7.widget.StaggeredGridLayoutManager;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.InlayAdEvent;
import com.soundcloud.android.events.InlayAdImpressionEvent;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import java.util.Date;

import javax.inject.Inject;

import rx.Observable;
import rx.functions.Func1;

class InlayAdOperations {
    private final EventBus eventBus;
    private final InlayAdHelper helper;

    final Func1<InlayAdEvent, InlayAdImpressionEvent> TO_IMPRESSION = new Func1<InlayAdEvent, InlayAdImpressionEvent>() {
        @Override
        public InlayAdImpressionEvent call(InlayAdEvent event) {
            event.getAd().setImpressionReported();

            return new InlayAdImpressionEvent(event.getAd(),
                    event.getPosition(),
                    event.getEventTime().getTime());
        }
    };

    @Inject
    InlayAdOperations(EventBus eventBus, InlayAdHelper helper) {
        this.eventBus = eventBus;
        this.helper = helper;
    }

    public Observable<InlayAdImpressionEvent> trackImpressions(StaggeredGridLayoutManager layoutManager) {
        return eventBus.queue(EventQueue.INLAY_AD)
                       .filter(new OnScreenAndImageLoaded(layoutManager, helper))
                       .map(TO_IMPRESSION);
    }

    static class OnScreenAndImageLoaded extends InlayAdEvent.Predicate {
        final StaggeredGridLayoutManager layoutManager;
        final InlayAdHelper helper;

        OnScreenAndImageLoaded(StaggeredGridLayoutManager layoutManager, InlayAdHelper helper) {
            this.layoutManager = layoutManager;
            this.helper = helper;
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
            return helper.isOnScreen(event.getPosition());
        }
    };
}
