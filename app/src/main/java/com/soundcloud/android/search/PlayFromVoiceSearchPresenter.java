package com.soundcloud.android.search;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.playback.ExpandPlayerCommand;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
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
    private final NavigationExecutor navigationExecutor;
    private final ExpandPlayerCommand expandPlayerCommand;

    private Context activityContext;

    private final Func1<SearchResult, Observable<PlaybackResult>> toPlayWithRecommendations = new Func1<SearchResult, Observable<PlaybackResult>>() {
        @Override
        public Observable<PlaybackResult> call(SearchResult searchResult) {
            List<ListItem> items = searchResult.getItems();
            if (items.isEmpty()) {
                throw new NoResultsException();
            }
            return RxJava.toV1Observable(playbackInitiator.playTrackWithRecommendationsLegacy(items.get(0).getUrn(),
                                                                                              new PlaySessionSource(Screen.VOICE_COMMAND)));
        }
    };

    private final Func1<SearchResult, ListItem> toRandomSearchResultItem = new Func1<SearchResult, ListItem>() {
        @Override
        public ListItem call(SearchResult searchResult) {
            List<ListItem> items = searchResult.getItems();
            if (items.isEmpty()) {
                throw new NoResultsException();
            }
            return items.get(random.nextInt(items.size()));
        }
    };

    @Inject
    PlayFromVoiceSearchPresenter(SearchOperations searchOperations,
                                 PlaybackInitiator playbackInitiator,
                                 Random random,
                                 NavigationExecutor navigationExecutor,
                                 ExpandPlayerCommand expandPlayerCommand) {
        this.searchOperations = searchOperations;
        this.playbackInitiator = playbackInitiator;
        this.random = random;
        this.navigationExecutor = navigationExecutor;
        this.expandPlayerCommand = expandPlayerCommand;
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
                .searchResult(query, Optional.absent(), SearchType.TRACKS)
                .onErrorReturn(t -> SearchResult.empty())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(toPlayWithRecommendations)
                .subscribe(new PlayFromQuerySubscriber(expandPlayerCommand, query));
    }

    private void playPlaylist(final String query) {
        searchOperations
                .searchResult(query, Optional.absent(), SearchType.PLAYLISTS)
                .onErrorReturn(t -> SearchResult.empty())
                .observeOn(AndroidSchedulers.mainThread())
                .map(toRandomSearchResultItem)
                .subscribe(new PlayFromPlaylistSubscriber(query));
    }

    private void fallbackToSearch(String query) {
        navigationExecutor.performSearch(activityContext, query);
    }

    private class PlayFromQuerySubscriber extends ExpandPlayerSubscriber {
        private final String query;

        public PlayFromQuerySubscriber(ExpandPlayerCommand expandPlayerCommand, String query) {
            super(expandPlayerCommand);
            this.query = query;
        }

        @Override
        public void onNext(PlaybackResult playbackResult) {
            activityContext.startActivity(new Intent(Actions.STREAM).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            super.onNext(playbackResult);
        }

        @Override
        public void onError(Throwable e) {
            if (e instanceof NoResultsException) {
                fallbackToSearch(query);
            } else {
                super.onError(e);
            }
        }
    }

    private class PlayFromPlaylistSubscriber extends DefaultSubscriber<ListItem> {
        private final String query;

        public PlayFromPlaylistSubscriber(String query) {
            this.query = query;
        }

        @Override
        public void onNext(ListItem result) {
            navigationExecutor.openPlaylistWithAutoPlay(activityContext,
                                                        result.getUrn(),
                                                        Screen.SEARCH_PLAYLISTS);
        }

        @Override
        public void onError(Throwable e) {
            if (e instanceof NoResultsException) {
                fallbackToSearch(query);
            } else {
                super.onError(e);
            }
        }
    }

    private static class NoResultsException extends RuntimeException {

    }
}


