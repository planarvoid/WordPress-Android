package com.soundcloud.android.associations;

import static com.google.common.collect.Collections2.filter;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.APIRequest;
import com.soundcloud.android.api.APIResponse;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.SoundCloudAPIRequest;
import com.soundcloud.android.api.SoundCloudRxHttpClient;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.TempEndpoints;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.api.legacy.model.UserAssociation;
import com.soundcloud.android.api.legacy.model.activities.Activities;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.onboarding.suggestions.SuggestedUser;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.storage.UserAssociationStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.android.users.UserUrn;
import com.soundcloud.api.Request;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Func1;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class FollowingOperations {

    private static final String LOG_TAG = FollowingOperations.class.getSimpleName();

    private final FollowStatus followStatus;
    private final SyncStateManager syncStateManager;
    private final ScModelManager modelManager;
    private final UserAssociationStorage userAssociationStorage;
    private final RxHttpClient rxHttpClient;
    private final SyncInitiator syncInitiator;

    @Deprecated
    public FollowingOperations() {
        this(new SoundCloudRxHttpClient(), new UserAssociationStorage(SoundCloudApplication.instance),
                new SyncStateManager(SoundCloudApplication.instance),
                FollowStatus.get(), SoundCloudApplication.sModelManager,
                new SyncInitiator(SoundCloudApplication.instance, SoundCloudApplication.instance.getAccountOperations()));
    }

    /**
     * Use this constructor to have all observables that are delegated to run on the same scheduler. This is useful
     * if you know that work is already being done on a background thread and things can run synchronously on that
     * same thread.
     *
     * @param scheduler the scheduler to use for all internal observables
     */
    @Deprecated
    public FollowingOperations(Scheduler scheduler) {
        this(new SoundCloudRxHttpClient(scheduler), new UserAssociationStorage(scheduler, SoundCloudApplication.instance.getContentResolver()),
                new SyncStateManager(SoundCloudApplication.instance),
                FollowStatus.get(), SoundCloudApplication.sModelManager,
                new SyncInitiator(SoundCloudApplication.instance, SoundCloudApplication.instance.getAccountOperations()));
    }

    public FollowingOperations(RxHttpClient httpClient, UserAssociationStorage userAssociationStorage,
                               SyncStateManager syncStateManager, FollowStatus followStatus,
                               ScModelManager modelManager, SyncInitiator syncInitiator) {
        this.rxHttpClient = httpClient;
        this.userAssociationStorage = userAssociationStorage;
        this.syncStateManager = syncStateManager;
        this.followStatus = followStatus;
        this.modelManager = modelManager;
        this.syncInitiator = syncInitiator;
    }

    public Observable<Boolean> addFollowing(@NotNull final PublicApiUser user) {
        updateLocalStatus(true, user.getId());
        return userAssociationStorage.follow(user).lift(new ToggleFollowOperator(user.getUrn(), true));
    }

    public Observable<UserAssociation> addFollowingBySuggestedUser(@NotNull final SuggestedUser suggestedUser) {
        updateLocalStatus(true, suggestedUser.getId());
        return userAssociationStorage.followSuggestedUser(suggestedUser);
    }

    public Observable<Boolean> removeFollowing(final PublicApiUser user) {
        updateLocalStatus(false, user.getId());
        return userAssociationStorage.unfollow(user).lift(new ToggleFollowOperator(user.getUrn(), false));
    }

    public Observable<UserAssociation> addFollowingsBySuggestedUsers(final List<SuggestedUser> suggestedUsers) {
        updateLocalStatus(true, ScModel.getIdList(suggestedUsers));
        return userAssociationStorage.followSuggestedUserList(suggestedUsers);
    }

    public Observable<UserAssociation> removeFollowingsBySuggestedUsers(List<SuggestedUser> suggestedUsers) {
        return removeFollowings(Lists.transform(suggestedUsers, new Function<SuggestedUser, PublicApiUser>() {
            @Override
            public PublicApiUser apply(SuggestedUser input) {
                return new PublicApiUser(input);
            }
        }));
    }

    private Observable<UserAssociation> removeFollowings(final List<PublicApiUser> users) {
        updateLocalStatus(false, ScModel.getIdList(users));
        return userAssociationStorage.unfollowList(users);
    }

    public Observable<Boolean> toggleFollowing(PublicApiUser user) {
        if (followStatus.isFollowing(user)) {
            return removeFollowing(user);
        } else {
            return addFollowing(user);
        }
    }

    public Observable<UserAssociation> toggleFollowingBySuggestedUser(SuggestedUser suggestedUser) {
        if (followStatus.isFollowing(suggestedUser.getUrn())) {
            return removeFollowingsBySuggestedUsers(Arrays.asList(suggestedUser));
        } else {
            return addFollowingBySuggestedUser(suggestedUser);
        }
    }

    public Observable<Collection<UserAssociation>> bulkFollowAssociations(final Collection<UserAssociation> userAssociations) {
        final APIRequest<Void> apiRequest = createBulkFollowApiRequest(userAssociations);
        if (apiRequest == null) {
            Log.d(LOG_TAG, "No api request, skipping bulk follow");
            return Observable.empty();
        } else {
            Log.d(LOG_TAG, "Executing bulk follow request: " + apiRequest);
            return rxHttpClient.fetchResponse(apiRequest).flatMap(new Func1<APIResponse, Observable<Collection<UserAssociation>>>() {
                @Override
                public Observable<Collection<UserAssociation>> call(APIResponse apiResponse) {
                    Log.d(LOG_TAG, "Bulk follow request returned with response: " + apiResponse);
                    return userAssociationStorage.setFollowingsAsSynced(userAssociations);
                }
            });
        }

    }

    @Nullable
    private APIRequest<Void> createBulkFollowApiRequest(final Collection<UserAssociation> userAssociations) {
        final Collection<UserAssociation> associationsWithTokens = filter(userAssociations, UserAssociation.HAS_TOKEN_PREDICATE);
        final Collection<String> tokens = Collections2.transform(associationsWithTokens, UserAssociation.TO_TOKEN_FUNCTION);
        if (!tokens.isEmpty()) {
            return SoundCloudAPIRequest.RequestBuilder.<Void>post(APIEndpoints.BULK_FOLLOW_USERS.path())
                    .forPublicAPI()
                    .withContent(new BulkFollowingsHolder(tokens))
                    .build();
        }
        return null;
    }

    public Observable<Boolean> waitForActivities(final Context context) {
        return getFollowingsNeedingSync().toList().flatMap(new Func1<List<UserAssociation>, Observable<Collection<UserAssociation>>>() {
            @Override
            public Observable<Collection<UserAssociation>> call(List<UserAssociation> userAssociations) {
                return bulkFollowAssociations(userAssociations);
            }
        }).flatMap(new Func1<Collection<UserAssociation>, Observable<Boolean>>() {
            @Override
            public Observable<Boolean> call(Collection<UserAssociation> userAssociations) {
                return fetchActivities(new PublicApi(context));
            }
        });
    }

    //TODO: didn't have enough time porting this over, next time :)
    // couldn't write tests either since Activities.fetch isn't mockable :(
    private Observable<Boolean> fetchActivities(final PublicApi api) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                try {
                    boolean hasActivities = false;
                    int attempts = 15;
                    final long backoffTime = 2000;
                    while (!hasActivities && attempts > 0) {
                        Log.d(LOG_TAG, "Fetching activities; tries left = " + attempts);
                        attempts--;
                        Activities activities = Activities.fetch(api, Request.to(TempEndpoints.e1.MY_STREAM));
                        hasActivities = activities != null && !activities.isEmpty();
                        Log.d(LOG_TAG, "Has activities = " + hasActivities);
                        if (!hasActivities) {
                            Log.d(LOG_TAG, "Sleeping for " + backoffTime);
                            SystemClock.sleep(backoffTime);
                        }
                    }
                    subscriber.onNext(hasActivities);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(ScSchedulers.API_SCHEDULER);
    }

    private Observable<UserAssociation> getFollowingsNeedingSync() {
        return Observable.create(new Observable.OnSubscribe<UserAssociation>() {
            @Override
            public void call(Subscriber<? super UserAssociation> subscriber) {
                RxUtils.emitIterable(subscriber, userAssociationStorage.getFollowingsNeedingSync());
                subscriber.onCompleted();
            }
        });
    }

    public Set<Long> getFollowedUserIds() {
        return followStatus.getFollowedUserIds();
    }

    public boolean isFollowing(UserUrn urn) {
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
        followStatus.toggleFollowing(userIds);
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

    public interface FollowStatusChangedListener {
        void onFollowChanged();
    }

    @VisibleForTesting
    public static class BulkFollowingsHolder {
        public BulkFollowingsHolder(Collection<String> tokens) {
            this.tokens = tokens;
        }

        @JsonProperty
        Collection<String> tokens;

        @Override
        public String toString() {
            return "BulkFollowingsHolder{" +
                    "tokensCount=" + tokens.size() +
                    '}';
        }
    }

    private final class ToggleFollowOperator implements Observable.Operator<Boolean, UserAssociation> {

        private final UserUrn userUrn;
        private final boolean successState;

        ToggleFollowOperator(UserUrn userUrn, boolean successState) {
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
