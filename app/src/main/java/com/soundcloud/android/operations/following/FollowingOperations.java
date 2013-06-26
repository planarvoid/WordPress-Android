package com.soundcloud.android.operations.following;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.dao.UserAssociationStorage;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.SuggestedUser;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.rx.schedulers.ScheduledOperations;
import com.soundcloud.android.service.sync.SyncStateManager;
import org.jetbrains.annotations.NotNull;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import java.util.ArrayList;
import java.util.List;

public class FollowingOperations extends ScheduledOperations {

    private final FollowStatus mFollowStatus;
    private final SyncStateManager mSyncStateManager;
    private final ScModelManager mModelManager;
    private final UserAssociationStorage mUserAssociationStorage;

    public FollowingOperations() {
        this(new UserAssociationStorage(), FollowStatus.get(), new SyncStateManager(), SoundCloudApplication.MODEL_MANAGER);
    }

    public FollowingOperations(UserAssociationStorage userAssociationStorage, FollowStatus followStatus,
                               SyncStateManager syncStateManager, ScModelManager modelManager){
        mUserAssociationStorage = userAssociationStorage;
        mFollowStatus = followStatus;
        mSyncStateManager = syncStateManager;
        mModelManager = modelManager;
    }

    public Observable<Void> addFollowing(@NotNull final User user){
        return schedule(Observable.create(new Func1<Observer<Void>, Subscription>() {
            @Override
            public Subscription call(Observer<Void> observer) {
                updateLocalStatus(true, user.getId());
                mUserAssociationStorage.addFollowing(user);
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    public Observable<Void> addFollowingBySuggestedUser(@NotNull final SuggestedUser suggestedUser){
        return schedule(Observable.create(new Func1<Observer<Void>, Subscription>() {
            @Override
            public Subscription call(Observer<Void> observer) {
                updateLocalStatus(true, suggestedUser.getId());
                mUserAssociationStorage.addFollowingBySuggestedUser(suggestedUser);
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    public Observable<Void> addFollowings(final List<User> users) {
        return schedule(Observable.create(new Func1<Observer<Void>, Subscription>() {
            @Override
            public Subscription call(Observer<Void> observer) {
                updateLocalStatus(true, ScModel.getIdList(users));
                mUserAssociationStorage.addFollowings(users);
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    public Observable<Void> removeFollowing(final User user) {
        return schedule(Observable.create(new Func1<Observer<Void>, Subscription>() {
            @Override
            public Subscription call(Observer<Void> observer) {
                updateLocalStatus(false, user.getId());
                mUserAssociationStorage.removeFollowing(user);
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    public Observable<Void> removeFollowings(final List<User> users) {
        return schedule(Observable.create(new Func1<Observer<Void>, Subscription>() {
            @Override
            public Subscription call(Observer<Void> observer) {
                updateLocalStatus(false, ScModel.getIdList(users));
                mUserAssociationStorage.removeFollowings(users);
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    public Observable<Void> addFollowingsBySuggestedUsers(final List<SuggestedUser> suggestedUsers) {
        return schedule(Observable.create(new Func1<Observer<Void>, Subscription>() {
            @Override
            public Subscription call(Observer<Void> observer) {
                updateLocalStatus(true, ScModel.getIdList(suggestedUsers));
                mUserAssociationStorage.addFollowingsBySuggestedUsers(suggestedUsers);
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    public Observable<Void> removeFollowingsBySuggestedUsers(List<SuggestedUser> suggestedUsers) {
        return removeFollowings(Lists.transform(suggestedUsers,new Function<SuggestedUser, User>() {
            @Override
            public User apply(SuggestedUser input) {
                return new User(input);
            }
        }));
    }

    public Observable<Void> toggleFollowing(User user) {
        if (mFollowStatus.isFollowing(user)){
            return removeFollowing(user);
        } else {
            return addFollowing(user);
        }
    }

    public Observable<Void> toggleFollowingBySuggestedUser(SuggestedUser suggestedUser) {
        if (mFollowStatus.isFollowing(suggestedUser.getId())){
            return removeFollowing(new User(suggestedUser));
        } else {
            return addFollowingBySuggestedUser(suggestedUser);
        }
    }

    private List<User> getUsersFromSuggestedUsers(List<SuggestedUser> suggestedUsers) {
        List<User> users = new ArrayList<User>(suggestedUsers.size());
        for (SuggestedUser suggestedUser : suggestedUsers){
            users.add(new User(suggestedUser));
        }
        return users;
    }

    private void updateLocalStatus(boolean newStatus, long... userIds) {
        final boolean hadNoFollowings = mFollowStatus.isEmpty();
        // update followings ID cache
        mFollowStatus.toggleFollowing(userIds);

        // first follower, set the stream to stale so next time the users goes there it will sync
        if (hadNoFollowings && userIds.length > 0) mSyncStateManager.forceToStale(Content.ME_SOUND_STREAM);

        // make sure the cache reflects the new state of each following
        for (long userId : userIds) {
            final User cachedUser = mModelManager.getCachedUser(userId);
            if (cachedUser != null) cachedUser.user_following = newStatus;
        }
    }

}