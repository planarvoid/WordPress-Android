package com.soundcloud.android.view.adapters;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.behavior.PlayableHolder;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.propeller.PropertySet;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

/**
 * Temporarily used to adapt ScListFragment lists that use public API models to PropertySets and the new cell design
 */
@Deprecated
public class SoundAdapter extends LegacyAdapterBridge<PublicApiResource> {

    private static final int TRACK_VIEW_TYPE = 0;
    private static final int PLAYLIST_VIEW_TYPE = 1;

    @Inject PlaybackOperations playbackOperations;
    @Inject TrackItemRenderer trackRenderer;
    @Inject PlaylistItemRenderer playlistRenderer;
    @Inject EventBus eventBus;
    @Inject Provider<ExpandPlayerSubscriber> subscriberProvider;

    private Subscription eventSubscriptions = Subscriptions.empty();

    @Deprecated
    public SoundAdapter(Uri uri) {
        super(uri);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    SoundAdapter(Uri uri, PlaybackOperations playbackOperations, TrackItemRenderer trackRenderer,
                 PlaylistItemRenderer playlistRenderer, EventBus eventBus, Provider<ExpandPlayerSubscriber> subscriberProvider) {
        super(uri);
        this.playbackOperations = playbackOperations;
        this.trackRenderer = trackRenderer;
        this.playlistRenderer = playlistRenderer;
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
            return playlistRenderer.createItemView(parent);
        }
    }

    @Override
    protected void bindRow(int position, View rowView) {
        if (isTrack(position)) {
            trackRenderer.bindItemView(position, rowView, (List) listItems);
        } else {
            // We assume this is a playlist
            playlistRenderer.bindItemView(position, rowView, (List) listItems);
        }
    }

    @Override
    protected List<PlayableItem> toPresentationModels(List<PublicApiResource> items) {
        ArrayList<PlayableItem> models = new ArrayList<>(items.size());

        List<Urn> tracksMissingTitles = new ArrayList<>();
        for (PublicApiResource resource : items) {
            Playable playable = ((PlayableHolder) resource).getPlayable();
            if (playable.getTitle() == null) {
                tracksMissingTitles.add(playable.getUrn());
            }
            models.add(playable.getUrn().isTrack()
                    ? TrackItem.from(toPropertySet(resource))
                    : PlaylistItem.from(toPropertySet(resource)));
        }

        if (!tracksMissingTitles.isEmpty()) {
            final String message = String.format("Invalid collection tracks found from uri : %s , tracks : %s",
                    contentUri, TextUtils.join(", ", tracksMissingTitles));
            ErrorUtils.handleSilentException(new IllegalStateException(message));
        }

        return models;
    }

    @Override
    protected PropertySet toPropertySet(PublicApiResource item) {
        Playable playable = ((PlayableHolder) item).getPlayable();
        return playable.toPropertySet();
    }

    @Override
    public int handleListItemClick(Context context, int position, long id, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        Playable playable = ((PlayableHolder) data.get(position)).getPlayable();
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

    private void startPlaylistActivity(Context context, Screen screen, PublicApiPlaylist playable, SearchQuerySourceInfo searchQuerySourceInfo) {
        PlaylistDetailActivity.start(context, playable.getUrn(), screen, searchQuerySourceInfo);
    }

    private void playTrack(int position, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        final List<Urn> trackUrns = toTrackUrn(filterPlayables(data));
        final int adjustedPosition = filterPlayables(data.subList(0, position)).size();
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
        playbackOperations
                .playTracks(trackUrns, adjustedPosition, playSessionSource)
                .subscribe(subscriberProvider.get());
    }

    private void playTrack(int position, Screen screen, Uri streamUri, SearchQuerySourceInfo searchQuerySourceInfo) {
        List<Urn> trackUrns = toTrackUrn(filterPlayables(data));
        int adjustedPosition = filterPlayables(data.subList(0, position)).size();
        Urn initialTrack = trackUrns.get(adjustedPosition);
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
        playbackOperations
                .playTracksFromUri(streamUri, adjustedPosition, initialTrack, playSessionSource)
                .subscribe(subscriberProvider.get());
    }

    @Override
    public void onViewCreated() {
        eventSubscriptions = new CompositeSubscription(
                eventBus.subscribe(EventQueue.PLAY_QUEUE_TRACK, new UpdatePlayingTrackSubscriber(this, trackRenderer)),
                eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new PlayableChangedSubscriber())
        );
    }

    @Override
    public void onDestroyView() {
        eventSubscriptions.unsubscribe();
    }
}
