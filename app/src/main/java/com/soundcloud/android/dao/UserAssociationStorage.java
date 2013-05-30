package com.soundcloud.android.dao;

import static com.soundcloud.android.dao.ResolverHelper.getWhereInClause;
import static com.soundcloud.android.dao.ResolverHelper.longListToStringArr;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Association;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserAssociation;
import com.soundcloud.android.provider.BulkInsertMap;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import org.jetbrains.annotations.NotNull;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import java.util.Date;
import java.util.List;

/**
 * Use this storage facade to persist information about user-to-user relations to the database.
 * These relations currently are: followers and followings.
 *
 * @see com.soundcloud.android.model.UserAssociation.Type
 */
public class UserAssociationStorage {
    private final ContentResolver mResolver;
    private final UserAssociationDAO mUserAssociationDAO;
    private final UserAssociationDAO mFollowingsDAO;

    public UserAssociationStorage() {
        this(SoundCloudApplication.instance.getContentResolver());
    }

    public UserAssociationStorage(ContentResolver resolver) {
        mResolver = resolver;
        mUserAssociationDAO = new UserAssociationDAO(mResolver);
        mFollowingsDAO = UserAssociationDAO.forContent(Content.ME_FOLLOWINGS, mResolver);
    }

    @Deprecated
    public List<Long> getStoredIds(Uri uri) {
        // TODO, this should return only things with no ADDED stamp
        return ResolverHelper.idCursorToList(
                mResolver.query(ResolverHelper.addIdOnlyParameter(uri), null, null, null, null)
        );
    }

    public List<UserAssociation> getFollowings() {
        return mFollowingsDAO.buildQuery().where(DBHelper.UserAssociationView.USER_ASSOCIATION_REMOVED_AT + " IS NULL").queryAll();
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

    /**
     * Persists user-followings information to the database. Will create a user association,
     * update the followers count of the target user, and commit to the database.
     *
     * @param user the user that is being followed
     * @return the new association created
     */
    public UserAssociation addFollowing(User user) {
        UserAssociation following = mFollowingsDAO.query(user.id);
        if (following == null || following.isMarkedForRemoval()){
            following = new UserAssociation(UserAssociation.Type.FOLLOWING, user);
            following.markForAddition();
            user.addAFollower();
            mFollowingsDAO.create(following);

        }
        return following;
    }

    /**
     * Remove a following for the logged in user. This will create an association, remove
     * it from the database, and update the corresponding user with the new count in local storage
     *
     * @param user the user whose following should be removed
     * @return
     */
    public UserAssociation removeFollowing(User user) {
        final UserAssociation following = new UserAssociation(SoundAssociation.Type.FOLLOWING, user);
        following.markForRemoval();
        if (mUserAssociationDAO.update(following) && user.removeAFollower()) {
            new UserDAO(mResolver).update(user);
            return following;
        }
        return null;
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

    @Deprecated
    public int insertAssociations(@NotNull List<? extends ScResource> resources, @NotNull Uri collectionUri,
                                  long userId) {
        BulkInsertMap map = new BulkInsertMap();
        for (int i = 0; i < resources.size(); i++) {
            ScResource r = resources.get(i);
            if (r == null) continue;

            r.putFullContentValues(map);
            ContentValues contentValues = new ContentValues();
            contentValues.put(DBHelper.UserAssociations.POSITION, i);
            contentValues.put(DBHelper.UserAssociations.TARGET_ID, r.id);
            contentValues.put(DBHelper.UserAssociations.OWNER_ID, userId);
            map.add(collectionUri, contentValues);
        }
        return map.insert(mResolver);
    }

    @Deprecated
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
}
