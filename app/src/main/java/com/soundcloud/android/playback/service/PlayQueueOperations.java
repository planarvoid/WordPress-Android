package com.soundcloud.android.playback.service;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;
import static com.soundcloud.android.utils.Log.ADS_TAG;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.api.model.PolicyInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackWriteStorage;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import javax.inject.Inject;
import java.util.List;

public class PlayQueueOperations {

    static final String SHARED_PREFERENCES_KEY = "playlistPos";

    private final SharedPreferences sharedPreferences;
    private final PlayQueueStorage playQueueStorage;
    private final TrackWriteStorage trackWriteStorage;
    private final RxHttpClient rxHttpClient;
    private final Action1<RecommendedTracksCollection> cacheRelatedTracks = new Action1<RecommendedTracksCollection>() {
        @Override
        public void call(RecommendedTracksCollection collection) {
            fireAndForget(trackWriteStorage.storeTracksAsync(collection.getCollection()));
        }
    };
    private final Action1<ModelCollection<PolicyInfo>> storePolicies = new Action1<ModelCollection<PolicyInfo>>() {
        @Override
        public void call(ModelCollection<PolicyInfo> policies) {
            if (Log.isLoggable(ADS_TAG, Log.DEBUG)) {
                for (PolicyInfo policy : policies.getCollection()) {
                    com.soundcloud.android.utils.Log.d(ADS_TAG, "Retrieved policy info: " + policy);
                }
            }
            fireAndForget(trackWriteStorage.storePoliciesAsync(policies.getCollection()));
        }
    };

    @Inject
    public PlayQueueOperations(Context context, PlayQueueStorage playQueueStorage,
                               TrackWriteStorage trackWriteStorage, RxHttpClient rxHttpClient) {
        sharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        this.playQueueStorage = playQueueStorage;
        this.trackWriteStorage = trackWriteStorage;
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

    @Nullable
    public Observable<PlayQueue> getLastStoredPlayQueue() {
        if (getLastStoredPlayingTrackId() != Consts.NOT_SET) {
            return playQueueStorage.loadAsync().toList()
                    .map(new Func1<List<PlayQueueItem>, PlayQueue>() {
                        @Override
                        public PlayQueue call(List<PlayQueueItem> playQueueItems) {
                            return new PlayQueue(playQueueItems);
                        }
                    }).observeOn(AndroidSchedulers.mainThread());
        }
        return null;
    }

    public Subscription saveQueue(PlayQueue playQueue) {
        return fireAndForget(playQueueStorage.storeAsync(playQueue));
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
        fireAndForget(playQueueStorage.clearAsync());
    }

    public Observable<RecommendedTracksCollection> getRelatedTracks(Urn urn) {
        final String endpoint = String.format(ApiEndpoints.RELATED_TRACKS.path(), urn.toEncodedString());
        final ApiRequest<RecommendedTracksCollection> request = ApiRequest.Builder.<RecommendedTracksCollection>get(endpoint)
                .forPrivateApi(1)
                .forResource(TypeToken.of(RecommendedTracksCollection.class)).build();

        return rxHttpClient.<RecommendedTracksCollection>fetchModels(request).doOnNext(cacheRelatedTracks);
    }

    public Observable<ModelCollection<PolicyInfo>> fetchAndStorePolicies(List<Urn> trackUrns) {
        final ApiRequest<PolicyCollection> request = ApiRequest.Builder.<PolicyCollection>post(ApiEndpoints.POLICIES.path())
                .withContent(transformUrnsToStrings(trackUrns))
                .forPrivateApi(1)
                .forResource(TypeToken.of(PolicyCollection.class)).build();

        final Observable<ModelCollection<PolicyInfo>> mapObservable = rxHttpClient.fetchModels(request);
        return mapObservable.doOnNext(storePolicies);
    }

    private List<String> transformUrnsToStrings(List<Urn> trackUrns) {
        return Lists.transform(trackUrns, new Function<Urn, String>() {
            @Override
            public String apply(Urn input) {
                return input.toString();
            }
        });
    }

    enum Keys {
        PLAY_POSITION, SEEK_POSITION, TRACK_ID
    }

    public static class PolicyCollection extends ModelCollection<PolicyInfo> {
        // Policy type token
    }
}
