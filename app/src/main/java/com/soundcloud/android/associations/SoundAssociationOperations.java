package com.soundcloud.android.associations;

import static com.soundcloud.android.api.SoundCloudAPIRequest.RequestBuilder;

import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.APIRequestException;
import com.soundcloud.android.api.APIResponse;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableUpdatedEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.LegacyPlaylistOperations;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.storage.SoundAssociationStorage;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.utils.Log;
import com.soundcloud.propeller.PropertySet;
import org.apache.http.HttpStatus;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.List;

/**
 * Provides operations for toggling like and repost status based on track/playlist URN and returns changes
 * as property sets.
 *
 * TODO: Remove internal dependency on legacy storage classes!
 */
public class SoundAssociationOperations {

    public static final String TAG = "SoundAssociations";

    private final EventBus eventBus;
    private final SoundAssociationStorage soundAssocStorage;
    private final RxHttpClient httpClient;
    private final ScModelManager modelManager;
    private final TrackStorage trackStorage;
    private final LegacyPlaylistOperations legacyPlaylistOperations;

    @Inject
    public SoundAssociationOperations(
            EventBus eventBus,
            SoundAssociationStorage soundAssocStorage,
            RxHttpClient httpClient,
            ScModelManager modelManager,
            TrackStorage trackStorage,
            LegacyPlaylistOperations legacyPlaylistOperations) {
        this.eventBus = eventBus;
        this.soundAssocStorage = soundAssocStorage;
        this.httpClient = httpClient;
        this.modelManager = modelManager;
        this.trackStorage = trackStorage;
        this.legacyPlaylistOperations = legacyPlaylistOperations;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // LIKING / UN-LIKING
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Observable<List<Urn>> getLikedTracks() {
        return soundAssocStorage.getLikesTrackUrnsAsync();
    }

    public Observable<PropertySet> toggleLike(final Urn soundUrn, final boolean addLike) {
        return resolveLegacyModel(soundUrn)
                .mergeMap(new Func1<Playable, Observable<PropertySet>>() {
                    @Override
                    public Observable<PropertySet> call(Playable playable) {
                        return toggleLike(playable, addLike);
                    }
                });
    }

    private Observable<PropertySet> toggleLike(final Playable playable, final boolean addLike) {
        logPlayable(addLike ? "LIKE" : "UNLIKE", playable);
        return updateLikeState(playable, addLike)
                .mergeMap(new Func1<SoundAssociation, Observable<APIResponse>>() {
                    @Override
                    public Observable<APIResponse> call(SoundAssociation soundAssociation) {
                        return httpClient.fetchResponse(buildRequestForLike(playable, addLike));
                    }
                })
                .map(playableToLikePropertiesFunc(playable))
                .onErrorResumeNext(handleLikeRequestError(playable, addLike));
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
                eventBus.publish(EventQueue.PLAYABLE_CHANGED, PlayableUpdatedEvent.forLike(updated.getUrn(), updated.user_like, updated.likes_count));
            }
        };
    }

    /*
     * A like could have been removed on the server but not yet synced to the client.
     * If we unliked and get a 404 then do not revert because client is already in correct state.
     */
    private Func1<Throwable, Observable<PropertySet>> handleLikeRequestError(final Playable playable, final boolean wasAddRequest) {
        return new Func1<Throwable, Observable<PropertySet>>() {
            @Override
            public Observable<PropertySet> call(Throwable throwable) {
                if (throwable instanceof APIRequestException) {
                    APIRequestException requestException = (APIRequestException) throwable;
                    if (!wasAddRequest && requestException.response() != null
                            && requestException.response().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                        Log.d(TAG, "Unliking a track that was not liked on server. Already in correct state.");
                        return Observable.just(createPropertySetFromLike(playable));
                    }
                }
                return updateLikeState(playable, !wasAddRequest).map(playableToLikePropertiesFunc(playable));
            }
        };
    }

    private Func1<Object, PropertySet> playableToLikePropertiesFunc(final Playable playable) {
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

    public Observable<PropertySet> toggleRepost(final Urn soundUrn, final boolean addRepost) {
        return resolveLegacyModel(soundUrn)
                .mergeMap(new Func1<Playable, Observable<PropertySet>>() {
                    @Override
                    public Observable<PropertySet> call(Playable playable) {
                        return toggleRepost(playable, addRepost);
                    }
                });
    }

    private Observable<PropertySet> toggleRepost(final Playable playable, final boolean addRepost) {
        logPlayable(addRepost ? "REPOST" : "UNPOST", playable);
        return updateRepostState(playable, addRepost)
                .mergeMap(new Func1<SoundAssociation, Observable<APIResponse>>() {
                    @Override
                    public Observable<APIResponse> call(SoundAssociation soundAssociation) {
                        return httpClient.fetchResponse(buildRequestForRepost(playable, addRepost));
                    }
                })
                .map(playableToRepostPropertiesFunc(playable))
                .onErrorResumeNext(handleRepostRequestError(playable, addRepost));
    }

    private Observable<SoundAssociation> updateRepostState(Playable playable, boolean addRepost) {
        Observable<SoundAssociation> updateObservable = addRepost
                ? soundAssocStorage.addRepostAsync(playable)
                : soundAssocStorage.removeRepostAsync(playable);
        return updateObservable.doOnNext(cacheAndPublishRepost());
    }

    private Action1<SoundAssociation> cacheAndPublishRepost() {
        return new Action1<SoundAssociation>() {
            @Override
            public void call(SoundAssociation soundAssociation) {
                Playable updated = soundAssociation.getPlayable();
                logPlayable("CACHE/PUBLISH", updated);
                modelManager.cache(updated, PublicApiResource.CacheUpdateMode.NONE);
                eventBus.publish(EventQueue.PLAYABLE_CHANGED, PlayableUpdatedEvent.forRepost(updated.getUrn(), updated.user_repost, updated.reposts_count));
            }
        };
    }

    /*
     * See 404 handling explaination for likes above!
     */
    private Func1<Throwable, Observable<PropertySet>> handleRepostRequestError(final Playable playable, final boolean wasAddRequest) {
        return new Func1<Throwable, Observable<PropertySet>>() {
            @Override
            public Observable<PropertySet> call(Throwable throwable) {
                if (throwable instanceof APIRequestException) {
                    APIRequestException requestException = (APIRequestException) throwable;
                    if (!wasAddRequest && requestException.response() != null
                            && requestException.response().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                        Log.d(TAG, "Unposting a track that was not reposted on server. Already in correct state.");
                        return Observable.just(createPropertySetFromRepost(playable));
                    }
                }
                return updateRepostState(playable, !wasAddRequest).map(playableToRepostPropertiesFunc(playable));
            }
        };
    }

    private Func1<Object, PropertySet> playableToRepostPropertiesFunc(final Playable playable) {
        return new Func1<Object, PropertySet>() {
            @Override
            public PropertySet call(Object soundAssociation) {
                return createPropertySetFromRepost(playable);
            }
        };
    }

    private PropertySet createPropertySetFromRepost(Playable playable) {
        return PropertySet.from(
                PlayableProperty.IS_REPOSTED.bind(playable.user_repost),
                PlayableProperty.REPOSTS_COUNT.bind(playable.reposts_count));
    }

    private APIRequest buildRequestForRepost(final Playable playable, final boolean repostAdded) {
        APIEndpoints endpoint = playable instanceof PublicApiTrack ? APIEndpoints.MY_TRACK_REPOSTS : APIEndpoints.MY_PLAYLIST_REPOSTS;
        final String path = endpoint.path() + "/" + playable.getId();
        RequestBuilder builder = repostAdded ? RequestBuilder.put(path) : RequestBuilder.delete(path);
        return builder.forPublicAPI().build();
    }

    private Observable<? extends Playable> resolveLegacyModel(final Urn urn) {
        return urn.isTrack()
                ? trackStorage.getTrackAsync(urn.numericId)
                : legacyPlaylistOperations.loadPlaylist(urn);
    }

    private void logPlayable(String action, final Playable playable) {
        Log.d(TAG, Thread.currentThread().getName() + "|" + action + " for playable " + playable.getId() + ": liked = "
                + playable.user_like + "; likes = " + playable.likes_count + "; reposted = " + playable.user_repost
                + "; reposts = " + playable.reposts_count);
    }

}
