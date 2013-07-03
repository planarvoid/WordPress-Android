package com.soundcloud.android.operations.following;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.dao.UserAssociationStorage;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.SuggestedUser;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserAssociation;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.rx.schedulers.ScheduledOperations;
import com.soundcloud.android.service.sync.SyncStateManager;
import org.jetbrains.annotations.NotNull;
import rx.Observable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    public Observable<UserAssociation> addFollowing(@NotNull final User user){
        updateLocalStatus(true, user.getId());
        return mUserAssociationStorage.addFollowing(user);
    }

    public Observable<UserAssociation> addFollowingBySuggestedUser(@NotNull final SuggestedUser suggestedUser){
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
        return removeFollowings(Lists.transform(suggestedUsers,new Function<SuggestedUser, User>() {
            @Override
            public User apply(SuggestedUser input) {
                return new User(input);
            }
        }));
    }

    public Observable<UserAssociation> toggleFollowing(User user) {
        if (mFollowStatus.isFollowing(user)){
            return removeFollowing(user);
        } else {
            return addFollowing(user);
        }
    }

    public Observable<UserAssociation> toggleFollowingBySuggestedUser(SuggestedUser suggestedUser) {
        if (mFollowStatus.isFollowing(suggestedUser.getId())){
            return removeFollowing(new User(suggestedUser));
        } else {
            return addFollowingBySuggestedUser(suggestedUser);
        }
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

    public interface FollowStatusChangedListener {
        void onFollowChanged();
    }
}