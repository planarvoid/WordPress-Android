package com.soundcloud.android.playback.service;

import com.google.common.reflect.TypeToken;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.api.http.SoundCloudAPIRequest;
import com.soundcloud.android.model.ClientUri;
import com.soundcloud.android.model.PlayQueueItem;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.RelatedTracksCollection;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.rx.observers.RxObserverHelper;
import com.soundcloud.android.storage.PlayQueueStorage;
import com.soundcloud.android.tracking.eventlogger.PlaySessionSource;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.utils.SharedPreferencesUtils;
import rx.Observable;
import rx.Subscription;
import rx.android.concurrency.AndroidSchedulers;
import rx.util.functions.Func1;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import javax.inject.Inject;
import java.util.List;

public class PlayQueueOperations {

    static final String SHARED_PREFERENCES_KEY = "playlistPos";
    enum Keys {
        PLAY_POSITION, SEEK_POSITION, TRACK_ID, ORIGIN_URL, SET_ID
    }

    private final SharedPreferences mSharedPreferences;
    private final PlayQueueStorage mPlayQueueStorage;
    private RxHttpClient mRxHttpClient;

    @Inject
    public PlayQueueOperations(Context context, PlayQueueStorage playQueueStorage, RxHttpClient rxHttpClient) {
        mSharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        mPlayQueueStorage = playQueueStorage;
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
        return new PlaySessionSource(mSharedPreferences.getString(Keys.ORIGIN_URL.name(), ScTextUtils.EMPTY_STRING),
                mSharedPreferences.getLong(Keys.SET_ID.name(), ScModel.NOT_SET));
    }

    public Observable<PlayQueue> getLastStoredPlayQueue() {
        if (getLastStoredPlayingTrackId() != Playable.NOT_SET) {
            final PlaySessionSource playSessionSource = getLastStoredPlaySessionSource();
            return mPlayQueueStorage.getPlayQueueItemsAsync()
                    .observeOn(AndroidSchedulers.mainThread()).map(new Func1<List<PlayQueueItem>, PlayQueue>() {
                        @Override
                        public PlayQueue call(List<PlayQueueItem> playQueueItems) {
                            return new PlayQueue(playQueueItems, getLastStoredPlayPosition(), playSessionSource);
                        }
                    });
        }
        return null;
    }

    public Subscription saveQueue(PlayQueue playQueue, long seekPosition) {
        SharedPreferences.Editor editor = mSharedPreferences.edit();

        editor.putLong(Keys.TRACK_ID.name(), playQueue.getCurrentTrackId());
        editor.putInt(Keys.PLAY_POSITION.name(), playQueue.getPosition());
        editor.putLong(Keys.SEEK_POSITION.name(), seekPosition);
        editor.putLong(Keys.SET_ID.name(), playQueue.getPlaylistId());
        editor.putString(Keys.ORIGIN_URL.name(), playQueue.getOriginScreen());
        SharedPreferencesUtils.apply(editor);

        return RxObserverHelper.fireAndForget(mPlayQueueStorage.storeCollectionAsync(playQueue.getItems()));
    }

    public void clear() {
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        editor.remove(Keys.TRACK_ID.name());
        editor.remove(Keys.PLAY_POSITION.name());
        editor.remove(Keys.SEEK_POSITION.name());
        editor.remove(Keys.SET_ID.name());
        editor.remove(Keys.ORIGIN_URL.name());
        SharedPreferencesUtils.apply(editor);
        mPlayQueueStorage.clearState();
    }

    public Observable<RelatedTracksCollection> getRelatedTracks(long trackId) {
        final ClientUri clientUri = ClientUri.fromTrack(trackId);
        if (clientUri != null){
            final String endpoint = String.format(APIEndpoints.RELATED_TRACKS.path(), clientUri.toEncodedString());
            final APIRequest<RelatedTracksCollection> request = SoundCloudAPIRequest.RequestBuilder.<RelatedTracksCollection>get(endpoint)
                    .forPrivateAPI(1)
                    .forResource(TypeToken.of(RelatedTracksCollection.class)).build();

            return mRxHttpClient.fetchModels(request);
        } else {
            Log.e(this, "Unable to parse client URI from id " + trackId);
        }
        return null;
    }


    private Uri extractUri(SharedPreferences sharedPreferences, String parameter) {
        return Uri.parse(sharedPreferences.getString(parameter, ScTextUtils.EMPTY_STRING));
    }
}
