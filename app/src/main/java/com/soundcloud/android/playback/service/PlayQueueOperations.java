package com.soundcloud.android.playback.service;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.api.http.SoundCloudAPIRequest;
import com.soundcloud.android.model.PlayQueueItem;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.RelatedTracksCollection;
import com.soundcloud.android.model.TrackSummary;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.storage.BulkStorage;
import com.soundcloud.android.storage.PlayQueueStorage;
import com.soundcloud.android.utils.SharedPreferencesUtils;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.util.functions.Func1;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;
import java.util.List;

public class PlayQueueOperations {

    static final String SHARED_PREFERENCES_KEY = "playlistPos";
    enum Keys {
        PLAY_POSITION, SEEK_POSITION, TRACK_ID
    }

    private final SharedPreferences mSharedPreferences;
    private final PlayQueueStorage mPlayQueueStorage;
    private final BulkStorage mBulkStorage;
    private final RxHttpClient mRxHttpClient;

    private final Action1<RelatedTracksCollection> mCacheRelatedTracks = new Action1<RelatedTracksCollection>() {
        @Override
        public void call(RelatedTracksCollection collection) {
            fireAndForget(mBulkStorage.bulkInsertAsync(Lists.transform(collection.getCollection(), TrackSummary.TO_TRACK)));
        }
    };


    @Inject
    public PlayQueueOperations(Context context, PlayQueueStorage playQueueStorage,
                               BulkStorage bulkStorage, RxHttpClient rxHttpClient) {
        mSharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        mPlayQueueStorage = playQueueStorage;
        mBulkStorage = bulkStorage;
        mRxHttpClient = rxHttpClient;
    }

    public long getLastStoredSeekPosition() {
        return mSharedPreferences.getLong(Keys.SEEK_POSITION.name(), 0);
    }

    public int getLastStoredPlayPosition() {
        return mSharedPreferences.getInt(Keys.PLAY_POSITION.name(), 0);
    }

    public long getLastStoredPlayingTrackId() {
        return mSharedPreferences.getLong(Keys.TRACK_ID.name(), Playable.NOT_SET);
    }

    public PlaySessionSource getLastStoredPlaySessionSource() {
        return new PlaySessionSource(mSharedPreferences);
    }

    public Observable<PlayQueue> getLastStoredPlayQueue() {
        if (getLastStoredPlayingTrackId() != Playable.NOT_SET) {
            return mPlayQueueStorage.getPlayQueueItemsAsync()
                    .observeOn(AndroidSchedulers.mainThread()).map(new Func1<List<PlayQueueItem>, PlayQueue>() {
                        @Override
                        public PlayQueue call(List<PlayQueueItem> playQueueItems) {
                            return new PlayQueue(playQueueItems, getLastStoredPlayPosition());
                        }
                    });
        }
        return null;
    }

    public Subscription saveQueue(PlayQueue playQueue, PlaySessionSource playSessionSource, long seekPosition) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();

        editor.putLong(Keys.TRACK_ID.name(), playQueue.getCurrentTrackId());
        editor.putInt(Keys.PLAY_POSITION.name(), playQueue.getPosition());
        editor.putLong(Keys.SEEK_POSITION.name(), seekPosition);

        playSessionSource.saveToPreferences(editor);

        SharedPreferencesUtils.apply(editor);

        return DefaultSubscriber.fireAndForget(mPlayQueueStorage.storeCollectionAsync(playQueue.getItems()));
    }

    public void clear() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.remove(Keys.TRACK_ID.name());
        editor.remove(Keys.PLAY_POSITION.name());
        editor.remove(Keys.SEEK_POSITION.name());
        PlaySessionSource.clearPreferenceKeys(editor);
        SharedPreferencesUtils.apply(editor);
        mPlayQueueStorage.clearState();
    }

    public Observable<RelatedTracksCollection> getRelatedTracks(long trackId) {
        final Urn urn = Urn.forTrack(trackId);
        final String endpoint = String.format(APIEndpoints.RELATED_TRACKS.path(), urn.toEncodedString());
        final APIRequest<RelatedTracksCollection> request = SoundCloudAPIRequest.RequestBuilder.<RelatedTracksCollection>get(endpoint)
                .forPrivateAPI(1)
                .forResource(TypeToken.of(RelatedTracksCollection.class)).build();

        return mRxHttpClient.<RelatedTracksCollection>fetchModels(request).doOnNext(mCacheRelatedTracks);
    }
}
