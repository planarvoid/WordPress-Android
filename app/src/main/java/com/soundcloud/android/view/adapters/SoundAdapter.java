package com.soundcloud.android.view.adapters;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.collections.ScBaseAdapter;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.behavior.PlayableHolder;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.propeller.PropertySet;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Temporarily used to adapt ScListFragment lists that use public API models to PropertySets and the new cell design
 */
public class SoundAdapter extends ScBaseAdapter<ScResource> {

    private static final int TRACK_VIEW_TYPE = 0;
    private static final int PLAYLIST_VIEW_TYPE = 1;

    @Inject PlaybackOperations playbackOperations;
    @Inject TrackItemPresenter trackPresenter;
    @Inject PlaylistItemPresenter playlistPresenter;
    @Inject EventBus eventBus;

    private Subscription eventSubscriptions = Subscriptions.empty();

    private final List<PropertySet> propertySets = new ArrayList<PropertySet>(Consts.LIST_PAGE_SIZE);

    @Deprecated
    public SoundAdapter(Uri uri) {
        super(uri);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    SoundAdapter(Uri uri, PlaybackOperations playbackOperations, TrackItemPresenter trackPresenter,
                 PlaylistItemPresenter playlistPresenter, EventBus eventBus) {
        super(uri);
        this.playbackOperations = playbackOperations;
        this.trackPresenter = trackPresenter;
        this.playlistPresenter = playlistPresenter;
        this.eventBus = eventBus;
    }

    @Override
    public int getItemViewType(int position) {
        int itemViewType = super.getItemViewType(position);
        if (itemViewType == Adapter.IGNORE_ITEM_VIEW_TYPE) {
            return itemViewType;
        } else {
            return isTrack(position) ? TRACK_VIEW_TYPE : PLAYLIST_VIEW_TYPE;
        }
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    protected View createRow(Context context, int position, ViewGroup parent) {
        if (isTrack(position)) {
            return trackPresenter.createItemView(position, parent);
        } else {
            // We assume this is a playlist
            return playlistPresenter.createItemView(position, parent);
        }
    }

    @Override
    protected void bindRow(int position, View rowView) {
        if (isTrack(position)) {
            trackPresenter.bindItemView(position, rowView, propertySets);
        } else {
            // We assume this is a playlist
            playlistPresenter.bindItemView(position, rowView, propertySets);
        }
    }

    @Override
    public void clearData() {
        super.clearData();
        this.propertySets.clear();
    }

    @Override
    public void addItems(List<ScResource> newItems) {
        super.addItems(newItems);
        this.propertySets.addAll(toPropertySets(newItems));
    }

    @Override
    public void updateItems(Map<Urn, ScResource> updatedItems){
        for (int i = 0; i < propertySets.size(); i++) {
            final Urn key = propertySets.get(i).get(PlayableProperty.URN);
            if (updatedItems.containsKey(key)){
                propertySets.set(i, ((Playable) updatedItems.get(key)).toPropertySet());
            }
        }
        notifyDataSetChanged();
    }

    private boolean isTrack(int position) {
        return propertySets.get(position).get(PlayableProperty.URN) instanceof TrackUrn;
    }

    private List<PropertySet> toPropertySets(List<ScResource> items) {
        final List<PropertySet> propertySets = new ArrayList<PropertySet>(items.size());
        for (ScResource item : items) {
            PlayableHolder playableHolder = (PlayableHolder) item;
            propertySets.add(playableHolder.getPlayable().toPropertySet());
        }
        return propertySets;
    }

    @Override
    public int handleListItemClick(Context context, int position, long id, Screen screen) {
        Uri streamUri = Content.match(contentUri).isMine() ? contentUri : null;
        playbackOperations.playFromAdapter(context, data, position, streamUri, screen);
        return ItemClickResults.LEAVING;
    }

    @Override
    public void onViewCreated() {
        eventSubscriptions = new CompositeSubscription(
                eventBus.subscribe(EventQueue.PLAY_QUEUE, new TrackChangedSubscriber(this, trackPresenter)),
                eventBus.subscribe(EventQueue.PLAYABLE_CHANGED, new PlayableChangedSubscriber())
        );
    }

    @Override
    public void onDestroyView() {
        eventSubscriptions.unsubscribe();
    }

    private final class PlayableChangedSubscriber extends DefaultSubscriber<PlayableChangedEvent> {
        @Override
        public void onNext(final PlayableChangedEvent event) {
            final int index = Iterables.indexOf(propertySets, new Predicate<PropertySet>() {
                @Override
                public boolean apply(PropertySet item) {
                    return item.get(PlayableProperty.URN).equals(event.getUrn());
                }
            });

            if (index > -1) {
                propertySets.get(index).merge(event.getChangeSet());
                notifyDataSetChanged();
            }
        }
    }
}
