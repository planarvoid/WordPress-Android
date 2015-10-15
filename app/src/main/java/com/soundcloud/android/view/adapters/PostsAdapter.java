package com.soundcloud.android.view.adapters;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.storage.provider.ScContentProvider;
import com.soundcloud.android.tracks.LegacyUpdatePlayingTrackSubscriber;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

/**
 * Temporarily used to adapt ScListFragment lists that use public API models to PropertySets and the new cell design
 */
@Deprecated
public class PostsAdapter extends LegacyAdapterBridge<SoundAssociation> {

    private static final int TRACK_VIEW_TYPE = 0;
    private static final int PLAYLIST_VIEW_TYPE = 1;

    private final String relatedUsername;

    @Inject PlaybackInitiator playbackInitiator;
    @Inject TrackItemRenderer trackRenderer;
    @Inject PlaylistItemRenderer playlistPresenter;
    @Inject EventBus eventBus;
    @Inject Provider<ExpandPlayerSubscriber> subscriberProvider;

    private Subscription eventSubscriptions = RxUtils.invalidSubscription();

    @Deprecated
    public PostsAdapter(Uri uri, String relatedUsername) {
        super(uri);
        this.relatedUsername = relatedUsername;
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    PostsAdapter(Uri uri, String relatedUsername, PlaybackInitiator playbackInitiator, TrackItemRenderer trackRenderer,
                 PlaylistItemRenderer playlistRenderer, EventBus eventBus, Provider<ExpandPlayerSubscriber> subscriberProvider) {
        super(uri);
        this.relatedUsername = relatedUsername;
        this.playbackInitiator = playbackInitiator;
        this.trackRenderer = trackRenderer;
        this.playlistPresenter = playlistRenderer;
        this.eventBus = eventBus;
        this.subscriberProvider = subscriberProvider;
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
            return trackRenderer.createItemView(parent);
        } else {
            // We assume this is a playlist
            return playlistPresenter.createItemView(parent);
        }
    }

    @Override
    protected void bindRow(int position, View rowView) {
        if (isTrack(position)) {
            trackRenderer.bindItemView(position, rowView, (List) listItems);
        } else {
            // We assume this is a playlist
            playlistPresenter.bindItemView(position, rowView, (List) listItems);
        }
    }

    @Override
    protected PropertySet toPropertySet(SoundAssociation resource) {
        PropertySet propertySet = resource.getPlayable().toPropertySet();
        if (resource.associationType == ScContentProvider.CollectionItemTypes.REPOST) {
            propertySet.put(PlayableProperty.REPOSTER, relatedUsername);
        }
        return propertySet;
    }

    @Override
    public int handleListItemClick(Context context, int position, long id, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        Playable playable = data.get(position).getPlayable();

        if (playable instanceof PublicApiTrack) {
            if (Content.match(contentUri).isMine()) {
                playTrack(position, screen, contentUri, searchQuerySourceInfo);
            } else {
                playTrack(position, screen, searchQuerySourceInfo);
            }
        } else if (playable instanceof PublicApiPlaylist) {
            startPlaylistActivity(context, screen, (PublicApiPlaylist) playable, searchQuerySourceInfo);
        }
        return ItemClickResults.LEAVING;
    }

    private void playTrack(int position, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        final List<Urn> trackUrns = toTrackUrn(filterPlayables(data));
        final int adjustedPosition = filterPlayables(data.subList(0, position)).size();
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen);

        playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);

        playbackInitiator
                .playTracks(trackUrns, adjustedPosition, playSessionSource)
                .subscribe(subscriberProvider.get());
    }

    private void playTrack(int position, Screen screen, Uri streamUri, SearchQuerySourceInfo searchQuerySourceInfo) {
        List<Urn> trackUrns = toTrackUrn(filterPlayables(data));
        int adjustedPosition = filterPlayables(data.subList(0, position)).size();
        Urn initialTrack = trackUrns.get(adjustedPosition);
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen);

        playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);

        playbackInitiator
                .playTracksFromUri(streamUri, adjustedPosition, initialTrack, playSessionSource)
                .subscribe(subscriberProvider.get());
    }

    private void startPlaylistActivity(Context context, Screen screen, PublicApiPlaylist playable, SearchQuerySourceInfo searchQuerySourceInfo) {
        PlaylistDetailActivity.start(context, playable.getUrn(), screen, searchQuerySourceInfo);
    }

    @Override
    public void onViewCreated() {
        eventSubscriptions = new CompositeSubscription(
                eventBus.subscribe(EventQueue.PLAY_QUEUE_TRACK, new LegacyUpdatePlayingTrackSubscriber(this, trackRenderer)),
                eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new PlayableChangedSubscriber())
        );
    }

    @Override
    public void onDestroyView() {
        eventSubscriptions.unsubscribe();
    }
}
