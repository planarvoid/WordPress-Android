package com.soundcloud.android.search;

import static com.soundcloud.java.checks.Preconditions.checkState;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import javax.inject.Inject;
import java.util.List;
import java.util.Random;

public class PlayFromVoiceSearchPresenter extends DefaultActivityLightCycle<AppCompatActivity> {

    private static final String ANDROID_INTENT_EXTRA_GENRE = "android.intent.extra.genre";

    private final SearchOperations searchOperations;
    private final PlaybackInitiator playbackInitiator;
    private final Random random;
    private final PlaybackToastHelper playbackToastHelper;
    private final Navigator navigator;
    private final EventBus eventBus;
    private Context activityContext;

    private final Func1<SearchResult, Observable<PlaybackResult>> toPlayWithRecommendations = new Func1<SearchResult, Observable<PlaybackResult>>() {
        @Override
        public Observable<PlaybackResult> call(SearchResult searchResult) {
            List<PropertySet> items = searchResult.getItems();
            checkState(!items.isEmpty(), "There is no result for this search");
            return playbackInitiator.playTrackWithRecommendationsLegacy(items.get(0).get(TrackProperty.URN),
                                                                        new PlaySessionSource(Screen.VOICE_COMMAND));
        }
    };

    private final Func1<SearchResult, PropertySet> toRandomSearchResultItem = new Func1<SearchResult, PropertySet>() {
        @Override
        public PropertySet call(SearchResult searchResult) {
            List<PropertySet> items = searchResult.getItems();
            checkState(!items.isEmpty(), "There is no result for this search");
            return items.get(random.nextInt(items.size()));
        }
    };

    @Inject
    PlayFromVoiceSearchPresenter(SearchOperations searchOperations,
                                 PlaybackInitiator playbackInitiator,
                                 Random random,
                                 PlaybackToastHelper playbackToastHelper,
                                 Navigator navigator,
                                 EventBus eventBus) {
        this.searchOperations = searchOperations;
        this.playbackInitiator = playbackInitiator;
        this.random = random;
        this.playbackToastHelper = playbackToastHelper;
        this.navigator = navigator;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        activityContext = activity;
    }

    @Override
    public void onResume(AppCompatActivity activity) {
        Intent intent = activity.getIntent();
        if (MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH.equals(intent.getAction())) {
            activity.findViewById(R.id.progress).setVisibility(View.VISIBLE);

            final String mediaFocus = intent.getStringExtra(MediaStore.EXTRA_MEDIA_FOCUS);
            if (mediaFocus != null && MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE.equals(mediaFocus)) {
                playPlaylist(getGenreKeyCompat(intent));
            } else {
                playTrackFromQuery(intent.getStringExtra(SearchManager.QUERY));
            }

        } else {
            activity.finish();
        }
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        activityContext = null;
    }

    private String getGenreKeyCompat(Intent intent) {
        return intent.getExtras().containsKey(ANDROID_INTENT_EXTRA_GENRE) ?
               intent.getStringExtra(ANDROID_INTENT_EXTRA_GENRE) :
               intent.getStringExtra(SearchManager.QUERY);
    }

    private void playTrackFromQuery(final String query) {
        searchOperations
                .searchResult(query, Optional.<Urn>absent(), SearchType.TRACKS)
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(toPlayWithRecommendations)
                .subscribe(new PlayFromQuerySubscriber(eventBus, playbackToastHelper, query));
    }

    private void playPlaylist(final String query) {
        searchOperations
                .searchResult(query, Optional.<Urn>absent(), SearchType.PLAYLISTS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(toRandomSearchResultItem)
                .subscribe(new PlayFromPlaylistSubscriber(query));
    }

    private void fallbackToSearch(String query) {
        navigator.performSearch(activityContext, query);
    }

    private class PlayFromQuerySubscriber extends ExpandPlayerSubscriber {
        private final String query;

        public PlayFromQuerySubscriber(EventBus eventBus, PlaybackToastHelper playbackToastHelper, String query) {
            super(eventBus, playbackToastHelper);
            this.query = query;
        }

        @Override
        public void onNext(PlaybackResult playbackResult) {
            activityContext.startActivity(new Intent(Actions.STREAM).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            super.onNext(playbackResult);
        }

        @Override
        public void onError(Throwable e) {
            fallbackToSearch(query);
        }
    }

    private class PlayFromPlaylistSubscriber extends DefaultSubscriber<PropertySet> {
        private final String query;

        public PlayFromPlaylistSubscriber(String query) {
            this.query = query;
        }

        @Override
        public void onNext(PropertySet result) {
            navigator.openPlaylistWithAutoPlay(activityContext,
                                               result.get(PlaylistProperty.URN),
                                               Screen.SEARCH_PLAYLIST_DISCO);
        }

        @Override
        public void onError(Throwable e) {
            fallbackToSearch(query);
        }
    }
}


