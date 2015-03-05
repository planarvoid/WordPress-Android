package com.soundcloud.android.playlists;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.lightcycle.DefaultSupportFragmentLightCycle;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.android.view.adapters.PlaylistItemPresenter;
import com.soundcloud.android.view.adapters.ReactiveAdapter;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

public class PlaylistLikesAdapter extends PagingItemAdapter<PropertySet>
        implements ReactiveAdapter<Iterable<PropertySet>> {

    private final DefaultSupportFragmentLightCycle lifeCycleHandler;
    private Subscription eventSubscriptions = Subscriptions.empty();

    @Inject
    public PlaylistLikesAdapter(PlaylistItemPresenter playlistPresenter, final EventBus eventBus) {
        super(playlistPresenter);
        this.lifeCycleHandler = createLifeCycleHandler(eventBus);
        playlistPresenter.allowOfflineOptions();
    }

    private DefaultSupportFragmentLightCycle createLifeCycleHandler(final EventBus eventBus) {
        return new DefaultSupportFragmentLightCycle(){
            @Override
            public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
                eventSubscriptions = new CompositeSubscription(
                        eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(PlaylistLikesAdapter.this))
                );
            }

            @Override
            public void onDestroyView(Fragment fragment) {
                eventSubscriptions.unsubscribe();
            }
        };
    }

    public DefaultSupportFragmentLightCycle getLifeCycleHandler() {
        return lifeCycleHandler;
    }
}
