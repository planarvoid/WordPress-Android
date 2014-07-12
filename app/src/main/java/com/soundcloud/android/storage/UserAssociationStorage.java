package com.soundcloud.android.storage;


import static com.soundcloud.android.storage.ResolverHelper.getWhereInClause;
import static com.soundcloud.android.storage.ResolverHelper.longListToStringArr;

import com.soundcloud.android.api.legacy.model.Association;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.onboarding.suggestions.SuggestedUser;
import com.soundcloud.android.api.legacy.model.UserAssociation;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.storage.provider.BulkInsertMap;
import com.soundcloud.android.storage.provider.Content;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Use this storage facade to persist information about user-to-user relations to the database.
 * These relations currently are: followers and followings.
 *
 * @see com.soundcloud.android.api.legacy.model.UserAssociation.Type
 */
public class UserAssociationStorage extends ScheduledOperations {
    private final ContentResolver resolver;
    private final UserAssociationDAO userAssociationDAO;
    private final UserAssociationDAO followingsDAO;

    @Inject
    public UserAssociationStorage(Context context) {
        this(ScSchedulers.STORAGE_SCHEDULER, context.getContentResolver());
    }

    public UserAssociationStorage(Scheduler scheduler, ContentResolver resolver) {
        super(scheduler);
        this.resolver = resolver;
        userAssociationDAO = new UserAssociationDAO(this.resolver);
        followingsDAO = UserAssociationDAO.forContent(Content.ME_FOLLOWINGS, this.resolver);
    }

    public Observable<UserAssociation> getFollowings() {
        return schedule(Observable.create(new Observable.OnSubscribe<UserAssociation>() {
            @Override
            public void call(Subscriber<? super UserAssociation> userAssociationObserver) {
                RxUtils.emitIterable(userAssociationObserver,
                        followingsDAO.buildQuery().
                                where(TableColumns.UserAssociationView.USER_ASSOCIATION_REMOVED_AT + " IS NULL").queryAll()
                );
                userAssociationObserver.onCompleted();
            }
        }));

    }

    /* Persists user-followings information to the database. Will create a user association,
     * update the followers count of the target user, and commit to the database.
     *
     * @param user the user that is being followed
     * @return the new association created
     */
    public Observable<UserAssociation> follow(final PublicApiUser user) {
        return schedule(Observable.create(new Observable.OnSubscribe<UserAssociation>() {
            @Override
            public void call(Subscriber<? super UserAssociation> userAssociationObserver) {
                UserAssociation following = queryFollowingByTargetUserId(user.getId());
                if (following == null || following.getLocalSyncState() == UserAssociation.LocalState.PENDING_REMOVAL) {
                    following = new UserAssociation(UserAssociation.Type.FOLLOWING, user).markForAddition();
                    followingsDAO.create(following);

                }
                userAssociationObserver.onNext(following);
                userAssociationObserver.onCompleted();
            }
        }));

    }

