package com.soundcloud.android.playback;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRxV2;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.Single;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;

public class PlayQueueOperations {

    @VisibleForTesting static final String SHARED_PREFERENCES_KEY = "playlistPos";

    private final SharedPreferences sharedPreferences;
    private final PlayQueueStorage playQueueStorage;
    private final StoreTracksCommand storeTracksCommand;
    private final ApiClientRxV2 apiClientRx;
    private final Scheduler scheduler;

    @Inject
    PlayQueueOperations(Context context, PlayQueueStorage playQueueStorage,
                        StoreTracksCommand storeTracksCommand, ApiClientRxV2 apiClientRx,
                        @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler) {
        this.storeTracksCommand = storeTracksCommand;
        this.scheduler = scheduler;
        this.sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        this.playQueueStorage = playQueueStorage;
        this.apiClientRx = apiClientRx;
    }

    int getLastStoredPlayPosition() {
        return sharedPreferences.getInt(Keys.PLAY_POSITION.name(), 0);
    }

    PlaySessionSource getLastStoredPlaySessionSource() {
        return new PlaySessionSource(sharedPreferences);
    }

    Maybe<PlayQueue> getLastStoredPlayQueue() {
        if (!sharedPreferences.contains(Keys.PLAY_POSITION.name())) {
            return Maybe.empty();
        }

        return playQueueStorage.load()
                               .map(PlayQueue::fromPlayQueueItems)
                               .toMaybe()
                               .subscribeOn(scheduler);
    }

    Completable saveQueue(PlayQueue playQueue) {
        return Completable.fromAction(() -> playQueueStorage.store(playQueue));
    }

    void savePlayInfo(int position, PlaySessionSource playSessionSource) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        playSessionSource.saveToPreferences(editor);
        editor.putInt(Keys.PLAY_POSITION.name(), position);
        editor.apply();
    }

    void clear() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(Keys.PLAY_POSITION.name());
        PlaySessionSource.clearPreferenceKeys(editor);
        editor.apply();

        playQueueStorage.clear()
                        .subscribeOn(scheduler)
                        .subscribe();
    }

    Single<RecommendedTracksCollection> relatedTracks(Urn seedTrack, boolean continuousPlay) {
        final String endpoint = String.format(ApiEndpoints.RELATED_TRACKS.path(), seedTrack.toEncodedString());
        final ApiRequest request = ApiRequest.get(endpoint)
                                             .forPrivateApi()
                                             .addQueryParam("continuous_play", continuousPlay ? "true" : "false")
                                             .build();

        return apiClientRx.mappedResponse(request, RecommendedTracksCollection.class)
                          .doOnSuccess(storeTracksCommand.toConsumer())
                          .subscribeOn(scheduler);
    }

    Single<PlayQueue> relatedTracksPlayQueue(final Urn seedTrack,
                                             final boolean continuousPlay,
                                             final PlaySessionSource playSessionSource) {
        return relatedTracks(seedTrack, continuousPlay).map(recommendedTracks -> {
            if (recommendedTracks.getCollection().isEmpty()) {
                return PlayQueue.empty();
            } else {
                return PlayQueue.fromRecommendations(seedTrack, continuousPlay, recommendedTracks, playSessionSource);
            }
        });
    }

    enum Keys {
        PLAY_POSITION
    }
}
