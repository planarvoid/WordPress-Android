package com.soundcloud.android.storage;


import com.soundcloud.android.api.legacy.model.Association;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.UserAssociation;
import com.soundcloud.android.storage.provider.BulkInsertMap;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.storage.provider.ScContentProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Use this storage facade to persist information about user-to-user relations to the database.
 * These relations currently are: followers and followings.
 *
 * @see com.soundcloud.android.api.legacy.model.UserAssociation.Type
 */
public class LegacyUserAssociationStorage {
    private final ContentResolver resolver;
    private final UserAssociationDAO userAssociationDAO;
    private final UserAssociationDAO followingsDAO;

    private static String[] longListToStringArr(Collection<Long> deletions) {
        int i = 0;
        String[] idList = new String[deletions.size()];
        for (Long id : deletions) {
            idList[i] = String.valueOf(id);
            i++;
        }
        return idList;
    }

    private static String getWhereInClause(String column, int size) {
        StringBuilder sb = new StringBuilder(column).append(" IN (?");
        for (int i = 1; i < size; i++) {
            sb.append(",?");
        }
        sb.append(')');
        return sb.toString();
    }

    private static List<Long> idCursorToList(Cursor c) {
        if (c == null) {
            return Collections.emptyList();
        }
        List<Long> ids = new ArrayList<>(c.getCount());
        while (c.moveToNext()) {
            ids.add(c.getLong(0));
        }
        c.close();
        return ids;
    }

    @Inject
    public LegacyUserAssociationStorage(Context context) {
        this(context.getContentResolver());
    }

    public LegacyUserAssociationStorage(ContentResolver resolver) {
        this.resolver = resolver;
        userAssociationDAO = new UserAssociationDAO(this.resolver);
        followingsDAO = UserAssociationDAO.forContent(Content.ME_FOLLOWINGS, this.resolver);
    }

    @Deprecated
    public List<Long> getStoredIds(Uri uri) {
        final String selection = Content.ME_FOLLOWINGS.uri.equals(uri)
                ? TableColumns.UserAssociations.REMOVED_AT + " IS NULL AND " + TableColumns.UserAssociations.ADDED_AT + " IS NULL"
                : null;
        return idCursorToList(resolver.query(uri.buildUpon()
                .appendQueryParameter(ScContentProvider.Parameter.IDS_ONLY, "1").build(), null, selection, null, null));
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
    public int insertAssociations(@NotNull List<? extends PublicApiResource> resources, @NotNull Uri collectionUri) {
        BulkInsertMap map = new BulkInsertMap();
        for (int i = 0; i < resources.size(); i++) {
            PublicApiResource r = resources.get(i);
            if (r == null) {
                continue;
            }

            r.putFullContentValues(map);
            ContentValues contentValues = new ContentValues();
            contentValues.put(TableColumns.UserAssociations.POSITION, i);
            contentValues.put(TableColumns.UserAssociations.TARGET_ID, r.getId());
            map.add(collectionUri, contentValues);
        }
        return map.insert(resolver);
    }

    @Deprecated//TODO: batching logic should be centralized somewhere, e.g. in BaseDAO
    public void insertInBatches(final List<Long> targetIds,
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
            }
            positionOffset += idBatch.size();
            resolver.bulkInsert(Content.ME_FOLLOWINGS.uri, cv);
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
                    following.clearLocalSyncState();
                    return followingsDAO.delete(following);
                default:
                    return false;
            }
        }
        return false;
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
