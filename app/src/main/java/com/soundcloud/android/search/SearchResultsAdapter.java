package com.soundcloud.android.search;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.User;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import com.soundcloud.android.view.adapters.PlaylistItemPresenter;
import com.soundcloud.android.view.adapters.PropertySetSourceProxyPresenter;
import com.soundcloud.android.view.adapters.TrackChangedSubscriber;
import com.soundcloud.android.view.adapters.TrackItemPresenter;
import com.soundcloud.android.view.adapters.UserItemPresenter;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import android.widget.ToggleButton;

import javax.inject.Inject;

class SearchResultsAdapter extends PagingItemAdapter<ScResource>
        implements FollowingOperations.FollowStatusChangedListener, UserItemPresenter.OnToggleFollowListener {

    static final int TYPE_USER = 0;
    static final int TYPE_TRACK = 1;
    static final int TYPE_PLAYLIST = 2;

    private final EventBus eventBus;
    private final FollowingOperations followingOperations;
    private final TrackItemPresenter trackPresenter;
    private final UserItemPresenter userPresenter;
    private Subscription eventSubscriptions = Subscriptions.empty();

    @Inject
    SearchResultsAdapter(UserItemPresenter userPresenter,
                         TrackItemPresenter trackPresenter,
                         PlaylistItemPresenter playlistPresenter,
                         FollowingOperations followingOperations, EventBus eventBus) {
        super(new CellPresenterEntity<ScResource>(TYPE_USER, new PropertySetSourceProxyPresenter(userPresenter, followingOperations)),
                new CellPresenterEntity<ScResource>(TYPE_TRACK, new PropertySetSourceProxyPresenter(trackPresenter, followingOperations)),
                new CellPresenterEntity<ScResource>(TYPE_PLAYLIST, new PropertySetSourceProxyPresenter(playlistPresenter, followingOperations)));
        this.eventBus = eventBus;
        this.followingOperations = followingOperations;
        this.trackPresenter = trackPresenter;
        this.userPresenter = userPresenter;
        followingOperations.requestUserFollowings(this);
        userPresenter.setToggleFollowListener(this);
    }

    @Override
    public int getItemViewType(int position) {
        int itemViewType = super.getItemViewType(position);
        if (itemViewType == IGNORE_ITEM_VIEW_TYPE) {
            return itemViewType;
        } else {
            final ScResource item = getItem(position);
            if (item instanceof User) {
                return TYPE_USER;
            } else if (item instanceof Track) {
                return TYPE_TRACK;
            } else if (item instanceof Playlist) {
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

    @Override
    public void onToggleFollowClicked(int position, ToggleButton toggleButton) {
        fireAndForget(followingOperations.toggleFollowing((User) getItem(position)));
    }

    @Override
    public void onFollowChanged() {
        notifyDataSetChanged();
    }

    void onViewCreated() {
        eventSubscriptions = new CompositeSubscription(
                eventBus.subscribe(EventQueue.PLAY_QUEUE, new TrackChangedSubscriber(this, trackPresenter)),
                eventBus.subscribe(EventQueue.PLAYABLE_CHANGED, new PlayableChangedSubscriber())
        );
    }

    void onDestroyView() {
        eventSubscriptions.unsubscribe();
    }

    private final class PlayableChangedSubscriber extends DefaultSubscriber<PlayableChangedEvent> {
        @Override
        public void onNext(final PlayableChangedEvent event) {
            final int index = Iterables.indexOf(items, new Predicate<ScResource>() {
                @Override
                public boolean apply(ScResource item) {
                    return item.getUrn().equals(event.getUrn());
                }
            });

            if (index > - 1) {
                items.set(index, event.getPlayable());
                notifyDataSetChanged();
            }
        }
    }

}
