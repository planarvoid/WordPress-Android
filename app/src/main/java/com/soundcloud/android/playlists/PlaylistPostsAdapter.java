package com.soundcloud.android.playlists;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.presentation.PagingListItemAdapter;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.view.adapters.ReactiveAdapter;
import com.soundcloud.android.view.adapters.UpdateCurrentDownloadSubscriber;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

public class PlaylistPostsAdapter extends PagingListItemAdapter<PlaylistItem>
        implements ReactiveAdapter<Iterable<PlaylistItem>> {

    private final DefaultSupportFragmentLightCycle lifeCycleHandler;

    private Subscription eventSubscriptions = Subscriptions.empty();

    @Inject
    public PlaylistPostsAdapter(DownloadablePlaylistItemRenderer playlistRenderer,
                                final EventBus eventBus) {
        super(playlistRenderer);
        this.lifeCycleHandler = createLifeCycleHandler(eventBus);
    }

    private DefaultSupportFragmentLightCycle createLifeCycleHandler(final EventBus eventBus) {
        return new DefaultSupportFragmentLightCycle(){
            @Override
            public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
                eventSubscriptions = new CompositeSubscription(
                        eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(PlaylistPostsAdapter.this)),
                        eventBus.subscribe(EventQueue.CURRENT_DOWNLOAD, new UpdateCurrentDownloadSubscriber(PlaylistPostsAdapter.this))
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
