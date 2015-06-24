package com.soundcloud.android.view.adapters;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.tracks.TrackItem;
import rx.Observable;

import android.content.Context;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;

public class MixedItemClickListener {

    private final PlaybackOperations playbackOperations;
    private final Provider<ExpandPlayerSubscriber> subscriberProvider;
    private final Navigator navigator;
    private final Screen screen;
    private final SearchQuerySourceInfo searchQuerySourceInfo;
    private final FeatureOperations featureOperations;

    public MixedItemClickListener(PlaybackOperations playbackOperations,
                                  Provider<ExpandPlayerSubscriber> subscriberProvider,
                                  FeatureOperations featureOperations,
                                  Navigator navigator,
                                  Screen screen,
                                  SearchQuerySourceInfo searchQuerySourceInfo) {
        this.playbackOperations = playbackOperations;
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
                playbackOperations
                        .playTracks(playables, item.getEntityUrn(), position, playSessionSource)
                        .subscribe(subscriberProvider.get());
            }
        } else {
            handleNonTrackItemClick(view, clickedItem.getEntityUrn());
        }
    }

    public void onItemClick(List<? extends ListItem> playables, View view, int position) {
        final Urn entityUrn = playables.get(position).getEntityUrn();
        if (entityUrn.isTrack()) {
            handleTrackClick(view.getContext(), playables, position);
        } else {
            handleNonTrackItemClick(view, entityUrn);
        }
    }

    private void handleNonTrackItemClick(View view, Urn entityUrn) {
        if (entityUrn.isPlaylist()) {
            navigator.openPlaylist(view.getContext(), entityUrn, screen, searchQuerySourceInfo);
        } else if (entityUrn.isUser()) {
            navigator.openProfile(view.getContext(), entityUrn, searchQuerySourceInfo);
        } else {
            throw new IllegalArgumentException("Unrecognized urn in " + this.getClass().getSimpleName() + ": " + entityUrn);
        }
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
            playbackOperations
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

        private final PlaybackOperations playbackOperations;
        private final Provider<ExpandPlayerSubscriber> subscriberProvider;
        private final FeatureOperations featureOperations;
        private final Navigator navigator;

        @Inject
        public Factory(PlaybackOperations playbackOperations,
                       Provider<ExpandPlayerSubscriber> subscriberProvider,
                       FeatureOperations featureOperations,
                       Navigator navigator) {
            this.playbackOperations = playbackOperations;
            this.subscriberProvider = subscriberProvider;
            this.featureOperations = featureOperations;
            this.navigator = navigator;
        }

        public MixedItemClickListener create(Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
            return new MixedItemClickListener(playbackOperations, subscriberProvider, featureOperations, navigator, screen, searchQuerySourceInfo);
        }
    }
}
