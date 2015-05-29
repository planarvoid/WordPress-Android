package com.soundcloud.android.search;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.view.adapters.CellRendererBinding;
import com.soundcloud.android.view.adapters.UserItemRenderer;
import com.soundcloud.lightcycle.SupportFragmentLightCycle;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import com.soundcloud.android.view.adapters.PlaylistItemRenderer;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;

class SearchResultsAdapter extends PagingItemAdapter<ListItem> implements SupportFragmentLightCycle<Fragment> {

    static final int TYPE_USER = 0;
    static final int TYPE_TRACK = 1;
    static final int TYPE_PLAYLIST = 2;

    private final EventBus eventBus;
    private final TrackItemRenderer trackRenderer;
    private Subscription eventSubscriptions = Subscriptions.empty();

    @Inject
    SearchResultsAdapter(UserItemRenderer userRenderer,
                         TrackItemRenderer trackRenderer,
                         PlaylistItemRenderer playlistRenderer,
                         EventBus eventBus) {
        super(new CellRendererBinding<>(TYPE_USER, userRenderer),
                new CellRendererBinding<>(TYPE_TRACK, trackRenderer),
                new CellRendererBinding<>(TYPE_PLAYLIST, playlistRenderer));
        this.eventBus = eventBus;
        this.trackRenderer = trackRenderer;
    }

    @Override
    public int getItemViewType(int position) {
        int itemViewType = super.getItemViewType(position);
        if (itemViewType == IGNORE_ITEM_VIEW_TYPE) {
            return itemViewType;
        } else {
            final Urn urn = getUrn(position);
            if (urn.isUser()) {
                return TYPE_USER;
            } else if (urn.isTrack()) {
                return TYPE_TRACK;
            } else if (urn.isPlaylist()) {
                return TYPE_PLAYLIST;
            } else {
                throw new IllegalStateException("Unexpected item type in " + SearchResultsAdapter.class.getSimpleName());
            }
        }
    }

    private Urn getUrn(int position) {
        final ListItem item = getItem(position);
        return item.getEntityUrn();
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        eventSubscriptions = new CompositeSubscription(
                eventBus.subscribe(EventQueue.PLAY_QUEUE_TRACK, new UpdatePlayingTrackSubscriber(this, trackRenderer)),
                eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(this))
        );
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        eventSubscriptions.unsubscribe();
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        /* no-op */
    }

    @Override
    public void onStart(Fragment fragment) {
        /* no-op */
    }

    @Override
    public void onResume(Fragment fragment) {
        /* no-op */
    }

    @Override
    public boolean onOptionsItemSelected(Fragment fragment, MenuItem item) {
        return false;
    }

    @Override
    public void onPause(Fragment fragment) {
        /* no-op */
    }

    @Override
    public void onStop(Fragment fragment) {
        /* no-op */
    }

    @Override
    public void onSaveInstanceState(Fragment fragment, Bundle bundle) {
        /* no-op */
    }

    @Override
    public void onRestoreInstanceState(Fragment fragment, Bundle bundle) {
        /* no-op */
    }

    @Override
    public void onDestroy(Fragment fragment) {
        /* no-op */
    }
}
