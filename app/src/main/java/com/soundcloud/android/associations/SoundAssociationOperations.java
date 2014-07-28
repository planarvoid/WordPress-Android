package com.soundcloud.android.associations;

import static com.soundcloud.android.api.SoundCloudAPIRequest.RequestBuilder;

import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.APIRequestException;
import com.soundcloud.android.api.APIResponse;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableChangedEvent;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.storage.SoundAssociationStorage;
import com.soundcloud.android.tracks.LegacyTrackOperations;
import com.soundcloud.android.tracks.TrackUrn;
import com.soundcloud.android.utils.Log;
import com.soundcloud.propeller.PropertySet;
import org.apache.http.HttpStatus;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
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
    private final LegacyTrackOperations legacyTrackOperations;

    @Inject
    public SoundAssociationOperations(
            EventBus eventBus,
            SoundAssociationStorage soundAssocStorage,
            RxHttpClient httpClient,
            ScModelManager modelManager,
            LegacyTrackOperations legacyTrackOperations) {
        this.eventBus = eventBus;
        this.soundAssocStorage = soundAssocStorage;
        this.httpClient = httpClient;
        this.modelManager = modelManager;
        this.legacyTrackOperations = legacyTrackOperations;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // LIKING / UN-LIKING
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Observable<List<Long>> getLikedTracksIds() {
        return soundAssocStorage.getTrackLikesAsIdsAsync();
    }

    public Observable<PropertySet> toggleTrackLike(TrackUrn trackUrn, final boolean addLike) {
        /*
         * TODO: This is a temporary solution.
         * We need to be able to load the like count based on track URN and then save the new like status in
         * model manager and the collections table.
         */
        return legacyTrackOperations.loadTrack(trackUrn.numericId, AndroidSchedulers.mainThread())
                .mergeMap(new Func1<PublicApiTrack, Observable<PropertySet>>() {
                    @Override
                    public Observable<PropertySet> call(PublicApiTrack track) {
                        return toggleLike(addLike, track);
                    }
                });
    }

    @Deprecated
    public Observable<PropertySet> toggleLike(final boolean addLike, final Playable playable) {
        logPlayable(addLike ? "LIKE" : "UNLIKE", playable);
        return updateLikeState(playable, addLike)
                .mergeMap(new Func1<SoundAssociation, Observable<PropertySet>>() {
                    @Override
                    public Observable<PropertySet> call(SoundAssociation soundAssociation) {
                        APIRequest apiRequest = buildRequestForLike(playable, addLike);
                        return httpClient.fetchResponse(apiRequest)
                                .map(playableToPropertySetFunc(playable))
                                .onErrorResumeNext(handleInvalidUnlike(playable))
                                .onErrorResumeNext(updateLikeState(playable, !addLike)
                                                .map(playableToPropertySetFunc(playable))
                                );

                    }
                });
    }

    private Observable<SoundAssociation> updateLikeState(Playable playable, boolean addLike) {
        Observable<SoundAssociation> updateObservable = addLike
                ? soundAssocStorage.addLikeAsync(playable)
                : soundAssocStorage.removeLikeAsync(playable);
        return updateObservable.doOnNext(cacheAndPublishLike());
    }

    private Action1<SoundAssociation> cacheAndPublishLike() {
        return new Action1<SoundAssociation>() {
            @Override
            public void call(SoundAssociation soundAssociation) {
                Playable updated = soundAssociation.getPlayable();
                logPlayable("CACHE/PUBLISH", updated);
                modelManager.cache(updated, PublicApiResource.CacheUpdateMode.NONE);
                eventBus.publish(EventQueue.PLAYABLE_CHANGED, PlayableChangedEvent.forLike(updated.getUrn(), updated.user_like, updated.likes_count));
            }
        };
    }

    /*
     * A like could have been removed on the server but not yet synced to the client.
     * If we unliked and get a 404 then do not revert because client is already in correct state.
     */
    private Func1<Throwable, Observable<PropertySet>> handleInvalidUnlike(final Playable playable) {
        return new Func1<Throwable, Observable<PropertySet>>() {
            @Override
            public Observable<PropertySet> call(Throwable throwable) {
                if (throwable instanceof APIRequestException) {
                    APIRequestException requestException = (APIRequestException) throwable;
                    if (requestException.response() != null
                            && requestException.response().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                        Log.d(TAG, "Unliking a track that was not liked on server. Already in correct state.");
                        return Observable.just(createPropertySetFromLike(playable));
                    }
                }
                // forward error as usual
                return Observable.error(throwable);
            }
        };
    }

    private Func1<Object, PropertySet> playableToPropertySetFunc(final Playable playable) {
        return new Func1<Object, PropertySet>() {
            @Override
            public PropertySet call(Object soundAssociation) {
                return createPropertySetFromLike(playable);
            }
        };
    }

    private PropertySet createPropertySetFromLike(Playable playable) {
        return PropertySet.from(
                PlayableProperty.IS_LIKED.bind(playable.user_like),
                PlayableProperty.LIKES_COUNT.bind(playable.likes_count));
    }

    private APIRequest buildRequestForLike(final Playable playable, final boolean likeAdded) {
        APIEndpoints endpoint = playable instanceof PublicApiTrack ? APIEndpoints.MY_TRACK_LIKES : APIEndpoints.MY_PLAYLIST_LIKES;
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

    private Observable<SoundAssociation> repost(final Playable playable) {
        logPlayable("REPOST", playable);
        return httpClient.fetchResponse(buildRequestForReposts(playable, true)).mergeMap(mapAddRepostResponse(playable))
                .doOnCompleted(handleRepostStateChanged(playable, true));
    }

    private Observable<SoundAssociation> unrepost(final Playable playable) {
        logPlayable("UNREPOST", playable);
        return httpClient.fetchResponse(buildRequestForReposts(playable, false)).mergeMap(mapRemoveRepostResponse(playable))
                .onErrorResumeNext(handle404(soundAssocStorage.removeRepostAsync(playable)))
                .doOnCompleted(handleRepostStateChanged(playable, false));
    }

    private APIRequest buildRequestForReposts(final Playable playable, final boolean likeAdded) {
        APIEndpoints endpoint = playable instanceof PublicApiTrack ? APIEndpoints.MY_TRACK_REPOSTS : APIEndpoints.MY_PLAYLIST_REPOSTS;
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

    // FIXME: the playable is written on a BG thread and read on the UI thread,
    // this might cause thread visibility issues.
    private Action0 handleRepostStateChanged(final Playable playable, final boolean isReposted) {
        return new Action0() {
            @Override
            public void call() {
                logPlayable("CACHE/PUBLISH", playable);
                modelManager.cache(playable, PublicApiResource.CacheUpdateMode.NONE);
                Log.d(TAG, "publishing playable change event");
                eventBus.publish(EventQueue.PLAYABLE_CHANGED, PlayableChangedEvent.forRepost(playable.getUrn(), isReposted, playable.reposts_count));
            }
        };
    }

    private void logPlayable(String action, final Playable playable) {
        Log.d(TAG, Thread.currentThread().getName() + "|" + action + " for playable " + playable.getId() + ": liked = "
                + playable.user_like + "; likes = " + playable.likes_count + "; reposted = " + playable.user_repost
                + "; reposts = " + playable.reposts_count);
    }
}
