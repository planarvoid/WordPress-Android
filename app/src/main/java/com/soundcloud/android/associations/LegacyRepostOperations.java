package com.soundcloud.android.associations;

import static com.soundcloud.android.api.ApiRequestException.Reason.NOT_FOUND;

import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.ApiScheduler;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.LegacyPlaylistOperations;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.storage.SoundAssociationStorage;
import com.soundcloud.android.storage.TrackStorage;
import com.soundcloud.android.utils.Log;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;

/**
 * Provides operations for toggling repost status based on track/playlist URN and returns changes
 * as property sets.
 */
@Deprecated
public class LegacyRepostOperations implements RepostCreator {

    public static final String TAG = LegacyRepostOperations.class.getSimpleName();

    private final EventBus eventBus;
    private final SoundAssociationStorage soundAssocStorage;
    private final ApiScheduler apiScheduler;
    private final ScModelManager modelManager;
    private final TrackStorage trackStorage;
    private final LegacyPlaylistOperations legacyPlaylistOperations;

    @Inject
    public LegacyRepostOperations(
            EventBus eventBus,
            SoundAssociationStorage soundAssocStorage,
            ApiScheduler apiScheduler,
            ScModelManager modelManager,
            TrackStorage trackStorage,
            LegacyPlaylistOperations legacyPlaylistOperations) {
        this.eventBus = eventBus;
        this.soundAssocStorage = soundAssocStorage;
        this.apiScheduler = apiScheduler;
        this.modelManager = modelManager;
        this.trackStorage = trackStorage;
        this.legacyPlaylistOperations = legacyPlaylistOperations;
    }

   public Observable<PropertySet> toggleRepost(final Urn soundUrn, final boolean addRepost) {
        return resolveLegacyModel(soundUrn)
                .flatMap(new Func1<Playable, Observable<PropertySet>>() {
                    @Override
                    public Observable<PropertySet> call(Playable playable) {
                        return toggleRepost(playable, addRepost);
                    }
                });
    }

    private Observable<PropertySet> toggleRepost(final Playable playable, final boolean addRepost) {
        logPlayable(addRepost ? "REPOST" : "UNPOST", playable);
        return updateRepostState(playable, addRepost)
                .flatMap(new Func1<SoundAssociation, Observable<ApiResponse>>() {
                    @Override
                    public Observable<ApiResponse> call(SoundAssociation soundAssociation) {
                        return apiScheduler.response(buildRequestForRepost(playable, addRepost));
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
                eventBus.publish(EventQueue.ENTITY_STATE_CHANGED, EntityStateChangedEvent.fromRepost(updated.getUrn(), updated.user_repost));
            }
        };
    }

    /*
     * A repost could have been removed on the server but not yet synced to the client.
     * If we unposted and get a 404 then do not revert because client is already in correct state.
     */
    private Func1<Throwable, Observable<PropertySet>> handleRepostRequestError(final Playable playable, final boolean wasAddRequest) {
        return new Func1<Throwable, Observable<PropertySet>>() {
            @Override
            public Observable<PropertySet> call(Throwable throwable) {
                if (throwable instanceof ApiRequestException) {
                    ApiRequestException requestException = (ApiRequestException) throwable;
                    if (!wasAddRequest && requestException.reason() == NOT_FOUND) {
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

    private ApiRequest buildRequestForRepost(final Playable playable, final boolean repostAdded) {
        ApiEndpoints endpoint = playable instanceof PublicApiTrack ? ApiEndpoints.MY_TRACK_REPOSTS : ApiEndpoints.MY_PLAYLIST_REPOSTS;
        final String path = endpoint.path(playable.getId());
        ApiRequest.Builder builder = repostAdded ? ApiRequest.Builder.put(path) : ApiRequest.Builder.delete(path);
        return builder.forPublicApi().build();
    }

    private Observable<? extends Playable> resolveLegacyModel(final Urn urn) {
        return urn.isTrack()
                ? trackStorage.getTrackAsync(urn.getNumericId())
                : legacyPlaylistOperations.loadPlaylist(urn);
    }

    private void logPlayable(String action, final Playable playable) {
        Log.d(TAG, Thread.currentThread().getName() + "|" + action + " for playable " + playable.getId() + ": liked = "
                + playable.user_like + "; likes = " + playable.likes_count + "; reposted = " + playable.user_repost
                + "; reposts = " + playable.reposts_count);
    }

}