    public Observable<UserAssociation> followSuggestedUser(final SuggestedUser suggestedUser) {
        return schedule(Observable.create(new Observable.OnSubscribe<UserAssociation>() {
            @Override
            public void call(Subscriber<? super UserAssociation> userAssociationObserver) {
                UserAssociation following = queryFollowingByTargetUserId(suggestedUser.getId());
                if (following == null || following.getLocalSyncState() == UserAssociation.LocalState.PENDING_REMOVAL) {
                    following = new UserAssociation(UserAssociation.Type.FOLLOWING, new PublicApiUser(suggestedUser))
                            .markForAddition(suggestedUser.getToken());
                    followingsDAO.create(following);
                }
                userAssociationObserver.onNext(following);
                userAssociationObserver.onCompleted();
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
    public Observable<UserAssociation> followList(final List<PublicApiUser> users) {
        return schedule(Observable.create(new Observable.OnSubscribe<UserAssociation>() {
            @Override
            public void call(Subscriber<? super UserAssociation> userAssociationObserver) {
                List<UserAssociation> userAssociations = new ArrayList<UserAssociation>(users.size());
                for (PublicApiUser user : users) {
                    userAssociations.add(new UserAssociation(UserAssociation.Type.FOLLOWING, user).markForAddition());
                }
                followingsDAO.createCollection(userAssociations);
                RxUtils.emitIterable(userAssociationObserver, userAssociations);
                userAssociationObserver.onCompleted();
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
        return schedule(Observable.create(new Observable.OnSubscribe<UserAssociation>() {
            @Override
            public void call(Subscriber<? super UserAssociation> userAssociationObserver) {
                List<UserAssociation> userAssociations = new ArrayList<UserAssociation>(suggestedUsers.size());
                for (SuggestedUser suggestedUser : suggestedUsers) {
                    userAssociations.add(new UserAssociation(
                            UserAssociation.Type.FOLLOWING, new PublicApiUser(suggestedUser)
                    ).markForAddition(suggestedUser.getToken()));
                }
                followingsDAO.createCollection(userAssociations);
                RxUtils.emitIterable(userAssociationObserver, userAssociations);
                userAssociationObserver.onCompleted();
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
    public Observable<UserAssociation> unfollow(final PublicApiUser user) {
        return schedule(Observable.create(new Observable.OnSubscribe<UserAssociation>() {
            @Override
            public void call(Subscriber<? super UserAssociation> userAssociationObserver) {
                final UserAssociation following = new UserAssociation(SoundAssociation.Type.FOLLOWING, user).markForRemoval();
                if (userAssociationDAO.update(following)) {
                    new UserDAO(resolver).update(user);
                    userAssociationObserver.onNext(following);
                    userAssociationObserver.onCompleted();
                } else {
                    userAssociationObserver.onError(new Exception("Update failed"));
                }
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
    public Observable<UserAssociation> unfollowList(final List<PublicApiUser> users) {
        return schedule(Observable.create(new Observable.OnSubscribe<UserAssociation>() {
            @Override
            public void call(Subscriber<? super UserAssociation> userAssociationObserver) {
                List<UserAssociation> userAssociations = new ArrayList<UserAssociation>(users.size());
                for (PublicApiUser user : users) {
                    userAssociations.add(new UserAssociation(UserAssociation.Type.FOLLOWING, user).markForRemoval());
                }
                followingsDAO.createCollection(userAssociations);
                RxUtils.emitIterable(userAssociationObserver, userAssociations);
                userAssociationObserver.onCompleted();
            }
        }));
    }

    @Deprecated
    public List<Long> getStoredIds(Uri uri) {
        final String selection = Content.ME_FOLLOWINGS.uri.equals(uri)
                ? TableColumns.UserAssociations.REMOVED_AT + " IS NULL AND " + TableColumns.UserAssociations.ADDED_AT + " IS NULL"
                : null;
        return ResolverHelper.idCursorToList(resolver.query(ResolverHelper.addIdOnlyParameter(uri), null, selection, null, null));
    }

    @Deprecated
    public List<Long> deleteAssociations(Uri uri, List<Long> itemDeletions) {
        if (!itemDeletions.isEmpty()) {
            for (int i = 0; i < itemDeletions.size(); i += BaseDAO.RESOLVER_BATCH_SIZE) {
                List<Long> batch = itemDeletions.subList(i, Math.min(i + BaseDAO.RESOLVER_BATCH_SIZE, itemDeletions.size()));
                resolver.delete(uri, getWhereInClause(TableColumns.UserAssociations.TARGET_ID, batch.size()), longListToStringArr(batch));
            }
        }
        return itemDeletions;
    }

    @Deprecated//This should operate on List<UserAssociation>, not ScResource
    public int insertAssociations(@NotNull List<? extends PublicApiResource> resources, @NotNull Uri collectionUri, long userId) {
        BulkInsertMap map = new BulkInsertMap();
        for (int i = 0; i < resources.size(); i++) {
            PublicApiResource r = resources.get(i);
            if (r == null) continue;

            r.putFullContentValues(map);
            ContentValues contentValues = new ContentValues();
            contentValues.put(TableColumns.UserAssociations.POSITION, i);
            contentValues.put(TableColumns.UserAssociations.TARGET_ID, r.getId());
            contentValues.put(TableColumns.UserAssociations.OWNER_ID, userId);
            map.add(collectionUri, contentValues);
        }
        return map.insert(resolver);
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
                cv[j].put(TableColumns.UserAssociations.POSITION, positionOffset + j);
                cv[j].put(TableColumns.UserAssociations.TARGET_ID, id);
                cv[j].put(TableColumns.UserAssociations.OWNER_ID, ownerId);
            }
            positionOffset += idBatch.size();
            resolver.bulkInsert(content.uri, cv);
        }
    }

    public void clear() {
        UserAssociationDAO.forContent(Content.USER_ASSOCIATIONS, resolver).deleteAll();
    }

    public List<UserAssociation> getFollowingsNeedingSync() {
        return followingsDAO.buildQuery().where(TableColumns.UserAssociationView.USER_ASSOCIATION_ADDED_AT + " IS NOT NULL OR " +
                TableColumns.UserAssociationView.USER_ASSOCIATION_REMOVED_AT + " IS NOT NULL"
        ).queryAll();
    }

    public boolean hasFollowingsNeedingSync() {
        return userAssociationDAO.count(
                TableColumns.UserAssociations.ASSOCIATION_TYPE + " = ? AND (" +
                        TableColumns.UserAssociations.ADDED_AT + " IS NOT NULL OR " +
                        TableColumns.UserAssociations.REMOVED_AT + " IS NOT NULL )"
                , String.valueOf(Association.Type.FOLLOWING.collectionType)) > 0;
    }

    public boolean setFollowingAsSynced(UserAssociation a) {
        UserAssociation following = queryFollowingByTargetUserId(a.getUser().getId());
        if (following != null) {
            switch (following.getLocalSyncState()) {
                case PENDING_ADDITION:
                    following.clearLocalSyncState();
                    return userAssociationDAO.update(following);
                case PENDING_REMOVAL:
                    return followingsDAO.delete(following);
            }
        }
        return false;
    }

    public boolean deleteFollowings(Collection<UserAssociation> followings){
        for (UserAssociation following : followings){
            if (!followingsDAO.delete(following)) return false;
        }
        return true;
    }

    //TODO: this should be a bulk insert
    public Observable<Collection<UserAssociation>> setFollowingsAsSynced(final Collection<UserAssociation> userAssociations) {
        return Observable.create(new Observable.OnSubscribe<Collection<UserAssociation>>() {
            @Override
            public void call(Subscriber<? super Collection<UserAssociation>> observer) {
                for (UserAssociation ua : userAssociations) {
                    ua.clearLocalSyncState();
                }
                // TODO: this will trigger an upsert, but we should have an explicit updateAll method
                followingsDAO.createCollection(userAssociations);
                observer.onNext(userAssociations);
                observer.onCompleted();
            }
        });
    }

    @Nullable
    private UserAssociation queryFollowingByTargetUserId(long targetUserId) {
        String where = TableColumns.UserAssociationView._ID + " = ? AND " +
                TableColumns.UserAssociationView.USER_ASSOCIATION_TYPE + " = ?";

        return followingsDAO.buildQuery()
                .where(where, String.valueOf(targetUserId), String.valueOf(Association.Type.FOLLOWING.collectionType))
                .first();
    }


}
