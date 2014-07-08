package com.soundcloud.android.playback.service;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.api.http.SoundCloudAPIRequest;
import com.soundcloud.android.model.RecommendedTracksCollection;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.BulkStorage;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;
import java.util.List;

public class PlayQueueOperations {

    static final String SHARED_PREFERENCES_KEY = "playlistPos";
    enum Keys {
        PLAY_POSITION, SEEK_POSITION, TRACK_ID
    }

    private final SharedPreferences sharedPreferences;
    private final PlayQueueStorage playQueueStorage;
    private final BulkStorage bulkStorage;
    private final RxHttpClient rxHttpClient;

    private final Action1<RecommendedTracksCollection> cacheRelatedTracks = new Action1<RecommendedTracksCollection>() {
        @Override
        public void call(RecommendedTracksCollection collection) {
            fireAndForget(bulkStorage.bulkInsertAsync(Lists.transform(collection.getCollection(), TrackSummary.TO_TRACK)));
        }
    };


    @Inject
    public PlayQueueOperations(Context context, PlayQueueStorage playQueueStorage,
                               BulkStorage bulkStorage, RxHttpClient rxHttpClient) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        this.playQueueStorage = playQueueStorage;
        this.bulkStorage = bulkStorage;
        this.rxHttpClient = rxHttpClient;
    }

    public long getLastStoredSeekPosition() {
        return sharedPreferences.getLong(Keys.SEEK_POSITION.name(), 0);
    }

    public int getLastStoredPlayPosition() {
        return sharedPreferences.getInt(Keys.PLAY_POSITION.name(), 0);
    }

    public long getLastStoredPlayingTrackId() {
        return sharedPreferences.getLong(Keys.TRACK_ID.name(), Consts.NOT_SET);
    }

    public PlaySessionSource getLastStoredPlaySessionSource() {
        return new PlaySessionSource(sharedPreferences);
    }

    public Observable<PlayQueue> getLastStoredPlayQueue() {
        if (getLastStoredPlayingTrackId() != Consts.NOT_SET) {
            return playQueueStorage.loadAsync().toList()
                    .map(new Func1<List<PlayQueueItem>, PlayQueue>() {
                        @Override
                        public PlayQueue call(List<PlayQueueItem> playQueueItems) {
                            return new PlayQueue(playQueueItems, getLastStoredPlayPosition());
                        }
                    }).observeOn(AndroidSchedulers.mainThread());
        }
        return null;
    }

    public Subscription saveQueue(PlayQueue playQueue, PlaySessionSource playSessionSource, long seekPosition) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putLong(Keys.TRACK_ID.name(), playQueue.getCurrentTrackId());
        editor.putInt(Keys.PLAY_POSITION.name(), playQueue.getPosition());
        editor.putLong(Keys.SEEK_POSITION.name(), seekPosition);

        playSessionSource.saveToPreferences(editor);

        editor.apply();

        return fireAndForget(playQueueStorage.storeAsync(playQueue));
    }

    public void clear() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(Keys.TRACK_ID.name());
        editor.remove(Keys.PLAY_POSITION.name());
        editor.remove(Keys.SEEK_POSITION.name());
        PlaySessionSource.clearPreferenceKeys(editor);
        editor.apply();
        fireAndForget(playQueueStorage.clearAsync());
    }

    public Observable<RecommendedTracksCollection> getRelatedTracks(long trackId) {
        final Urn urn = Urn.forTrack(trackId);
        final String endpoint = String.format(APIEndpoints.RELATED_TRACKS.path(), urn.toEncodedString());
        final APIRequest<RecommendedTracksCollection> request = SoundCloudAPIRequest.RequestBuilder.<RecommendedTracksCollection>get(endpoint)
                .forPrivateAPI(1)
                .forResource(TypeToken.of(RecommendedTracksCollection.class)).build();

        return rxHttpClient.<RecommendedTracksCollection>fetchModels(request).doOnNext(cacheRelatedTracks);
    }
}
