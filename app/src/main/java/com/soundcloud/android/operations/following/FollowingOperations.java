package com.soundcloud.android.operations.following;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.dao.UserAssociationStorage;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
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
        final boolean hadNoFollowings = mFollowStatus.isEmpty();
        updateLocalStatus(true, user);

        return schedule(Observable.create(new Func1<Observer<Void>, Subscription>() {
            @Override
            public Subscription call(Observer<Void> observer) {
                // first follower, set the stream to stale so next time the users goes there it will sync
                if (hadNoFollowings) mSyncStateManager.forceToStale(Content.ME_SOUND_STREAM);

                mUserAssociationStorage.addFollowing(user);
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    public Observable<Void> addFollowings(final List<User> users) {

        final boolean hadNoFollowings = mFollowStatus.isEmpty();
        updateLocalStatus(true, users.toArray(new User[users.size()]));

        return schedule(Observable.create(new Func1<Observer<Void>, Subscription>() {
            @Override
            public Subscription call(Observer<Void> observer) {
                // first follower, set the stream to stale so next time the users goes there it will sync
                if (hadNoFollowings && !users.isEmpty()) mSyncStateManager.forceToStale(Content.ME_SOUND_STREAM);

                mUserAssociationStorage.addFollowings(users);
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    public Observable<Void> removeFollowing(final User user) {
        updateLocalStatus(false, user);

        return schedule(Observable.create(new Func1<Observer<Void>, Subscription>() {
            @Override
            public Subscription call(Observer<Void> observer) {
                mUserAssociationStorage.removeFollowing(user);
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    public Observable<Void> removeFollowings(final List<User> users) {
        updateLocalStatus(false, users.toArray(new User[users.size()]));

        return schedule(Observable.create(new Func1<Observer<Void>, Subscription>() {
            @Override
            public Subscription call(Observer<Void> observer) {
                mUserAssociationStorage.removeFollowings(users);
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    public void toggleFollowing(User user) {
        if (mFollowStatus.isFollowing(user)){
            addFollowing(user);
        } else {
            removeFollowing(user);
        }
    }

    public Observable<Void> removeFollowingsBySuggestedUsers(List<SuggestedUser> suggestedUsers) {
        return removeFollowings(getUsersFromSuggestedUsers(suggestedUsers));
    }

    public Observable<Void> addFollowingsBySuggestedUsers(List<SuggestedUser> suggestedUsers) {
        return addFollowings(getUsersFromSuggestedUsers(suggestedUsers));
    }

    private List<User> getUsersFromSuggestedUsers(List<SuggestedUser> suggestedUsers) {
        List<User> users = new ArrayList<User>(suggestedUsers.size());
        for (SuggestedUser suggestedUser : suggestedUsers){
            users.add(new User(suggestedUser));
        }
        return users;
    }

    private void updateLocalStatus(boolean newStatus, User... users) {
        // update followings ID cache
        mFollowStatus.toggleFollowing(users);
        // make sure the cache reflects the new state of each following
        for (User user : users) {
            mModelManager.cache(user, ScResource.CacheUpdateMode.NONE).user_following = newStatus;
        }
    }

}