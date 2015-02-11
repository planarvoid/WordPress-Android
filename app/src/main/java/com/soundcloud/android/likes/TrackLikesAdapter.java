package com.soundcloud.android.likes;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineContentEvent;
import com.soundcloud.android.lightcycle.DefaultFragmentLightCycle;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.tracks.SyncableTrackItemPresenter;
import com.soundcloud.android.tracks.TrackChangedSubscriber;
import com.soundcloud.android.tracks.TrackItemPresenter;
import com.soundcloud.android.view.adapters.EndlessAdapter;
import com.soundcloud.android.view.adapters.ListContentChangedSubscriber;
import com.soundcloud.android.view.adapters.ListContentSyncedSubscriber;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;
import rx.functions.Func1;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

class TrackLikesAdapter extends EndlessAdapter<PropertySet> {

    private final SyncableTrackItemPresenter trackPresenter;
    private final DefaultFragmentLightCycle lifeCycleHandler;
    private final Func1<OfflineContentEvent, Boolean> isTrackDownloadEvent = new Func1<OfflineContentEvent, Boolean>() {
        @Override
        public Boolean call(OfflineContentEvent offlineContentEvent) {
            return offlineContentEvent.getKind() == OfflineContentEvent.DOWNLOAD_FINISHED
                    || offlineContentEvent.getKind() == OfflineContentEvent.DOWNLOAD_STARTED
                    || offlineContentEvent.getKind() == OfflineContentEvent.STOP;
        }
    };

    private Subscription eventSubscriptions = Subscriptions.empty();

    @Inject
    public TrackLikesAdapter(final SyncableTrackItemPresenter trackPresenter, final EventBus eventBus) {
        super(trackPresenter);
        this.trackPresenter = trackPresenter;
        this.lifeCycleHandler = createLifeCycleHandler(trackPresenter, eventBus);
    }

    public TrackItemPresenter getTrackPresenter() {
        return trackPresenter;
    }

    public DefaultFragmentLightCycle getLifeCycleHandler() {
        return lifeCycleHandler;
    }

    private DefaultFragmentLightCycle createLifeCycleHandler(final TrackItemPresenter trackPresenter, final EventBus eventBus) {
        return new DefaultFragmentLightCycle(){
            @Override
            public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
                eventSubscriptions = new CompositeSubscription(
                        eventBus.subscribe(EventQueue.PLAY_QUEUE_TRACK, new TrackChangedSubscriber(TrackLikesAdapter.this, trackPresenter)),
                        eventBus.subscribe(EventQueue.PLAYABLE_CHANGED, new ListContentChangedSubscriber(TrackLikesAdapter.this)),
                        eventBus.subscribe(EventQueue.ENTITY_UPDATED, new ListContentSyncedSubscriber(TrackLikesAdapter.this)),
                        eventBus.queue(EventQueue.OFFLINE_CONTENT)
                                .filter(isTrackDownloadEvent)
                                .subscribe(new UpdateAdapterFromDownloadSubscriber(TrackLikesAdapter.this))
                );
            }

            @Override
            public void onDestroyView(Fragment fragment) {
                eventSubscriptions.unsubscribe();
            }
        };
    }

}
