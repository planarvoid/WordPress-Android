package com.soundcloud.android.view.adapters;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.AttributingActivity;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.LinkType;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlayableWithReposter;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import rx.Observable;

import android.content.Context;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MixedItemClickListener {

    private final PlaybackInitiator playbackInitiator;
    private final Provider<ExpandPlayerSubscriber> subscriberProvider;
    private final Navigator navigator;
    private final Screen screen;
    private final SearchQuerySourceInfo searchQuerySourceInfo;

    public MixedItemClickListener(PlaybackInitiator playbackInitiator,
                                  Provider<ExpandPlayerSubscriber> subscriberProvider,
                                  Navigator navigator,
                                  Screen screen,
                                  SearchQuerySourceInfo searchQuerySourceInfo) {
        this.playbackInitiator = playbackInitiator;
        this.subscriberProvider = subscriberProvider;
        this.navigator = navigator;
        this.screen = screen;
        this.searchQuerySourceInfo = searchQuerySourceInfo;
    }

    public void onItemClick(Observable<List<Urn>> playables, View view, int position, ListItem clickedItem) {
        if (clickedItem.getUrn().isTrack()) {
            final TrackItem item = (TrackItem) clickedItem;
            final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
            playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
            playbackInitiator
                    .playTracks(playables, item.getUrn(), position, playSessionSource)
                    .subscribe(subscriberProvider.get());
        } else {
            handleNonTrackItemClick(view.getContext(), clickedItem, Optional.<Module>absent());
        }
    }

    public void legacyOnPostClick(Observable<List<PlayableWithReposter>> playablesWithReposters, View view, int position, ListItem clickedItem) {
        onPostClick(playablesWithReposters,
                    view,
                    position,
                    clickedItem,
                    new PlaySessionSource(screen),
                    Optional.<Module>absent());
    }

    public void onPostClick(Observable<List<PlayableItem>> playables,
                            View view,
                            int position,
                            ListItem clickedItem,
                            Module module) {
        onPostClick(playables.map(items -> Lists.transform(items, PlayableWithReposter::from)),
                    view,
                    position,
                    clickedItem,
                    new PlaySessionSource(screen),
                    Optional.of(module));
    }

    public void onProfilePostClick(Observable<List<PlayableItem>> playables,
                                   View view,
                                   int position,
                                   ListItem clickedItem,
                                   Urn userUrn) {
        onPostClick(playables.map(items -> Lists.transform(items, PlayableWithReposter::from)),
                    view,
                    position,
                    clickedItem,
                    PlaySessionSource.forArtist(screen, userUrn),
                    Optional.absent());
    }

    private void onPostClick(Observable<List<PlayableWithReposter>> playablesWithReposters,
                             View view,
                             int position,
                             ListItem clickedItem,
                             PlaySessionSource playSessionSource,
                             Optional<Module> module) {
        if (clickedItem.getUrn().isTrack()) {
            final TrackItem item = (TrackItem) clickedItem;
            playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
            if (item.isPromoted()) {
                playSessionSource.setPromotedSourceInfo(PromotedSourceInfo.fromItem((TrackItem) clickedItem));
            }
            playbackInitiator
                    .playPosts(playablesWithReposters, item.getUrn(), position, playSessionSource)
                    .subscribe(subscriberProvider.get());
        } else {
            handleNonTrackItemClick(view.getContext(), clickedItem, module);
        }
    }

    public void onItemClick(List<? extends ListItem> playables, Context context, int position) {
        ListItem playable = playables.get(position);
        if (playable.getUrn().isTrack()) {
            handleTrackClick(playables, position);
        } else {
            handleNonTrackItemClick(context, playable, Optional.<Module>absent());
        }
    }

    public void onItemClick(ListItem playable, Context context) {
        if (playable.getUrn().isTrack()) {
            handleTrackClick(Collections.singletonList(playable), 0);
        } else {
            handleNonTrackItemClick(context, playable, Optional.<Module>absent());
        }
    }

    private void handleNonTrackItemClick(Context context, ListItem item, Optional<Module> module) {
        Urn entityUrn = item.getUrn();
        if (item instanceof PlayableItem) {
            navigator.openPlaylist(context,
                                   entityUrn,
                                   screen,
                                   searchQuerySourceInfo,
                                   promotedPlaylistInfo(item),
                                   UIEvent.fromNavigation(entityUrn, getEventContextMetadata((PlayableItem) item, module)));
        } else if (entityUrn.isPlaylist()) {
            navigator.legacyOpenPlaylist(context,
                                         entityUrn,
                                         screen,
                                         searchQuerySourceInfo,
                                         promotedPlaylistInfo(item));
        } else if (entityUrn.isUser()) {
            navigator.legacyOpenProfile(context, entityUrn, screen, searchQuerySourceInfo);
        } else {
            throw new IllegalArgumentException("Unrecognized urn [" + entityUrn + "] in " + this.getClass()
                                                                            .getSimpleName() + ": " + entityUrn);
        }
    }

    private EventContextMetadata getEventContextMetadata(PlayableItem item, Optional<Module> module) {
        final EventContextMetadata.Builder builder = EventContextMetadata.builder()
                                                                         .invokerScreen(ScreenElement.LIST.get())
                                                                         .contextScreen(screen.get())
                                                                         .pageName(screen.get())
                                                                         .attributingActivity(AttributingActivity.fromPlayableItem(
                                                                                 item))
                                                                         .linkType(LinkType.SELF);

        if (module.isPresent()) {
            builder.module(module.get());
        }
        return builder.build();
    }

    private PromotedSourceInfo promotedPlaylistInfo(ListItem item) {
        return (item instanceof PlayableItem && ((PlayableItem) item).isPromoted()) ? PromotedSourceInfo.fromItem((PlayableItem) item) : null;
    }

    private void handleTrackClick(List<? extends ListItem> playables, int position) {
        final List<Urn> trackUrns = filterTracks(playables);
        final int adjustedPosition = filterTracks(playables.subList(0, position)).size();
        final PlaySessionSource playSessionSource = new PlaySessionSource(screen);
        playSessionSource.setSearchQuerySourceInfo(searchQuerySourceInfo);
        playbackInitiator
                .playTracks(trackUrns, adjustedPosition, playSessionSource)
                .subscribe(subscriberProvider.get());
    }

    private List<Urn> filterTracks(List<? extends ListItem> data) {
        ArrayList<Urn> urns = new ArrayList<>(data.size());
        for (ListItem listItem : data) {
            if (listItem.getUrn().isTrack()) {
                urns.add(listItem.getUrn());
            }
        }
        return urns;
    }

    public static class Factory {

        private final PlaybackInitiator playbackInitiator;
        private final Provider<ExpandPlayerSubscriber> subscriberProvider;
        private final Navigator navigator;

        @Inject
        public Factory(PlaybackInitiator playbackInitiator,
                       Provider<ExpandPlayerSubscriber> subscriberProvider,
                       Navigator navigator) {
            this.playbackInitiator = playbackInitiator;
            this.subscriberProvider = subscriberProvider;
            this.navigator = navigator;
        }

        public MixedItemClickListener create(Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
            return new MixedItemClickListener(playbackInitiator,
                                              subscriberProvider,
                                              navigator,
                                              screen,
                                              searchQuerySourceInfo);
        }
    }
}
