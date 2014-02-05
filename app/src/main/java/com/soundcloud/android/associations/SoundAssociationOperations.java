package com.soundcloud.android.associations;

import static com.soundcloud.android.api.http.SoundCloudAPIRequest.RequestBuilder;

import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.APIRequestException;
import com.soundcloud.android.api.http.APIResponse;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.storage.SoundAssociationStorage;
import com.soundcloud.android.utils.Log;
import org.apache.http.HttpStatus;
import rx.Observable;
import rx.util.functions.Action0;
import rx.util.functions.Func1;

import javax.inject.Inject;
import java.util.List;


/**
 * Contains all business logic related to interacting with playables (tracks, playlists)
 * such as liking, reposting, sharing, etc.
 */
public class SoundAssociationOperations {

    public static final String TAG = "SoundAssociations";

    private final EventBus mEventBus;
    private final SoundAssociationStorage mSoundAssocStorage;
    private final RxHttpClient mHttpClient;
    private final ScModelManager mModelManager;

    @Inject
    public SoundAssociationOperations(
            EventBus eventBus,
            SoundAssociationStorage soundAssocStorage,
            RxHttpClient httpClient,
            ScModelManager modelManager) {
        mEventBus = eventBus;
        mSoundAssocStorage = soundAssocStorage;
        mHttpClient = httpClient;
        mModelManager = modelManager;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // LIKING / UN-LIKING
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Observable<List<Long>> getLikedTracksIds() {
        return mSoundAssocStorage.getTrackLikesAsIdsAsync();
    }

    public Observable<SoundAssociation> toggleLike(boolean addLike, final Playable playable) {
        return addLike ? like(playable) : unlike(playable);
    }

    public Observable<SoundAssociation> like(final Playable playable) {
        logPlayable("LIKE", playable);
        return mHttpClient.fetchResponse(buildRequestForLike(playable, true)).mapMany(mapAddLikeResponse(playable))
                .doOnCompleted(handlePlayableStateChanged(playable));
    }

    public Observable<SoundAssociation> unlike(final Playable playable) {
        logPlayable("UNLIKE", playable);
        return mHttpClient.fetchResponse(buildRequestForLike(playable, false)).mapMany(mapRemoveLikeResponse(playable))
                .onErrorResumeNext(handle404(mSoundAssocStorage.removeLikeAsync(playable)))
                .doOnCompleted(handlePlayableStateChanged(playable));
    }

    // If a like has already been removed server side, and it hasn't synced back to the client, it can happen
    // that we're trying to remove a like that doesn't exist anymore. This action recovers from this scenarion
    // by removing the like locally and then resuming as usual.
    private Func1<Throwable, Observable<? extends SoundAssociation>> handle404(
            final Observable<SoundAssociation> fallbackRemovalFunction) {
        return new Func1<Throwable, Observable<? extends SoundAssociation>>() {
            @Override
            public Observable<? extends SoundAssociation> call(Throwable throwable) {
                if (throwable instanceof APIRequestException) {
                    APIRequestException requestException = (APIRequestException) throwable;
                    if (requestException.response().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                        Log.d(TAG, "not found; force removing association");
                        return fallbackRemovalFunction;
                    }
                }
                // forward error as usual
                return Observable.error(throwable);
            }
        };
    }

    private Func1<APIResponse, Observable<SoundAssociation>> mapAddLikeResponse(final Playable playable) {
        return new Func1<APIResponse, Observable<SoundAssociation>>() {
            @Override
            public Observable<SoundAssociation> call(APIResponse response) {
                logPlayable("STORE", playable);
                return mSoundAssocStorage.addLikeAsync(playable);
            }
        };
    }

    private Func1<APIResponse, Observable<SoundAssociation>> mapRemoveLikeResponse(final Playable playable) {
        return new Func1<APIResponse, Observable<SoundAssociation>>() {
            @Override
            public Observable<SoundAssociation> call(APIResponse response) {
                logPlayable("REMOVE", playable);
                return mSoundAssocStorage.removeLikeAsync(playable);
            }
        };
    }

    private APIRequest buildRequestForLike(final Playable playable, final boolean likeAdded) {
        APIEndpoints endpoint = playable instanceof Track ? APIEndpoints.MY_TRACK_LIKES : APIEndpoints.MY_PLAYLIST_LIKES;
        final String path = endpoint.path() + "/" + playable.getId();
        RequestBuilder builder = likeAdded ? RequestBuilder.put(path) : RequestBuilder.delete(path);
        return builder.forPublicAPI().build();
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // REPOSTING / UN-REPOSTING
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Observable<SoundAssociation> toggleRepost(boolean addRepost, final Playable playable) {
        return addRepost ? repost(playable) : unrepost(playable);
    }

    public Observable<SoundAssociation> repost(final Playable playable) {
        logPlayable("REPOST", playable);
        return mHttpClient.fetchResponse(buildRequestForReposts(playable, true)).mapMany(mapAddRepostResponse(playable))
                .doOnCompleted(handlePlayableStateChanged(playable));
    }

    public Observable<SoundAssociation> unrepost(final Playable playable) {
        logPlayable("UNREPOST", playable);
        return mHttpClient.fetchResponse(buildRequestForReposts(playable, false)).mapMany(mapRemoveRepostResponse(playable))
                .onErrorResumeNext(handle404(mSoundAssocStorage.removeRepostAsync(playable)))
                .doOnCompleted(handlePlayableStateChanged(playable));
    }

    private APIRequest buildRequestForReposts(final Playable playable, final boolean likeAdded) {
        APIEndpoints endpoint = playable instanceof Track ? APIEndpoints.MY_TRACK_REPOSTS : APIEndpoints.MY_PLAYLIST_REPOSTS;
        final String path = endpoint.path() + "/" + playable.getId();
        RequestBuilder builder = likeAdded ? RequestBuilder.put(path) : RequestBuilder.delete(path);
        return builder.forPublicAPI().build();
    }

    private Func1<APIResponse, Observable<SoundAssociation>> mapAddRepostResponse(final Playable playable) {
        return new Func1<APIResponse, Observable<SoundAssociation>>() {
            @Override
            public Observable<SoundAssociation> call(APIResponse response) {
                logPlayable("STORE", playable);
                return mSoundAssocStorage.addRepostAsync(playable);
            }
        };
    }

    private Func1<APIResponse, Observable<SoundAssociation>> mapRemoveRepostResponse(final Playable playable) {
        return new Func1<APIResponse, Observable<SoundAssociation>>() {
            @Override
            public Observable<SoundAssociation> call(APIResponse response) {
                logPlayable("REMOVE", playable);
                return mSoundAssocStorage.removeRepostAsync(playable);
            }
        };
    }

    // FIXME: the playable is written on a BG thread and read on the UI thread,
    // this might cause thread visibility issues.
    private Action0 handlePlayableStateChanged(final Playable playable) {
        return new Action0() {
            @Override
            public void call() {
                logPlayable("CACHE/PUBLISH", playable);
                mModelManager.cache(playable, ScResource.CacheUpdateMode.NONE);
                Log.d(TAG, "publishing playable change event");
                mEventBus.publish(EventQueue.PLAYABLE_CHANGED, PlayableChangedEvent.create(playable));
            }
        };
    }

    private void logPlayable(String action, final Playable playable) {
        Log.d(TAG, Thread.currentThread().getName() + "|" + action + " for playable " + playable.getId() + ": liked = "
                + playable.user_like + "; likes = " + playable.likes_count + "; reposted = " + playable.user_repost
                + "; reposts = " + playable.reposts_count);
    }
}
