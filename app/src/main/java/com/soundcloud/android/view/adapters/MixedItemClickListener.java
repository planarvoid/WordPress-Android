package com.soundcloud.android.view.adapters;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playlists.PromotedPlaylistItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.tracks.PromotedTrackItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import rx.Observable;

import android.content.Context;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

public class MixedItemClickListener {

    private final PlaybackInitiator playbackInitiator;
    private final Provider<ExpandPlayerSubscriber> subscriberProvider;
    private final Navigator navigator;
    private final Screen screen;
    private final SearchQuerySourceInfo searchQuerySourceInfo;
    private final FeatureOperations featureOperations;

    public MixedItemClickListener(PlaybackInitiator playbackInitiator,
                                  Provider<ExpandPlayerSubscriber> subscriberProvider,
                                  FeatureOperations featureOperations,
                                  Navigator navigator,
                                  Screen screen,
                                  SearchQuerySourceInfo searchQuerySourceInfo) {
        this.playbackInitiator = playbackInitiator;
        this.subscriberProvider = subscriberProvider;
        this.navigator = navigator;
        this.screen = screen;
        this.searchQuerySourceInfo = searchQuerySourceInfo;
        this.featureOperations = featureOperations;
    }

    public void onItemClick(Observable<List<Urn>> playables, View view, int position, ListItem clickedItem) {
        if (clickedItem.getEntityUrn().isTrack()) {
            final TrackItem item = (TrackItem) clickedItem;
            if (shouldShowUpsell(item)) {
                navigator.openUpgrade(view.getContext());
            } else {
                final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
                playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
                playbackInitiator
                        .playTracks(playables, item.getEntityUrn(), position, playSessionSource)
                        .subscribe(subscriberProvider.get());
            }
        } else {
            handleNonTrackItemClick(view, clickedItem);
        }
    }

    public void onPostClick(Observable<List<PropertySet>> playables, View view, int position, ListItem clickedItem) {
        if (clickedItem.getEntityUrn().isTrack()) {
            final TrackItem item = (TrackItem) clickedItem;
            if (shouldShowUpsell(item)) {
                navigator.openUpgrade(view.getContext());
            } else {
                final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
                playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
                if (clickedItem instanceof PromotedTrackItem) {
                    playSessionSource.setPromotedSourceInfo(PromotedSourceInfo.fromItem((PromotedTrackItem) clickedItem));
                }
                playbackInitiator
                        .playPosts(playables, item.getEntityUrn(), position, playSessionSource)
                        .subscribe(subscriberProvider.get());
            }
        } else {
            handleNonTrackItemClick(view, clickedItem);
        }
    }

    public void onItemClick(List<? extends ListItem> playables, View view, int position) {
        ListItem playable = playables.get(position);
        if (playable.getEntityUrn().isTrack()) {
            handleTrackClick(view.getContext(), playables, position);
        } else {
            handleNonTrackItemClick(view, playable);
        }
    }

    private void handleNonTrackItemClick(View view, ListItem item) {
        Urn entityUrn = item.getEntityUrn();
        if (entityUrn.isPlaylist()) {
            navigator.openPlaylist(view.getContext(), entityUrn, screen, searchQuerySourceInfo, promotedPlaylistInfo(item));
        } else if (entityUrn.isUser()) {
            navigator.openProfile(view.getContext(), entityUrn, screen, searchQuerySourceInfo);
        } else {
            throw new IllegalArgumentException("Unrecognized urn in " + this.getClass().getSimpleName() + ": " + entityUrn);
        }
    }

    private PromotedSourceInfo promotedPlaylistInfo(ListItem item) {
        return (item instanceof PromotedPlaylistItem) ? PromotedSourceInfo.fromItem((PromotedPlaylistItem) item) : null;
    }

    private void handleTrackClick(Context context, List<? extends ListItem> playables, int position) {
        final TrackItem item = (TrackItem) playables.get(position);
        if (shouldShowUpsell(item)) {
            navigator.openUpgrade(context);
        } else {
            final List<Urn> trackUrns = filterTracks(playables);
            final int adjustedPosition = filterTracks(playables.subList(0, position)).size();
            final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
            playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
            playbackInitiator
                    .playTracks(trackUrns, adjustedPosition, playSessionSource)
                    .subscribe(subscriberProvider.get());
        }
    }

    private List<Urn> filterTracks(List<? extends ListItem> data) {
        ArrayList<Urn> urns = new ArrayList<>(data.size());
        for (ListItem listItem : data) {
            if (listItem.getEntityUrn().isTrack()) {
                urns.add(listItem.getEntityUrn());
            }
        }
        return urns;
    }

    private boolean shouldShowUpsell(TrackItem item) {
        return item.isMidTier() && featureOperations.upsellMidTier();
    }

    public static class Factory {

        private final PlaybackInitiator playbackInitiator;
        private final Provider<ExpandPlayerSubscriber> subscriberProvider;
        private final FeatureOperations featureOperations;
        private final Navigator navigator;

        @Inject
        public Factory(PlaybackInitiator playbackInitiator,
                       Provider<ExpandPlayerSubscriber> subscriberProvider,
                       FeatureOperations featureOperations,
                       Navigator navigator) {
            this.playbackInitiator = playbackInitiator;
            this.subscriberProvider = subscriberProvider;
            this.featureOperations = featureOperations;
            this.navigator = navigator;
        }

        public MixedItemClickListener create(Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
            return new MixedItemClickListener(playbackInitiator, subscriberProvider, featureOperations, navigator, screen, searchQuerySourceInfo);
        }
    }
}
