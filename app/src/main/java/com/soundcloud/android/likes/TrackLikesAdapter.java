package com.soundcloud.android.likes;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineSyncEvent;
import com.soundcloud.android.lightcycle.DefaultFragmentLightCycle;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.tracks.SyncableTrackItemPresenter;
import com.soundcloud.android.tracks.TrackChangedSubscriber;
import com.soundcloud.android.tracks.TrackItemPresenter;
import com.soundcloud.android.view.adapters.EndlessAdapter;
import com.soundcloud.android.view.adapters.ListContentChangedSubscriber;
import com.soundcloud.android.view.adapters.ListContentSyncedSubscriber;
import com.soundcloud.android.view.adapters.ReactiveAdapter;
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

public class TrackLikesAdapter extends EndlessAdapter<PropertySet>
        implements ReactiveAdapter<Iterable<PropertySet>> {

    private final SyncableTrackItemPresenter trackPresenter;
    private final DefaultFragmentLightCycle lifeCycleHandler;
    private final Func1<OfflineSyncEvent, Boolean> isDownloadFinishedEvent = new Func1<OfflineSyncEvent, Boolean>() {
        @Override
        public Boolean call(OfflineSyncEvent offlineSyncEvent) {
            return offlineSyncEvent.getKind() == OfflineSyncEvent.DOWNLOAD_FINISHED;
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
                        eventBus.queue(EventQueue.OFFLINE_SYNC).filter(isDownloadFinishedEvent).subscribe(new UpdateAdapterFromDownloadSubscriber(TrackLikesAdapter.this))
                );
            }

            @Override
            public void onDestroyView(Fragment fragment) {
                eventSubscriptions.unsubscribe();
            }
        };
    }

}
