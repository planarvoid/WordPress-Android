package com.soundcloud.android.dao;

import static com.soundcloud.android.dao.ResolverHelper.getWhereInClause;
import static com.soundcloud.android.dao.ResolverHelper.longListToStringArr;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Association;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.SuggestedUser;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserAssociation;
import com.soundcloud.android.provider.BulkInsertMap;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.schedulers.ScheduledOperations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscription;
import rx.subscriptions.BooleanSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Use this storage facade to persist information about user-to-user relations to the database.
 * These relations currently are: followers and followings.
 *
 * @see com.soundcloud.android.model.UserAssociation.Type
 */
public class UserAssociationStorage extends ScheduledOperations {
    private final ContentResolver mResolver;
    private final UserAssociationDAO mUserAssociationDAO;
    private final UserAssociationDAO mFollowingsDAO;

    public UserAssociationStorage() {
        this(ScSchedulers.STORAGE_SCHEDULER, SoundCloudApplication.instance.getContentResolver());
    }

    public UserAssociationStorage(Scheduler scheduler, ContentResolver resolver) {
        super(scheduler);
        mResolver = resolver;
        mUserAssociationDAO = new UserAssociationDAO(mResolver);
        mFollowingsDAO = UserAssociationDAO.forContent(Content.ME_FOLLOWINGS, mResolver);
    }

    public Observable<UserAssociation> getFollowings() {
        return schedule(Observable.create(new Func1<Observer<UserAssociation>, Subscription>() {
            @Override
            public Subscription call(Observer<UserAssociation> userAssociationObserver) {
                RxUtils.emitCollection(userAssociationObserver,
                        mFollowingsDAO.buildQuery().where(DBHelper.UserAssociationView.USER_ASSOCIATION_REMOVED_AT + " IS NULL").queryAll()
                );
                userAssociationObserver.onCompleted();
                return Subscriptions.empty();
            }
        }));

    }

    /* Persists user-followings information to the database. Will create a user association,
     * update the followers count of the target user, and commit to the database.
     *
     * @param user the user that is being followed
     * @return the new association created
     */
    public Observable<UserAssociation> follow(final User user) {
        return schedule(Observable.create(new Func1<Observer<UserAssociation>, Subscription>() {
            @Override
            public Subscription call(Observer<UserAssociation> userAssociationObserver) {
                UserAssociation following = queryFollowingByTargetUserId(user.getId());
                if (following == null || following.getLocalSyncState() == UserAssociation.LocalState.PENDING_REMOVAL) {
                    following = new UserAssociation(UserAssociation.Type.FOLLOWING, user).markForAddition();
                    mFollowingsDAO.create(following);

                }
                userAssociationObserver.onNext(following);
                userAssociationObserver.onCompleted();
                return Subscriptions.empty();
            }
        }));

    }

    public Observable<UserAssociation> followSuggestedUser(final SuggestedUser suggestedUser) {
        return schedule(Observable.create(new Func1<Observer<UserAssociation>, Subscription>() {
            @Override
            public Subscription call(Observer<UserAssociation> userAssociationObserver) {
                UserAssociation following = queryFollowingByTargetUserId(suggestedUser.getId());
                if (following == null || following.getLocalSyncState() == UserAssociation.LocalState.PENDING_REMOVAL) {
                    following = new UserAssociation(UserAssociation.Type.FOLLOWING, new User(suggestedUser))
                            .markForAddition(suggestedUser.getToken());
                    mFollowingsDAO.create(following);
                }
                userAssociationObserver.onNext(following);
                userAssociationObserver.onCompleted();
                return Subscriptions.empty();
            }
        }));

    }

    /**
     * Add the users passed in as followings. This will not take into account the current status of the logged in
     * user's association, but will just create or update the current status
     *
     * @param users The users to be followed
     * @return the UserAssociations inserted
     */
    public Observable<UserAssociation> followList(final List<User> users) {
        return schedule(Observable.create(new Func1<Observer<UserAssociation>, Subscription>() {
            @Override
            public Subscription call(Observer<UserAssociation> userAssociationObserver) {
                List<UserAssociation> userAssociations = new ArrayList<UserAssociation>(users.size());
                for (User user : users) {
                    userAssociations.add(new UserAssociation(UserAssociation.Type.FOLLOWING, user).markForAddition());
                }
                mFollowingsDAO.createCollection(userAssociations);
                RxUtils.emitCollection(userAssociationObserver, userAssociations);
                userAssociationObserver.onCompleted();
                return Subscriptions.empty();
            }
        }));

    }

