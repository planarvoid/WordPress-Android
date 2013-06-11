package com.soundcloud.android.operations;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.dao.UserAssociationStorage;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.rx.schedulers.ScheduledOperations;
import com.soundcloud.android.service.sync.SyncStateManager;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

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

    public Observable<Void> addFollowings(final List<User> users) {

        final boolean hadNoFollowings = mFollowStatus.isEmpty();
        updateLocalStatus(users, true);

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

    public Observable<Void> removeFollowings(final List<User> users) {
        updateLocalStatus(users, false);

        return schedule(Observable.create(new Func1<Observer<Void>, Subscription>() {
            @Override
            public Subscription call(Observer<Void> observer) {
                mUserAssociationStorage.removeFollowings(users);
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    private void updateLocalStatus(List<User> users, boolean newStatus) {
        // update followings ID cache
        mFollowStatus.toggleFollowing(users.toArray(new User[users.size()]));
        // make sure the cache reflects the new state of each following
        for (User user : users) {
            mModelManager.cache(user, ScResource.CacheUpdateMode.NONE).user_following = newStatus;
        }
    }

}