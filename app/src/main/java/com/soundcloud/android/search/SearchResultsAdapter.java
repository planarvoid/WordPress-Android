package com.soundcloud.android.search;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.main.FragmentLifeCycle;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.view.adapters.EndlessAdapter;
import com.soundcloud.android.view.adapters.ListContentChangedSubscriber;
import com.soundcloud.android.view.adapters.PlaylistItemPresenter;
import com.soundcloud.android.view.adapters.TrackChangedSubscriber;
import com.soundcloud.android.view.adapters.TrackItemPresenter;
import com.soundcloud.android.view.adapters.UserItemPresenter;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

class SearchResultsAdapter extends EndlessAdapter<PropertySet> implements FragmentLifeCycle<Fragment> {

    static final int TYPE_USER = 0;
    static final int TYPE_TRACK = 1;
    static final int TYPE_PLAYLIST = 2;

    private final EventBus eventBus;
    private final TrackItemPresenter trackPresenter;
    private Subscription eventSubscriptions = Subscriptions.empty();

    @Inject
    SearchResultsAdapter(UserItemPresenter userPresenter,
                         TrackItemPresenter trackPresenter,
                         PlaylistItemPresenter playlistPresenter,
                         EventBus eventBus) {
        super(new CellPresenterEntity<>(TYPE_USER, userPresenter),
                new CellPresenterEntity<>(TYPE_TRACK, trackPresenter),
                new CellPresenterEntity<>(TYPE_PLAYLIST, playlistPresenter));
        this.eventBus = eventBus;
        this.trackPresenter = trackPresenter;
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
        final PropertySet item = getItem(position);
        return item.getOrElse(UserProperty.URN, PlayableProperty.URN);
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        eventSubscriptions = new CompositeSubscription(
                eventBus.subscribe(EventQueue.PLAY_QUEUE_TRACK, new TrackChangedSubscriber(this, trackPresenter)),
                // TODO: this is not gonna work because of the different Urn types
                eventBus.subscribe(EventQueue.PLAYABLE_CHANGED, new ListContentChangedSubscriber(this))
        );
    }

    @Override
    public void onDestroyView() {
        eventSubscriptions.unsubscribe();
    }

    @Override
    public void onBind(Fragment owner) {
        /* no-op */
    }

    @Override
    public void onCreate(@Nullable Bundle bundle) {
        /* no-op */
    }

    @Override
    public void onStart() {
        /* no-op */
    }

    @Override
    public void onResume() {
        /* no-op */
    }

    @Override
    public void onPause() {
        /* no-op */
    }

    @Override
    public void onStop() {
        /* no-op */
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        /* no-op */
    }

    @Override
    public void onRestoreInstanceState(Bundle bundle) {
        /* no-op */
    }

    @Override
    public void onDestroy() {
        /* no-op */
    }
}