    /**
     * Add the Suggested Users passed in as followings. This will also pass the Suggested User token to the constructor
     * of the User Association. This will not take into account the current status of the logged in
     * user's association, but will just create or update the current status
     *
     * @param suggestedUsers
     * @return
     */
    public Observable<UserAssociation> followSuggestedUserList(final List<SuggestedUser> suggestedUsers) {
        return schedule(Observable.create(new Func1<Observer<UserAssociation>, Subscription>() {
            @Override
            public Subscription call(Observer<UserAssociation> userAssociationObserver) {
                List<UserAssociation> userAssociations = new ArrayList<UserAssociation>(suggestedUsers.size());
                for (SuggestedUser suggestedUser : suggestedUsers) {
                    userAssociations.add(new UserAssociation(
                            UserAssociation.Type.FOLLOWING, new User(suggestedUser)
                    ).markForAddition(suggestedUser.getToken()));
                }
                mFollowingsDAO.createCollection(userAssociations);
                RxUtils.emitCollection(userAssociationObserver, userAssociations);
                userAssociationObserver.onCompleted();
                return Subscriptions.empty();
            }
        }));

    }

    /**
     * Remove a following for the logged in user. This will create an association, remove
     * it from the database, and update the corresponding user with the new count in local storage
     *
     * @param user the user whose following should be removed
     * @return
     */
    public Observable<UserAssociation> unfollow(final User user) {
        return schedule(Observable.create(new Func1<Observer<UserAssociation>, Subscription>() {
            @Override
            public Subscription call(Observer<UserAssociation> userAssociationObserver) {
                final UserAssociation following = new UserAssociation(SoundAssociation.Type.FOLLOWING, user).markForRemoval();
                if (mUserAssociationDAO.update(following)) {
                    new UserDAO(mResolver).update(user);
                    userAssociationObserver.onNext(following);
                    userAssociationObserver.onCompleted();
                } else {
                    userAssociationObserver.onError(new Exception("Update failed"));
                }
                return Subscriptions.empty();
            }
        }));
    }

    /**
     * Create or update user associations of type FOLLOWING as marked for removal. This will ignore any current user
     * associations and do a bulk insert.
     *
     * @param users the users to mark for removal
     * @return the number of insertions/updates performed
     */
    public Observable<UserAssociation> unfollowList(final List<User> users) {
        return  schedule(Observable.create(new Func1<Observer<UserAssociation>, Subscription>() {
            @Override
            public Subscription call(Observer<UserAssociation> userAssociationObserver) {
                List<UserAssociation> userAssociations = new ArrayList<UserAssociation>(users.size());
                for (User user : users) {
                    userAssociations.add(new UserAssociation(UserAssociation.Type.FOLLOWING, user).markForRemoval());
                }
                mFollowingsDAO.createCollection(userAssociations);
                RxUtils.emitCollection(userAssociationObserver, userAssociations);
                userAssociationObserver.onCompleted();
                return Subscriptions.empty();
            }
        }));
    }

    @Deprecated
    public List<Long> getStoredIds(Uri uri) {
        final String selection = Content.ME_FOLLOWINGS.uri.equals(uri)
                ? DBHelper.UserAssociations.REMOVED_AT + " IS NULL AND " + DBHelper.UserAssociations.ADDED_AT + " IS NULL"
                : null;
        return ResolverHelper.idCursorToList(mResolver.query(ResolverHelper.addIdOnlyParameter(uri), null, selection, null, null));
    }

    @Deprecated
    public List<Long> deleteAssociations(Uri uri, List<Long> itemDeletions) {
        if (!itemDeletions.isEmpty()) {
            for (int i = 0; i < itemDeletions.size(); i += BaseDAO.RESOLVER_BATCH_SIZE) {
                List<Long> batch = itemDeletions.subList(i, Math.min(i + BaseDAO.RESOLVER_BATCH_SIZE, itemDeletions.size()));
                mResolver.delete(uri, getWhereInClause(DBHelper.UserAssociations.TARGET_ID, batch.size()), longListToStringArr(batch));
            }
        }
        return itemDeletions;
    }

