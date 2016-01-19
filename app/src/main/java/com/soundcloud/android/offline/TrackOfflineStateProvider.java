package com.soundcloud.android.offline;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineContentChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.Map;

public class TrackOfflineStateProvider {

    private final TrackDownloadsStorage trackDownloadsStorage;
    private final EventBus eventBus;
    private final Scheduler scheduler;
    private Map<Urn, OfflineState> offlineStates = Collections.emptyMap();

    @Inject
    public TrackOfflineStateProvider(TrackDownloadsStorage trackDownloadsStorage, EventBus eventBus,
                                     @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.trackDownloadsStorage = trackDownloadsStorage;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
    }

    public void subscribe(){
        trackDownloadsStorage.getOfflineStates()
                .subscribeOn(scheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new StorageSubscriber());
    }

    public void clear(){
        offlineStates.clear();
    }

    // NOTE: this is known to give a potentially incorrect state if the states have not loaded yet.
    // If this is a problem for a future usage, we should add an asychronous version
    public OfflineState getOfflineState(Urn track) {
        return offlineStates.containsKey(track) ? offlineStates.get(track) : OfflineState.NOT_OFFLINE;
    }

    private final class StorageSubscriber extends DefaultSubscriber<Map<Urn, OfflineState>> {
        @Override
        public void onNext(Map<Urn, OfflineState> offlineStates) {
            TrackOfflineStateProvider.this.offlineStates = offlineStates;
        }

        @Override
        public void onCompleted() {
            eventBus.subscribe(EventQueue.OFFLINE_CONTENT_CHANGED, new ContentChangedSubscriber());
        }
    }

    private final class ContentChangedSubscriber extends DefaultSubscriber<OfflineContentChangedEvent> {
        @Override
        public void onNext(OfflineContentChangedEvent args) {
            for (Urn urn : args.entities) {
                if (urn.isTrack()) {
                    offlineStates.put(urn, args.kind);
                }
            }
        }
    }
}
