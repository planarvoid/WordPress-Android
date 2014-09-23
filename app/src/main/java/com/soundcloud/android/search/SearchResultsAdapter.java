package com.soundcloud.android.search;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.view.adapters.EndlessAdapter;
import com.soundcloud.android.view.adapters.ListContentChangedSubscriber;
import com.soundcloud.android.view.adapters.PlaylistItemPresenter;
import com.soundcloud.android.view.adapters.TrackChangedSubscriber;
import com.soundcloud.android.view.adapters.TrackItemPresenter;
import com.soundcloud.android.view.adapters.UserItemPresenter;
import com.soundcloud.propeller.PropertySet;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;

class SearchResultsAdapter extends EndlessAdapter<PropertySet> {

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
            final PropertySet item = getItem(position);
            if (item.contains(UserProperty.URN)) {
                return TYPE_USER;
            } else if (item.contains(TrackProperty.URN)) {
                return TYPE_TRACK;
            } else if (item.contains(PlaylistProperty.URN)) {
                return TYPE_PLAYLIST;
            } else {
                throw new IllegalStateException("Unexpected item type in " + SearchResultsAdapter.class.getSimpleName());
            }
        }
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    void onViewCreated() {
        eventSubscriptions = new CompositeSubscription(
                eventBus.subscribe(EventQueue.PLAY_QUEUE_TRACK, new TrackChangedSubscriber(this, trackPresenter)),
                // TODO: this is not gonna work because of the different Urn types
                eventBus.subscribe(EventQueue.PLAYABLE_CHANGED, new ListContentChangedSubscriber(this))
        );
    }

    void onDestroyView() {
        eventSubscriptions.unsubscribe();
    }
}