    @Deprecated//This should operate on List<UserAssociation>, not ScResource
    public int insertAssociations(@NotNull List<? extends ScResource> resources, @NotNull Uri collectionUri, long userId) {
        BulkInsertMap map = new BulkInsertMap();
        for (int i = 0; i < resources.size(); i++) {
            ScResource r = resources.get(i);
            if (r == null) continue;

            r.putFullContentValues(map);
            ContentValues contentValues = new ContentValues();
            contentValues.put(DBHelper.UserAssociations.POSITION, i);
            contentValues.put(DBHelper.UserAssociations.TARGET_ID, r.getId());
            contentValues.put(DBHelper.UserAssociations.OWNER_ID, userId);
            map.add(collectionUri, contentValues);
        }
        return map.insert(mResolver);
    }

    @Deprecated//TODO: batching logic should be centralized somewhere, e.g. in BaseDAO
    public void insertInBatches(final Content content, final long ownerId, final List<Long> targetIds,
                                final int startPosition, int batchSize) {
        // insert in batches so as to not hold a write lock in a single transaction for too long
        int positionOffset = startPosition;
        for (int i = 0; i < targetIds.size(); i += batchSize) {

            List<Long> idBatch = targetIds.subList(i, Math.min(i + batchSize, targetIds.size()));
            ContentValues[] cv = new ContentValues[idBatch.size()];
            for (int j = 0; j < idBatch.size(); j++) {
                long id = idBatch.get(j);
                cv[j] = new ContentValues();
                cv[j].put(DBHelper.UserAssociations.POSITION, positionOffset + j);
                cv[j].put(DBHelper.UserAssociations.TARGET_ID, id);
                cv[j].put(DBHelper.UserAssociations.OWNER_ID, ownerId);
            }
            positionOffset += idBatch.size();
            mResolver.bulkInsert(content.uri, cv);
        }
    }

    public void clear() {
        UserAssociationDAO.forContent(Content.USER_ASSOCIATIONS, mResolver).deleteAll();
    }

    public List<UserAssociation> getFollowingsNeedingSync() {
        return mFollowingsDAO.buildQuery().where(DBHelper.UserAssociationView.USER_ASSOCIATION_ADDED_AT + " IS NOT NULL OR " +
                DBHelper.UserAssociationView.USER_ASSOCIATION_REMOVED_AT + " IS NOT NULL"
        ).queryAll();
    }

    public boolean hasFollowingsNeedingSync() {
        return mUserAssociationDAO.count(
                DBHelper.UserAssociations.ASSOCIATION_TYPE + " = ? AND (" +
                        DBHelper.UserAssociations.ADDED_AT + " IS NOT NULL OR " +
                        DBHelper.UserAssociations.REMOVED_AT + " IS NOT NULL )"
                , String.valueOf(Association.Type.FOLLOWING.collectionType)) > 0;
    }

    public boolean setFollowingAsSynced(UserAssociation a) {
        UserAssociation following = queryFollowingByTargetUserId(a.getUser().getId());
        if (following != null) {
            switch (following.getLocalSyncState()) {
                case PENDING_ADDITION:
                    following.clearLocalSyncState();
                    return mUserAssociationDAO.update(following);
                case PENDING_REMOVAL:
                    return mFollowingsDAO.delete(following);
            }
        }
        return false;
    }

    public boolean deleteFollowings(Collection<UserAssociation> followings){
        for (UserAssociation following : followings){
            if (!mFollowingsDAO.delete(following)) return false;
        }
        return true;
    }

    //TODO: this should be a bulk insert
    public Observable<Collection<UserAssociation>> setFollowingsAsSynced(final Collection<UserAssociation> userAssociations) {
        return Observable.create(new Func1<Observer<Collection<UserAssociation>>, Subscription>() {
            @Override
            public Subscription call(Observer<Collection<UserAssociation>> observer) {
                final BooleanSubscription subscription = new BooleanSubscription();
                for (UserAssociation ua : userAssociations) {
                    ua.clearLocalSyncState();
                }
                // TODO: this will trigger an upsert, but we should have an explicit updateAll method
                mFollowingsDAO.createCollection(userAssociations);
                observer.onNext(userAssociations);
                observer.onCompleted();
                return subscription;
            }
        });
    }

    @Nullable
    private UserAssociation queryFollowingByTargetUserId(long targetUserId) {
        String where = DBHelper.UserAssociationView._ID + " = ? AND " +
                DBHelper.UserAssociationView.USER_ASSOCIATION_TYPE + " = ?";

        return mFollowingsDAO.buildQuery()
                .where(where, String.valueOf(targetUserId), String.valueOf(Association.Type.FOLLOWING.collectionType))
                .first();
    }


}
