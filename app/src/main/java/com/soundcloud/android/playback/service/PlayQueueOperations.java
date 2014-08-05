package com.soundcloud.android.playback.service;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.SoundCloudAPIRequest;
import com.soundcloud.android.api.model.PolicyInfo;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.tracks.TrackWriteStorage;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;
import java.util.Collection;
import java.util.List;

public class PlayQueueOperations {

    static final String SHARED_PREFERENCES_KEY = "playlistPos";

    private final SharedPreferences sharedPreferences;
    private final PlayQueueStorage playQueueStorage;
    private final TrackWriteStorage trackWriteStorage;
    private final RxHttpClient rxHttpClient;

    enum Keys {
        PLAY_POSITION, SEEK_POSITION, TRACK_ID
    }

    private final Action1<RecommendedTracksCollection> cacheRelatedTracks = new Action1<RecommendedTracksCollection>() {
        @Override
        public void call(RecommendedTracksCollection collection) {
            fireAndForget(trackWriteStorage.storeTracksAsync(collection.getCollection()));
        }
    };

    private final Action1<Collection<PolicyInfo>> storePolicies = new Action1<Collection<PolicyInfo>>() {
        @Override
        public void call(Collection<PolicyInfo> policies) {
            fireAndForget(trackWriteStorage.storePoliciesAsync(policies));
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
                            return new PlayQueue(playQueueItems, getLastStoredPlayPosition());
                        }
                    }).observeOn(AndroidSchedulers.mainThread());
        }
        return null;
    }

    public Subscription saveQueue(PlayQueue playQueue, PlaySessionSource playSessionSource, long seekPosition) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // TODO: migrate the preferences to store the URN, not the ID
        editor.putLong(Keys.TRACK_ID.name(), playQueue.getCurrentTrackUrn().numericId);
        editor.putInt(Keys.PLAY_POSITION.name(), playQueue.getCurrentPosition());
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

    public Observable<RecommendedTracksCollection> getRelatedTracks(TrackUrn urn) {
        final String endpoint = String.format(APIEndpoints.RELATED_TRACKS.path(), urn.toEncodedString());
        final APIRequest<RecommendedTracksCollection> request = SoundCloudAPIRequest.RequestBuilder.<RecommendedTracksCollection>get(endpoint)
                .forPrivateAPI(1)
                .forResource(TypeToken.of(RecommendedTracksCollection.class)).build();

        return rxHttpClient.<RecommendedTracksCollection>fetchModels(request).doOnNext(cacheRelatedTracks);
    }

    public Observable<Collection<PolicyInfo>> fetchAndStorePolicies(List<TrackUrn> trackUrns){
        final APIRequest<Collection<PolicyInfo>> request = SoundCloudAPIRequest.RequestBuilder.<Collection<PolicyInfo>>post(APIEndpoints.POLICIES.path())
                .withContent(transformUrnsToStrings(trackUrns))
                .forPrivateAPI(1)
                .forResource(new TypeToken<Collection<PolicyInfo>>() {}).build();

        final Observable<Collection<PolicyInfo>> mapObservable = rxHttpClient.fetchModels(request);
        return mapObservable.doOnNext(storePolicies);
    }

    private List<String> transformUrnsToStrings(List<TrackUrn> trackUrns) {
        return Lists.transform(trackUrns, new Function<TrackUrn, String>() {
            @Override
            public String apply(TrackUrn input) {
                return input.toString();
            }
        });
    }
}
