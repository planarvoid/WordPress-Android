package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.search.SearchPlayQueueFilter;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;
import io.reactivex.Observable;

import javax.inject.Inject;
import java.util.List;

public class SearchClickListener {
    private final EventTracker eventTracker;
    private final SearchPlayQueueFilter searchPlayQueueFilter;
    private final PlaybackInitiator playbackInitiator;
    private final EventBus eventBus;
    private final Navigator navigator;

    @Inject
    SearchClickListener(EventTracker eventTracker,
                        SearchPlayQueueFilter searchPlayQueueFilter,
                        PlaybackInitiator playbackInitiator,
                        EventBus eventBus,
                        Navigator navigator) {
        this.eventTracker = eventTracker;
        this.searchPlayQueueFilter = searchPlayQueueFilter;
        this.playbackInitiator = playbackInitiator;
        this.eventBus = eventBus;
        this.navigator = navigator;
    }

    ClickResultAction playlistClickToNavigateAction(ClickParams params) {
        return activity -> navigator.navigateTo(activity, NavigationTarget.forPlaylist(params.urn(),
                                                                             params.screen(),
                                                                             Optional.of(params.searchQuerySourceInfo()),
                                                                             Optional.absent(),
                                                                             Optional.of(params.uiEvent())));
    }

    ClickResultAction userClickToNavigateAction(ClickParams params) {
        return context -> navigator.navigateTo(context, NavigationTarget.forProfile(params.urn(), Optional.of(params.uiEvent()), Optional.of(params.screen()), Optional.absent()));
    }

    Observable<PlaybackResult> trackClickToPlaybackResult(TrackClickParams params) {
        return playbackInitiator.playPosts(searchPlayQueueFilter.correctQueue(params.playableItems(), params.playPosition()),
                                           params.clickParams().urn(),
                                           searchPlayQueueFilter.correctPosition(params.playPosition()),
                                           params.playSessionSource())
                                .doOnSuccess(__ -> eventTracker.trackSearch(SearchEvent.tapItemOnScreen(params.clickParams().screen(),
                                                                                                        params.clickParams().searchQuerySourceInfo(),
                                                                                                        params.clickParams().clickSource())))
                                .doOnSuccess(this::trackPlaybackSuccess).toObservable();
    }

    private void trackPlaybackSuccess(PlaybackResult playbackResult) {
        if (playbackResult.isSuccess()) {
            eventBus.publish(EventQueue.PLAYER_COMMAND, PlayerUICommand.expandPlayer());
        }
    }

    @AutoValue
    abstract static class ClickParams {
        abstract Urn urn();

        abstract String searchQuery();

        abstract Optional<Urn> queryUrn();

        abstract int position();

        abstract Module module();

        abstract Screen screen();

        abstract SearchEvent.ClickSource clickSource();

        static ClickParams create(Urn urn, String searchQuery, Optional<Urn> queryUrn, int position, Module module, Screen screen, SearchEvent.ClickSource clickSource) {
            return new AutoValue_SearchClickListener_ClickParams(urn, searchQuery, queryUrn, position, module, screen, clickSource);
        }

        SearchQuerySourceInfo searchQuerySourceInfo() {
            return new SearchQuerySourceInfo(queryUrn().or(Urn.NOT_SET), position(), urn(), searchQuery());
        }

        UIEvent uiEvent() {
            return UIEvent.fromNavigation(urn(), getEventContextMetadata());
        }

        private EventContextMetadata getEventContextMetadata() {
            return EventContextMetadata.builder().pageName(screen().get()).module(module()).build();
        }
    }

    @AutoValue
    abstract static class TrackClickParams {
        abstract ClickParams clickParams();

        abstract List<Urn> playableItems();

        private int playPosition() {
            return playableItems().indexOf(clickParams().urn());
        }

        PlaySessionSource playSessionSource() {

            final PlaySessionSource playSessionSource = new PlaySessionSource(clickParams().screen());
            playSessionSource.setSearchQuerySourceInfo(clickParams().searchQuerySourceInfo());
            return playSessionSource;
        }

        static TrackClickParams create(Urn urn,
                                       String searchQuery,
                                       Optional<Urn> queryUrn,
                                       int position,
                                       Module module,
                                       Screen screen,
                                       SearchEvent.ClickSource clickSource,
                                       List<Urn> playableItems) {
            return new AutoValue_SearchClickListener_TrackClickParams(ClickParams.create(urn, searchQuery, queryUrn, position, module, screen, clickSource), playableItems);
        }
    }
}
