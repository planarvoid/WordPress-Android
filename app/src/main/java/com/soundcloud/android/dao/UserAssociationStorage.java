package com.soundcloud.android.dao;

import static com.soundcloud.android.dao.ResolverHelper.getWhereInClause;
import static com.soundcloud.android.dao.ResolverHelper.longListToStringArr;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.provider.BulkInsertMap;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import org.jetbrains.annotations.NotNull;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

import java.util.List;

/**
 * Use this storage facade to persist information about user-to-user relations to the database.
 * These relations currently are: followers and followings.
 *
 * @see com.soundcloud.android.model.UserAssociation.Type
 */
public class UserAssociationStorage {
    private final ContentResolver mResolver;

    public UserAssociationStorage() {
        mResolver = SoundCloudApplication.instance.getContentResolver();
    }

    public UserAssociationStorage(ContentResolver resolver) {
        mResolver = resolver;
    }

    public List<Long> getStoredIds(Uri uri) {
        return ResolverHelper.idCursorToList(
                mResolver.query(ResolverHelper.addIdOnlyParameter(uri), null, null, null, null)
        );
    }

    public List<Long> deleteAssociations(Uri uri, List<Long> itemDeletions) {
        if (!itemDeletions.isEmpty()) {
            int i = 0;
            while (i < itemDeletions.size()) {
                List<Long> batch = itemDeletions.subList(i, Math.min(i + BaseDAO.RESOLVER_BATCH_SIZE, itemDeletions.size()));
                mResolver.delete(uri, getWhereInClause(DBHelper.UserAssociations.TARGET_ID, batch.size()), longListToStringArr(batch));
                i += BaseDAO.RESOLVER_BATCH_SIZE;
            }
        }
        return itemDeletions;
    }

    public int insertAssociations(@NotNull List<? extends ScResource> resources, @NotNull Uri collectionUri, long userId) {
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

    public void insertInBatches(final Content content, final long ownerId, final List<Long> targetIds,
                                final int startPosition, int batchSize) {
        int numBatches = 1;
        if (targetIds.size() > batchSize) {
            // split up the transaction into batches, so as to not block readers too long
            numBatches = (int) Math.ceil((float) targetIds.size() / batchSize);
        } else {
            batchSize = targetIds.size();
        }

        // insert in batches so as to not hold a write lock in a single transaction for too long
        int positionOffset = startPosition;
        for (int i = 0; i < numBatches; i++) {
            int batchStart = i * batchSize;
            int batchEnd = Math.min(batchStart + batchSize, targetIds.size());

            List<Long> idBatch = targetIds.subList(batchStart, batchEnd);
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
