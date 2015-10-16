package com.soundcloud.android.associations;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.api.legacy.model.UserAssociation;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.LegacyUserAssociationStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.java.collections.MoreCollections;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Func1;

import android.support.annotation.VisibleForTesting;
import android.util.Log;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collection;

public class FollowingOperations {

    private static final String LOG_TAG = FollowingOperations.class.getSimpleName();

    private final FollowStatus followStatus;
    private final SyncStateManager syncStateManager;
    private final ScModelManager modelManager;
    private final LegacyUserAssociationStorage legacyUserAssociationStorage;
    private final ApiClientRx apiClientRx;
    private final SyncInitiator syncInitiator;
    private final Scheduler scheduler;

    @Inject
    public FollowingOperations(ApiClientRx apiClientRx, LegacyUserAssociationStorage legacyUserAssociationStorage,
                               SyncStateManager syncStateManager,
                               ScModelManager modelManager, SyncInitiator syncInitiator, @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.apiClientRx = apiClientRx;
        this.legacyUserAssociationStorage = legacyUserAssociationStorage;
        this.syncStateManager = syncStateManager;
        this.scheduler = scheduler;
        this.followStatus = FollowStatus.get();
        this.modelManager = modelManager;
        this.syncInitiator = syncInitiator;
    }

    @VisibleForTesting
    FollowingOperations(ApiClientRx apiClientRx, LegacyUserAssociationStorage legacyUserAssociationStorage,
                        SyncStateManager syncStateManager, FollowStatus followStatus,
                        ScModelManager modelManager, SyncInitiator syncInitiator, Scheduler scheduler) {
        this.apiClientRx = apiClientRx;
        this.legacyUserAssociationStorage = legacyUserAssociationStorage;
        this.syncStateManager = syncStateManager;
        this.followStatus = followStatus;
        this.modelManager = modelManager;
        this.syncInitiator = syncInitiator;
        this.scheduler = scheduler;
    }

    public Observable<Boolean> addFollowing(@NotNull final PublicApiUser user) {
        updateLocalStatus(true, user.getId());
        return legacyUserAssociationStorage.follow(user).lift(new ToggleFollowOperator(user.getUrn(), true));
    }

    public Observable<Boolean> removeFollowing(final PublicApiUser user) {
        updateLocalStatus(false, user.getId());
        return legacyUserAssociationStorage.unfollow(user).lift(new ToggleFollowOperator(user.getUrn(), false));
    }

    public Observable<Boolean> toggleFollowing(PublicApiUser user) {
        if (followStatus.isFollowing(user)) {
            return removeFollowing(user);
        } else {
            return addFollowing(user);
        }
    }

    public Observable<Collection<UserAssociation>> bulkFollowAssociations(final Collection<UserAssociation> userAssociations) {
        final ApiRequest apiRequest = createBulkFollowApiRequest(userAssociations);
        if (apiRequest == null) {
            Log.d(LOG_TAG, "No api request, skipping bulk follow");
            return Observable.empty();
        } else {
            Log.d(LOG_TAG, "Executing bulk follow request: " + apiRequest);
            return apiClientRx.response(apiRequest).subscribeOn(scheduler).flatMap(new Func1<ApiResponse, Observable<Collection<UserAssociation>>>() {
                @Override
                public Observable<Collection<UserAssociation>> call(ApiResponse apiResponse) {
                    Log.d(LOG_TAG, "Bulk follow request returned with response: " + apiResponse);
                    return legacyUserAssociationStorage.setFollowingsAsSynced(userAssociations);
                }
            });
        }

    }

    public boolean isFollowing(Urn urn) {
        return followStatus.isFollowing(urn);
    }

    public void requestUserFollowings(FollowStatusChangedListener listener) {
        followStatus.requestUserFollowings(listener);
    }

    public static void init() {
        FollowStatus.get();
    }

    public static void clearState() {
        FollowStatus.clearState();
    }

    public void updateLocalStatus(boolean shouldFollow, long... userIds) {
        final boolean hadNoFollowings = followStatus.isEmpty();
        // update followings ID cache
        if (shouldFollow) {
            followStatus.addFollowing(userIds);
        } else {
            followStatus.removeFollowing(userIds);
        }
        // make sure the cache reflects the new state of each following
        for (long userId : userIds) {
            final PublicApiUser cachedUser = modelManager.getCachedUser(userId);
            if (cachedUser != null) {
                cachedUser.user_following = shouldFollow;
            }
        }
        // invalidate stream SyncState if necessary
        if (hadNoFollowings && !followStatus.isEmpty()) {
            fireAndForget(syncStateManager.forceToStaleAsync(Content.ME_SOUND_STREAM));
        }
    }

    @Nullable
    private ApiRequest createBulkFollowApiRequest(final Collection<UserAssociation> userAssociations) {
        final Collection<UserAssociation> associationsWithTokens = MoreCollections.filter(userAssociations, UserAssociation.HAS_TOKEN_PREDICATE);
        final Collection<String> tokens = MoreCollections.transform(associationsWithTokens, UserAssociation.TO_TOKEN_FUNCTION);
        if (!tokens.isEmpty()) {
            return ApiRequest.post(ApiEndpoints.BULK_FOLLOW_USERS.path())
                    .forPublicApi()
                    .withContent(new BulkFollowingsHolder(tokens))
                    .build();
        }
        return null;
    }

    public interface FollowStatusChangedListener {
        void onFollowChanged();
    }

    @VisibleForTesting
    static class BulkFollowingsHolder {
        @JsonProperty
        Collection<String> tokens;

        public BulkFollowingsHolder(Collection<String> tokens) {
            this.tokens = tokens;
        }

        @Override
        public String toString() {
            return "BulkFollowingsHolder{" +
                    "tokensCount=" + tokens.size() +
                    '}';
        }
    }

    private final class ToggleFollowOperator implements Observable.Operator<Boolean, UserAssociation> {

        private final Urn userUrn;
        private final boolean successState;

        ToggleFollowOperator(Urn userUrn, boolean successState) {
            this.userUrn = userUrn;
            this.successState = successState;
        }

        @Override
        public Subscriber<? super UserAssociation> call(final Subscriber<? super Boolean> subscriber) {
            return new Subscriber<UserAssociation>() {
                @Override
                public void onCompleted() {
                    syncInitiator.pushFollowingsToApi();
                    subscriber.onCompleted();
                }

                @Override
                public void onError(Throwable e) {
                    subscriber.onNext(isFollowing(userUrn));
                }

                @Override
                public void onNext(UserAssociation userAssociation) {
                    subscriber.onNext(successState);
                }
            };
        }
    }
}
