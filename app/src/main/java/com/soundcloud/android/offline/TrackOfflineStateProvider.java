package com.soundcloud.android.offline;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.rx.observers.DefaultSingleObserver;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

import android.annotation.SuppressLint;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.Map;

@Singleton
public class TrackOfflineStateProvider {

    private final TrackDownloadsStorage trackDownloadsStorage;
    private final EventBusV2 eventBus;
    private final Scheduler scheduler;
    private Map<Urn, OfflineState> offlineStates = Collections.emptyMap();
    @SuppressLint("sc.MissingCompositeDisposableRecycle")
    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Inject
    public TrackOfflineStateProvider(TrackDownloadsStorage trackDownloadsStorage, EventBusV2 eventBus,
                                     @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler) {
        this.trackDownloadsStorage = trackDownloadsStorage;
        this.eventBus = eventBus;
        this.scheduler = scheduler;
    }

    public void subscribe() {
        compositeDisposable.add(trackDownloadsStorage.getOfflineStates()
                                                     .subscribeOn(scheduler)
                                                     .observeOn(AndroidSchedulers.mainThread())
                                                     .subscribeWith(new StorageObserver()));
    }

    public void clear() {
        offlineStates.clear();
    }

    // NOTE: this is known to give a potentially incorrect state if the states have not loaded yet.
    // If this is a problem for a future usage, we should add an asychronous version
    public OfflineState getOfflineState(Urn track) {
        return offlineStates.containsKey(track) ? offlineStates.get(track) : OfflineState.NOT_OFFLINE;
    }

    private final class StorageObserver extends DefaultSingleObserver<Map<Urn, OfflineState>> {
        @Override
        public void onSuccess(Map<Urn, OfflineState> offlineStates) {
            TrackOfflineStateProvider.this.offlineStates = offlineStates;
            compositeDisposable.add(eventBus.subscribe(EventQueue.OFFLINE_CONTENT_CHANGED, new ContentChangedSubscriber()));
            super.onSuccess(offlineStates);
        }
    }

    private final class ContentChangedSubscriber extends DefaultObserver<OfflineContentChangedEvent> {
        @Override
        public void onNext(OfflineContentChangedEvent event) {
            for (Urn urn : event.entities) {
                if (urn.isTrack()) {
                    offlineStates.put(urn, event.state);
                }
            }
        }
    }
}
