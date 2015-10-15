package com.soundcloud.android.search;

import static com.soundcloud.java.checks.Preconditions.checkState;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.ui.view.PlaybackToastHelper;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;

import javax.inject.Inject;
import java.util.List;
import java.util.Random;

@SuppressWarnings({"PMD.AccessorClassGeneration"})
public class PlayFromVoiceSearchActivity extends Activity {

    private static final String ANDROID_INTENT_EXTRA_GENRE = "android.intent.extra.genre";

    @Inject SearchOperations searchOperations;
    @Inject PlaybackInitiator playbackInitiator;
    @Inject Random random;
    @Inject EventBus eventBus;
    @Inject PlaybackToastHelper playbackToastHelper;

    private final Func1<SearchResult, Observable<PlaybackResult>> toPlayWithRecommendations = new Func1<SearchResult, Observable<PlaybackResult>>() {
        @Override
        public Observable<PlaybackResult> call(SearchResult searchResult) {
            List<PropertySet> items = searchResult.getItems();
            checkState(!items.isEmpty(), "There is no result for this search");
            return playbackInitiator.playTrackWithRecommendationsLegacy(items.get(0).get(TrackProperty.URN), new PlaySessionSource(Screen.VOICE_COMMAND));
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

    public PlayFromVoiceSearchActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    public PlayFromVoiceSearchActivity(SearchOperations searchOperations, PlaybackInitiator playbackInitiator, Random random) {
        this.searchOperations = searchOperations;
        this.playbackInitiator = playbackInitiator;
        this.random = random;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.resolve);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent intent = getIntent();
        if (MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH.equals(intent.getAction())) {
            findViewById(R.id.progress).setVisibility(View.VISIBLE);

            final String mediaFocus = intent.getStringExtra(MediaStore.EXTRA_MEDIA_FOCUS);
            if (mediaFocus != null && MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE.equals(mediaFocus)) {
                playPlaylist(getGenreKeyCompat(intent));
            } else {
                playTrackFromQuery(intent.getStringExtra(SearchManager.QUERY));
            }

        } else {
            finish();
        }
    }

    private String getGenreKeyCompat(Intent intent) {
        return intent.getExtras().containsKey(ANDROID_INTENT_EXTRA_GENRE) ?
                            intent.getStringExtra(ANDROID_INTENT_EXTRA_GENRE) :
                            intent.getStringExtra(SearchManager.QUERY);
    }

    private void playTrackFromQuery(final String query) {
        searchOperations
                .searchResult(query, SearchOperations.TYPE_TRACKS)
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(toPlayWithRecommendations)
                .subscribe(new PlayFromQuerySubscriber(eventBus, playbackToastHelper, query));
    }

    private void playPlaylist(final String query) {
        searchOperations
                .searchResult(query, SearchOperations.TYPE_PLAYLISTS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(toRandomSearchResultItem)
                .subscribe(new PlayFromPlaylistSubscriber(query));
    }

    private void fallbackToSearch(String query) {
        final Intent intent = new Intent(Actions.PERFORM_SEARCH)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(SearchManager.QUERY, query);

        startActivity(intent);
    }

    private class PlayFromQuerySubscriber extends ExpandPlayerSubscriber {
        private final String query;

        public PlayFromQuerySubscriber(EventBus eventBus, PlaybackToastHelper playbackToastHelper, String query) {
            super(eventBus, playbackToastHelper);
            this.query = query;
        }

        @Override
        public void onNext(PlaybackResult playbackResult) {
            startActivity(new Intent(Actions.STREAM).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
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
            PlaylistDetailActivity.start(PlayFromVoiceSearchActivity.this, result.get(PlaylistProperty.URN), Screen.SEARCH_PLAYLIST_DISCO, true);
        }

        @Override
        public void onError(Throwable e) {
            fallbackToSearch(query);
        }
    }
}


