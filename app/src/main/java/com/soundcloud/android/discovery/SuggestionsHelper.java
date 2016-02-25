package com.soundcloud.android.discovery;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.search.suggestions.SuggestionsAdapter;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.Context;

import javax.inject.Provider;

/**
 * This class exists essentially to wrap the interface of SuggestionsAdapter (full of legacy code).
 * There is a task to rewrite it: https://soundcloud.atlassian.net/browse/SEARCH-417
 */
@AutoFactory(allowSubclasses = true)
class SuggestionsHelper {

    private final Navigator navigator;
    private final EventBus eventBus;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final PlaybackInitiator playbackInitiator;
    private final SuggestionsAdapter adapter;

    SuggestionsHelper(@Provided Navigator navigator,
                      @Provided EventBus eventBus,
                      @Provided Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                      @Provided PlaybackInitiator playbackInitiator,
                      SuggestionsAdapter suggestionsAdapter) {
        this.navigator = navigator;
        this.eventBus = eventBus;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.playbackInitiator = playbackInitiator;
        this.adapter = suggestionsAdapter;
    }

    void launchSuggestion(Context context, int position) {
        final Urn urn = adapter.getUrn(position);

        final SearchQuerySourceInfo searchQuerySourceInfo = getQuerySourceInfo(position);
        trackSuggestion(position, urn, searchQuerySourceInfo);

        if (urn.isTrack()) {
            playTrack(urn, searchQuerySourceInfo);
        } else {
            navigator.openProfile(context, urn, Screen.SEARCH_SUGGESTIONS, searchQuerySourceInfo);
        }
    }

    private void trackSuggestion(int position, Urn itemUrn, SearchQuerySourceInfo searchQuerySourceInfo) {
        final SearchEvent event = SearchEvent.searchSuggestion(
                itemUrn, adapter.isLocalResult(position), searchQuerySourceInfo);
        eventBus.publish(EventQueue.TRACKING, event);
    }

    private SearchQuerySourceInfo getQuerySourceInfo(int position) {
        SearchQuerySourceInfo searchQuerySourceInfo = null;
        Urn queryUrn = adapter.getQueryUrn(position);

        if (!queryUrn.equals(Urn.NOT_SET)) {
            searchQuerySourceInfo = new SearchQuerySourceInfo(queryUrn,
                    adapter.getQueryPosition(position),
                    adapter.getUrn(position));
        }

        return searchQuerySourceInfo;
    }

    private void playTrack(Urn urn, SearchQuerySourceInfo searchQuerySourceInfo) {
        playbackInitiator.startPlaybackWithRecommendations(urn, Screen.SEARCH_SUGGESTIONS, searchQuerySourceInfo)
                .subscribe(expandPlayerSubscriberProvider.get());
    }
}
