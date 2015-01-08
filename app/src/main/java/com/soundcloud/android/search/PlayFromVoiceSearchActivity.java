package com.soundcloud.android.search;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.playlists.PlaylistDetailActivity;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.propeller.PropertySet;

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
    @Inject PlaybackOperations playbackOperations;
    @Inject Random random;

    public PlayFromVoiceSearchActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    public PlayFromVoiceSearchActivity(SearchOperations searchOperations, PlaybackOperations playbackOperations, Random random) {
        this.searchOperations = searchOperations;
        this.playbackOperations = playbackOperations;
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
        searchOperations.searchResult(query, SearchOperations.TYPE_TRACKS).subscribe(new FetchResourceSubscriber(query, false) {
            @Override
            public void onResult(PropertySet result) {
                playbackOperations.playTrackWithRecommendations(result.get(TrackProperty.URN), new PlaySessionSource(Screen.VOICE_COMMAND));
            }
        });
    }

    private void playPlaylist(final String query) {
        searchOperations.searchResult(query, SearchOperations.TYPE_PLAYLISTS).subscribe(new FetchResourceSubscriber(query, true) {
            @Override
            public void onResult(PropertySet result) {
                PlaylistDetailActivity.start(PlayFromVoiceSearchActivity.this, result.get(PlaylistProperty.URN), Screen.SEARCH_PLAYLIST_DISCO, true);
            }
        });
    }

    private abstract class FetchResourceSubscriber extends DefaultSubscriber<SearchResult> {
        private final String query;
        private final boolean randomResult;

        private FetchResourceSubscriber(String query, boolean randomResult) {
            this.query = query;
            this.randomResult = randomResult;
        }

        @Override
        public void onNext(SearchResult searchResult) {
            List<PropertySet> items = searchResult.getItems();
            if (!items.isEmpty()) {
                onResult(randomResult ? items.get(random.nextInt(items.size())) : items.get(0));
            } else {
                fallbackToSearch(query);
            }
        }

        @Override
        public void onError(Throwable e) {
            fallbackToSearch(query);
        }

        public abstract void onResult(PropertySet result);


    }

    private void fallbackToSearch(String query) {
        final Intent intent = new Intent(Actions.PERFORM_SEARCH)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(SearchManager.QUERY, query);

        startActivity(intent);
    }
}


