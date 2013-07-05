package com.soundcloud.android.operations.following;

import static com.google.common.collect.Collections2.filter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.APIEndpoints;
import com.soundcloud.android.api.http.APIRequest;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.api.http.SoundCloudAPIRequest;
import com.soundcloud.android.api.http.SoundCloudRxHttpClient;
import com.soundcloud.android.dao.UserAssociationStorage;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.SuggestedUser;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserAssociation;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.rx.ScActions;
import com.soundcloud.android.rx.schedulers.ScheduledOperations;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.service.sync.SyncStateManager;
import org.jetbrains.annotations.NotNull;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class FollowingOperations extends ScheduledOperations {

    private final FollowStatus mFollowStatus;
    private final SyncStateManager mSyncStateManager;
    private final ScModelManager mModelManager;
    private final UserAssociationStorage mUserAssociationStorage;
    private final RxHttpClient mRxHttpClient;

    public FollowingOperations() {
        this(new SoundCloudRxHttpClient(), new UserAssociationStorage(), new SyncStateManager(),
                FollowStatus.get(), SoundCloudApplication.MODEL_MANAGER);
    }

    /**
     * Use this constructor to have all observables that are delegated to run on the same scheduler. This is useful
     * if you know that work is already being done on a background thread and things can run synchronously on that
     * same thread.
     *
     * @param scheduler the scheduler to use for all internal observables
     */
    public FollowingOperations(Scheduler scheduler) {
        this(new SoundCloudRxHttpClient(scheduler), new UserAssociationStorage(scheduler, SoundCloudApplication.instance.getContentResolver()),
                new SyncStateManager(scheduler),
                FollowStatus.get(), SoundCloudApplication.MODEL_MANAGER);
    }

    // TODO, rollback memory state on error
    public FollowingOperations(RxHttpClient httpClient, UserAssociationStorage userAssociationStorage,
                               SyncStateManager syncStateManager, FollowStatus followStatus, ScModelManager modelManager) {
        mRxHttpClient = httpClient;
        mUserAssociationStorage = userAssociationStorage;
        mSyncStateManager = syncStateManager;
        mFollowStatus = followStatus;
        mModelManager = modelManager;
    }

    public Observable<UserAssociation> addFollowing(@NotNull final User user) {
        updateLocalStatus(true, user.getId());
        return mUserAssociationStorage.addFollowing(user);
    }

    public Observable<UserAssociation> addFollowingBySuggestedUser(@NotNull final SuggestedUser suggestedUser) {
        updateLocalStatus(true, suggestedUser.getId());
        return mUserAssociationStorage.addFollowingBySuggestedUser(suggestedUser);
    }

    public Observable<UserAssociation> addFollowings(final List<User> users) {
        updateLocalStatus(true, ScModel.getIdList(users));
        return mUserAssociationStorage.addFollowings(users);
    }

    public Observable<UserAssociation> removeFollowing(final User user) {
        updateLocalStatus(false, user.getId());
        return mUserAssociationStorage.removeFollowing(user);
    }

    public Observable<UserAssociation> removeFollowings(final List<User> users) {
        updateLocalStatus(false, ScModel.getIdList(users));
        return mUserAssociationStorage.removeFollowings(users);
    }

    public Observable<UserAssociation> addFollowingsBySuggestedUsers(final List<SuggestedUser> suggestedUsers) {
        updateLocalStatus(true, ScModel.getIdList(suggestedUsers));
        return mUserAssociationStorage.addFollowingsBySuggestedUsers(suggestedUsers);
    }

    public Observable<UserAssociation> removeFollowingsBySuggestedUsers(List<SuggestedUser> suggestedUsers) {
        return removeFollowings(Lists.transform(suggestedUsers, new Function<SuggestedUser, User>() {
            @Override
            public User apply(SuggestedUser input) {
                return new User(input);
            }
        }));
    }

    public Observable<UserAssociation> toggleFollowing(User user) {
        if (mFollowStatus.isFollowing(user)) {
            return removeFollowing(user);
        } else {
            return addFollowing(user);
        }
    }

    public Observable<UserAssociation> toggleFollowingBySuggestedUser(SuggestedUser suggestedUser) {
        if (mFollowStatus.isFollowing(suggestedUser.getId())) {
            return removeFollowing(new User(suggestedUser));
        } else {
            return addFollowingBySuggestedUser(suggestedUser);
        }
    }

    public Observable<Void> bulkFollowAssociations(final Collection<UserAssociation> userAssociations) {
        return createApiRequestObservable(userAssociations).flatMap(new Func1<APIRequest<Void>, Observable<Void>>() {
            @Override
            public Observable<Void> call(APIRequest<Void> request) {
                return mRxHttpClient.fetchModels(request);
            }
        });
    }

    private Observable<APIRequest<Void>> createApiRequestObservable(final Collection<UserAssociation> userAssociations) {
        return Observable.create(new Func1<Observer<APIRequest<Void>>, Subscription>() {
            @Override
            public Subscription call(Observer<APIRequest<Void>> apiRequestObserver) {
                final Collection<UserAssociation> associationsWithTokens = filter(userAssociations, UserAssociation.HAS_TOKEN_PREDICATE);
                final Collection<String> tokens = Collections2.transform(associationsWithTokens, UserAssociation.TO_TOKEN_FUNCTION);
                if (!tokens.isEmpty()) {
                    APIRequest<Void> request = SoundCloudAPIRequest.RequestBuilder.<Void>post(APIEndpoints.BULK_FOLLOW_USERS.path())
                            .forPublicAPI()
                            .withContent(new BulkFollowingsHolder(tokens))
                            .build();

                    apiRequestObserver.onNext(request);
                }
                apiRequestObserver.onCompleted();
                return Subscriptions.empty();
            }
        });
    }

    public Observable<Void> pushFollowings(final Context context) {
        return schedule(Observable.create(new Func1<Observer<Void>, Subscription>() {
            @Override
            public Subscription call(final Observer<Void> observer) {
                log("Pushing followings...");

                final BooleanSubscription subscription = new BooleanSubscription();

                // make sure the result receiver is invoked on the main thread, that's because after a sync error
                // we call through to the observer directly rather than going through a scheduler
                final ResultReceiver receiver = new ResultReceiver(new Handler(Looper.getMainLooper())) {
                    @Override
                    protected void onReceiveResult(int resultCode, Bundle resultData) {
                        if (!subscription.isUnsubscribed()) {
                            handleSyncResult(resultCode, observer);
                        } else {
                            log("Not delivering results, was unsubscribed");
                        }
                    }
                };

                Intent intent = new Intent(context, ApiSyncService.class)
                        .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, receiver)
                        .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                        .setData(Content.ME_FOLLOWINGS.uri)
                        .setAction(ApiSyncService.ACTION_PUSH);
                context.startService(intent);
                return subscription;
            }

            private void handleSyncResult(int resultCode, Observer<Void> observer) {
                log("handleSyncResult");
                switch (resultCode) {
                    case ApiSyncService.STATUS_SYNC_FINISHED:
                        log("Sync successful!");
                        observer.onCompleted();
                        break;

                    case ApiSyncService.STATUS_SYNC_ERROR:
                        //TODO: Proper Syncer error handling
                        observer.onError(new Exception("Sync failed"));
                        break;

                    default:
                        throw new IllegalArgumentException("Unexpected syncer result code: " + resultCode);
                }
            }
        }));
    }

    public Set<Long> getFollowedUserIds() {
        return mFollowStatus.getFollowedUserIds();
    }

    public boolean isFollowing(User user) {
        return mFollowStatus.isFollowing(user);
    }

    public void requestUserFollowings(FollowStatusChangedListener listener) {
        mFollowStatus.requestUserFollowings(listener);
    }

    public static void clearState() {
        FollowStatus.clearState();
    }


    private void updateLocalStatus(boolean shouldFollow, long... userIds) {
        final boolean hadNoFollowings = mFollowStatus.isEmpty();
        // update followings ID cache
        mFollowStatus.toggleFollowing(userIds);
        // make sure the cache reflects the new state of each following
        for (long userId : userIds) {
            final User cachedUser = mModelManager.getCachedUser(userId);
            if (cachedUser != null) cachedUser.user_following = shouldFollow;
        }
        // invalidate stream SyncState if necessary
        if (hadNoFollowings && !mFollowStatus.isEmpty()) {
            mSyncStateManager.forceToStale(Content.ME_SOUND_STREAM).subscribe(ScActions.NO_OP);
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
    }

}