package com.soundcloud.android.stream;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayQueueEvent;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import com.soundcloud.android.view.adapters.PlaylistItemPresenter;
import com.soundcloud.android.view.adapters.TrackItemPresenter;
import com.soundcloud.propeller.PropertySet;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import javax.inject.Inject;

class SoundStreamAdapter extends PagingItemAdapter<PropertySet> {

    static final int TRACK_ITEM_TYPE = 0;
    static final int PLAYLIST_ITEM_TYPE = 1;

    private final EventBus eventBus;
    private Subscription eventSubscriptions = Subscriptions.empty();
    private TrackItemPresenter trackPresenter;

    @Inject
    SoundStreamAdapter(TrackItemPresenter trackPresenter, PlaylistItemPresenter playlistPresenter, EventBus eventBus) {
        super(new CellPresenterEntity<PropertySet>(TRACK_ITEM_TYPE, trackPresenter),
                new CellPresenterEntity<PropertySet>(PLAYLIST_ITEM_TYPE, playlistPresenter));
        this.eventBus = eventBus;
        init(trackPresenter);
    }

    private void init(TrackItemPresenter trackPresenter) {
        this.trackPresenter = trackPresenter;
    }

    @Override
    public int getItemViewType(int position) {
        final int itemViewType = super.getItemViewType(position);
        if (itemViewType == IGNORE_ITEM_VIEW_TYPE) {
            return itemViewType;
        } else if (getItem(position).get(PlayableProperty.URN) instanceof TrackUrn) {
            return TRACK_ITEM_TYPE;
        } else {
            return PLAYLIST_ITEM_TYPE;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    void onViewCreated() {
        eventSubscriptions = new CompositeSubscription(
                eventBus.subscribe(EventQueue.PLAY_QUEUE, new PlayQueueChangedSubscriber()),
                eventBus.subscribe(EventQueue.PLAYABLE_CHANGED, new PlayableChangedSubscriber())
        );
    }

    void onDestroyView() {
        eventSubscriptions.unsubscribe();
    }

    private final class PlayQueueChangedSubscriber extends DefaultSubscriber<PlayQueueEvent> {
        @Override
        public void onNext(PlayQueueEvent event) {
            if (event.getKind() == PlayQueueEvent.NEW_QUEUE || event.getKind() == PlayQueueEvent.TRACK_CHANGE) {
                trackPresenter.setPlayingTrack(event.getCurrentTrackUrn());
                notifyDataSetChanged();
            }
        }
    }

    private final class PlayableChangedSubscriber extends DefaultSubscriber<PlayableChangedEvent> {
        @Override
        public void onNext(final PlayableChangedEvent event) {
            final int index = Iterables.indexOf(items, new Predicate<PropertySet>() {
                @Override
                public boolean apply(PropertySet item) {
                    return item.get(PlayableProperty.URN).equals(event.getUrn());
                }
            });

            if (index > - 1) {
                items.get(index).merge(event.getChangeSet());
                notifyDataSetChanged();
            }
        }
    }
}
