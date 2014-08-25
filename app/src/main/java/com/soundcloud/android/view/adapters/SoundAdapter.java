package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.view.adapters.AdaptersUtils.filterPlayables;
import static com.soundcloud.android.view.adapters.AdaptersUtils.toTrackUrn;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.collections.ScBaseAdapter;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.api.legacy.model.behavior.PlayableHolder;
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
public class SoundAdapter extends ScBaseAdapter<PublicApiResource> {

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
    public void addItems(List<PublicApiResource> newItems) {
        super.addItems(newItems);
        this.propertySets.addAll(toPropertySets(newItems));
    }

    private List<PropertySet> toPropertySets(List<PublicApiResource> items) {
        ArrayList<PropertySet> propSets = new ArrayList<PropertySet>(items.size());
        for (PublicApiResource resource : items) {
            propSets.add(((PlayableHolder) resource).getPlayable().toPropertySet());
        }
        return propSets;
    }

    @Override
    public void updateItems(Map<Urn, PublicApiResource> updatedItems){
        for (int i = 0; i < propertySets.size(); i++) {
            final PropertySet originalPropertySet = propertySets.get(i);
            final Urn key = originalPropertySet.get(PlayableProperty.URN);
            if (updatedItems.containsKey(key)){
                propertySets.set(i, ((PlayableHolder) updatedItems.get(key)).getPlayable().toPropertySet());
            }
        }
        notifyDataSetChanged();
    }

    private boolean isTrack(int position) {
        return propertySets.get(position).get(PlayableProperty.URN) instanceof TrackUrn;
    }

    @Override
    public int handleListItemClick(Context context, int position, long id, Screen screen) {
        Playable playable = ((PlayableHolder) data.get(position)).getPlayable();
        if (playable instanceof PublicApiTrack) {
            if (Content.match(contentUri).isMine()) {
                playTrack(position, screen, contentUri);
            } else {
                playTrack(position, screen);
            }
        } else if (playable instanceof PublicApiPlaylist) {
            startPlaylistActivity(context, screen, (PublicApiPlaylist) playable);
        }
        return ItemClickResults.LEAVING;
    }

    private void startPlaylistActivity(Context context, Screen screen, PublicApiPlaylist playable) {
        PlaylistDetailActivity.start(context, playable.getUrn(), screen);
    }

    private void playTrack(int position, Screen screen) {
        final List<TrackUrn> trackUrns = toTrackUrn(filterPlayables(data));
        final int adjustedPosition = filterPlayables(data.subList(0, position)).size();
        playbackOperations.playTracks(trackUrns, adjustedPosition, new PlaySessionSource(screen));
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forExpandPlayer());
    }

    private void playTrack(int position, Screen screen, Uri streamUri) {
        List<TrackUrn> trackUrns = toTrackUrn(filterPlayables(data));
        int adjustedPosition = filterPlayables(data.subList(0, position)).size();
        TrackUrn initialTrack = trackUrns.get(adjustedPosition);
        playbackOperations.playFromUri(streamUri, adjustedPosition, initialTrack, new PlaySessionSource(screen), PlayerUIEvent.actionForExpandPlayer(eventBus));
    }

    @Override
    public void onViewCreated() {
        eventSubscriptions = new CompositeSubscription(
                eventBus.subscribe(EventQueue.PLAY_QUEUE_TRACK, new TrackChangedSubscriber(this, trackPresenter)),
                eventBus.subscribe(EventQueue.PLAYABLE_CHANGED, new PlayableChangedSubscriber())
        );
    }

    @Override
    public void onDestroyView() {
        eventSubscriptions.unsubscribe();
    }

    private final class PlayableChangedSubscriber extends DefaultSubscriber<PlayableUpdatedEvent> {
        @Override
        public void onNext(final PlayableUpdatedEvent event) {
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
