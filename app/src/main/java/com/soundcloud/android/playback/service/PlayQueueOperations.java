package com.soundcloud.android.playback.service;

import com.soundcloud.android.model.PlayQueueItem;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.rx.observers.RxObserverHelper;
import com.soundcloud.android.storage.PlayQueueStorage;
import com.soundcloud.android.tracking.eventlogger.PlaySessionSource;
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

    private static final String SHARED_PREFERENCES_KEY = "playlistPos";
    enum Keys {
        PLAY_POSITION, SEEK_POSITION, TRACK_ID, ORIGIN_URL, SET_ID
    }

    private final SharedPreferences mSharedPreferences;
    private final PlayQueueStorage mPlayQueueStorage;

    public PlayQueueOperations(Context context, PlayQueueStorage playQueueStorage) {
        this(context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE), playQueueStorage);
    }

    @Inject
    public PlayQueueOperations(SharedPreferences sharedPreferences, PlayQueueStorage playQueueStorage) {
        mSharedPreferences = sharedPreferences;
        mPlayQueueStorage = playQueueStorage;
    }

    public int getLastStoredSeekPosition() {
        return mSharedPreferences.getInt(Keys.SEEK_POSITION.name(), 0);
    }

    public int getLastStoredPlayPosition() {
        return mSharedPreferences.getInt(Keys.PLAY_POSITION.name(), 0);
    }

    public long getLastStoredPlayingTrackId() {
        return mSharedPreferences.getLong(Keys.TRACK_ID.name(), Playable.NOT_SET);
    }

    public PlaySessionSource getLastStoredPlaySessionSource() {
        return new PlaySessionSource(extractUri(mSharedPreferences, Keys.ORIGIN_URL.name()),
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
        editor.putLong(Keys.SET_ID.name(), playQueue.getSetId());

        final Uri origin = playQueue.getOriginPage();
        if (origin != null && origin != Uri.EMPTY) {
            editor.putString(Keys.ORIGIN_URL.name(), String.valueOf(origin));
        }
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


    private Uri extractUri(SharedPreferences sharedPreferences, String parameter) {
        return Uri.parse(sharedPreferences.getString(parameter, ScTextUtils.EMPTY_STRING));
    }
}
