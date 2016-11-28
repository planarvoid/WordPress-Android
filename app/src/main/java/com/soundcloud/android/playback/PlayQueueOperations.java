package com.soundcloud.android.playback;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.commands.StoreTracksCommand;
import com.soundcloud.android.model.Urn;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.functions.Func1;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

public class PlayQueueOperations {

    @VisibleForTesting static final String SHARED_PREFERENCES_KEY = "playlistPos";

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

    int getLastStoredPlayPosition() {
        return sharedPreferences.getInt(Keys.PLAY_POSITION.name(), 0);
    }

    PlaySessionSource getLastStoredPlaySessionSource() {
        return new PlaySessionSource(sharedPreferences);
    }

    Observable<PlayQueue> getLastStoredPlayQueue() {
        if (!sharedPreferences.contains(Keys.PLAY_POSITION.name())) {
            return Observable.empty();
        }

        return playQueueStorage.loadAsync()
                               .toList()
                               .map(new Func1<List<PlayQueueItem>, PlayQueue>() {
                                   @Override
                                   public PlayQueue call(List<PlayQueueItem> playQueueItems) {
                                       return PlayQueue.fromPlayQueueItems(playQueueItems);
                                   }
                               })
                               .subscribeOn(scheduler);
    }

    Subscription saveQueue(PlayQueue playQueue) {
        return fireAndForget(
                playQueueStorage.storeAsync(playQueue).subscribeOn(scheduler)
        );
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
        fireAndForget(
                playQueueStorage.clearAsync().subscribeOn(scheduler)
        );
    }

    public Observable<RecommendedTracksCollection> relatedTracks(Urn seedTrack, boolean continuousPlay) {
        final String endpoint = String.format(ApiEndpoints.RELATED_TRACKS.path(), seedTrack.toEncodedString());
        final ApiRequest request = ApiRequest.get(endpoint)
                                             .forPrivateApi()
                                             .addQueryParam("continuous_play", continuousPlay ? "true" : "false")
                                             .build();

        return apiClientRx.mappedResponse(request, RecommendedTracksCollection.class)
                          .doOnNext(storeTracksCommand.toAction1())
                          .subscribeOn(scheduler);
    }

    public Observable<PlayQueue> relatedTracksPlayQueue(final Urn seedTrack,
                                                        final boolean continuousPlay,
                                                        final PlaySessionSource playSessionSource) {
        return relatedTracks(seedTrack, continuousPlay).map(new Func1<RecommendedTracksCollection, PlayQueue>() {
            @Override
            public PlayQueue call(RecommendedTracksCollection recommendedTracks) {
                if (recommendedTracks.getCollection().isEmpty()) {
                    return PlayQueue.empty();
                }

                return PlayQueue.fromRecommendations(seedTrack, continuousPlay, recommendedTracks, playSessionSource);
            }
        });
    }

    enum Keys {
        PLAY_POSITION
    }

}
