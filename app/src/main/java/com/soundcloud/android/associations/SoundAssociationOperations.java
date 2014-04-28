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
import rx.functions.Action0;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.List;


/**
 * Contains all business logic related to interacting with playables (tracks, playlists)
 * such as liking, reposting, sharing, etc.
 */
public class SoundAssociationOperations {

    public static final String TAG = "SoundAssociations";

    private final EventBus eventBus;
    private final SoundAssociationStorage soundAssocStorage;
    private final RxHttpClient httpClient;
    private final ScModelManager modelManager;

    @Inject
    public SoundAssociationOperations(
            EventBus eventBus,
            SoundAssociationStorage soundAssocStorage,
            RxHttpClient httpClient,
            ScModelManager modelManager) {
        this.eventBus = eventBus;
        this.soundAssocStorage = soundAssocStorage;
        this.httpClient = httpClient;
        this.modelManager = modelManager;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // LIKING / UN-LIKING
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Observable<List<Long>> getLikedTracksIds() {
        return soundAssocStorage.getTrackLikesAsIdsAsync();
    }

    public Observable<SoundAssociation> toggleLike(boolean addLike, final Playable playable) {
        return addLike ? like(playable) : unlike(playable);
    }

    public Observable<SoundAssociation> like(final Playable playable) {
        logPlayable("LIKE", playable);
        return httpClient.fetchResponse(buildRequestForLike(playable, true)).mergeMap(mapAddLikeResponse(playable))
                .doOnCompleted(handlePlayableStateChanged(playable));
    }

    public Observable<SoundAssociation> unlike(final Playable playable) {
        logPlayable("UNLIKE", playable);
        return httpClient.fetchResponse(buildRequestForLike(playable, false)).mergeMap(mapRemoveLikeResponse(playable))
                .onErrorResumeNext(handle404(soundAssocStorage.removeLikeAsync(playable)))
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
                    if (requestException.response() != null
                            && requestException.response().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
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
                return soundAssocStorage.addLikeAsync(playable);
            }
        };
    }

    private Func1<APIResponse, Observable<SoundAssociation>> mapRemoveLikeResponse(final Playable playable) {
        return new Func1<APIResponse, Observable<SoundAssociation>>() {
            @Override
            public Observable<SoundAssociation> call(APIResponse response) {
                logPlayable("REMOVE", playable);
                return soundAssocStorage.removeLikeAsync(playable);
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
        return httpClient.fetchResponse(buildRequestForReposts(playable, true)).mergeMap(mapAddRepostResponse(playable))
                .doOnCompleted(handlePlayableStateChanged(playable));
    }

    public Observable<SoundAssociation> unrepost(final Playable playable) {
        logPlayable("UNREPOST", playable);
        return httpClient.fetchResponse(buildRequestForReposts(playable, false)).mergeMap(mapRemoveRepostResponse(playable))
                .onErrorResumeNext(handle404(soundAssocStorage.removeRepostAsync(playable)))
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
                return soundAssocStorage.addRepostAsync(playable);
            }
        };
    }

    private Func1<APIResponse, Observable<SoundAssociation>> mapRemoveRepostResponse(final Playable playable) {
        return new Func1<APIResponse, Observable<SoundAssociation>>() {
            @Override
            public Observable<SoundAssociation> call(APIResponse response) {
                logPlayable("REMOVE", playable);
                return soundAssocStorage.removeRepostAsync(playable);
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
                modelManager.cache(playable, ScResource.CacheUpdateMode.NONE);
                Log.d(TAG, "publishing playable change event");
                eventBus.publish(EventQueue.PLAYABLE_CHANGED, PlayableChangedEvent.create(playable));
            }
        };
    }

    private void logPlayable(String action, final Playable playable) {
        Log.d(TAG, Thread.currentThread().getName() + "|" + action + " for playable " + playable.getId() + ": liked = "
                + playable.user_like + "; likes = " + playable.likes_count + "; reposted = " + playable.user_repost
                + "; reposts = " + playable.reposts_count);
    }
}
