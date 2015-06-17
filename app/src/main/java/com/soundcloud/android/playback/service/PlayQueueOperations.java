package com.soundcloud.android.playback.service;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Func1;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

class PlayQueueOperations {

    static final String SHARED_PREFERENCES_KEY = "playlistPos";

    private final SharedPreferences sharedPreferences;
    private final PlayQueueStorage playQueueStorage;
    private final StoreTracksCommand storeTracksCommand;
    private final ApiClientRx apiClientRx;
    private final Scheduler scheduler;

    @Inject
    PlayQueueOperations(Context context, PlayQueueStorage playQueueStorage,
                               StoreTracksCommand storeTracksCommand, ApiClientRx apiClientRx,
                               @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.storeTracksCommand = storeTracksCommand;
        this.scheduler = scheduler;
        this.sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        this.playQueueStorage = playQueueStorage;
        this.apiClientRx = apiClientRx;
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

    @Nullable
    public Observable<PlayQueue> getLastStoredPlayQueue() {
        if (getLastStoredPlayingTrackId() != Consts.NOT_SET) {
            return playQueueStorage.loadAsync()
                    .toList()
                    .map(new Func1<List<PlayQueueItem>, PlayQueue>() {
                        @Override
                        public PlayQueue call(List<PlayQueueItem> playQueueItems) {
                            return new PlayQueue(playQueueItems);
                        }
                    })
                    .subscribeOn(scheduler);
        }
        return null;
    }

    public Subscription saveQueue(PlayQueue playQueue) {
        return fireAndForget(
                playQueueStorage.storeAsync(playQueue).subscribeOn(scheduler)
        );
    }

    public void savePositionInfo(int position, Urn currentUrn, PlaySessionSource playSessionSource, long seekPosition) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // TODO: migrate the preferences to store the URN, not the ID
        editor.putLong(Keys.TRACK_ID.name(), currentUrn.getNumericId());
        editor.putInt(Keys.PLAY_POSITION.name(), position);
        editor.putLong(Keys.SEEK_POSITION.name(), seekPosition);

        playSessionSource.saveToPreferences(editor);

        editor.apply();
    }

    public void clear() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(Keys.TRACK_ID.name());
        editor.remove(Keys.PLAY_POSITION.name());
        editor.remove(Keys.SEEK_POSITION.name());
        PlaySessionSource.clearPreferenceKeys(editor);
        editor.apply();
        fireAndForget(
                playQueueStorage.clearAsync().subscribeOn(scheduler)
        );
    }

    public Observable<RecommendedTracksCollection> getRelatedTracks(Urn urn) {
        final String endpoint = String.format(ApiEndpoints.RELATED_TRACKS.path(), urn.toEncodedString());
        final ApiRequest request = ApiRequest.get(endpoint).forPrivateApi(1).build();

        return apiClientRx.mappedResponse(request, RecommendedTracksCollection.class)
                .doOnNext(storeTracksCommand.toAction())
                .subscribeOn(scheduler);
    }

    enum Keys {
        PLAY_POSITION, SEEK_POSITION, TRACK_ID
    }

}
