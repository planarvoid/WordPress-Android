package com.soundcloud.android.dao;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import com.soundcloud.android.model.ModelLike;
import com.soundcloud.android.provider.BulkInsertMap;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.utils.UriUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.soundcloud.android.dao.ResolverHelper.getWhereInClause;
import static com.soundcloud.android.dao.ResolverHelper.idCursorToList;
import static com.soundcloud.android.dao.ResolverHelper.longListToStringArr;

public abstract class BaseDAO<T extends ModelLike & ContentValuesProvider> {
    protected final ContentResolver mResolver;

    protected BaseDAO(ContentResolver contentResolver) {
        this.mResolver = contentResolver;
    }

    public long create(T resource) {
        final BulkInsertMap dependencies = new BulkInsertMap();
        resource.putFullContentValues(dependencies);
        dependencies.insert(mResolver);
        // TODO this will insert twice
        return create(resource.toUri(), resource.buildContentValues());
    }

    public int createCollection(Collection<T> resources) {
        BulkInsertMap map = new BulkInsertMap();
        for (T r : resources) {
            r.putFullContentValues(map);
        }
        return map.insert(mResolver);
    }

    public long create(ContentValues values) {
        return create(getContent().uri, values);
    }

    @Deprecated
    public long create(Uri uri, ContentValues values) {
        Uri objUri = mResolver.insert(uri,  values);
        if (objUri != null) {
            try {
                return Long.parseLong(objUri.getLastPathSegment());
            } catch (NumberFormatException e) {
                throw new DAOException(e);
            }
        } else {
            throw new DAOException();
        }
    }

    public long createOrUpdate(T resource) {
        return createOrUpdate(resource.getId(), resource.buildContentValues());
    }

    public int deleteAll(Collection<T> resources) {
        Set<Long> toRemove = new HashSet<Long>(resources.size());
        for (T res : resources) {
            toRemove.add(res.getId());
        }
        if (!toRemove.isEmpty()) {
            return mResolver.delete(getContent().uri,
                getWhereInClause(BaseColumns._ID, toRemove.size()),
                longListToStringArr(toRemove));
        } else {
            return 0;
        }
    }

    public long createOrUpdate(long id, ContentValues values) {
        T obj = queryForId(id);
        if (obj == null) {
            return create(values);
        } else {
            update(id, values);
            return id;
        }
    }

    public boolean update(T resource) {
        return update(resource.getId(), resource.buildContentValues());
    }

    public boolean update(long id, ContentValues values) {
        return mResolver.update(getContent().forId(id), values, null, null) == 1;
    }

    public boolean delete(T resource) {
        return mResolver.delete(resource.toUri(), null, null) == 1;
    }

    public List<T> queryAll() {
        Cursor c = mResolver.query(getContent().uri, null, null, null, null);
        if (c != null) {
            List<T> objects = new ArrayList<T>(c.getCount());
            while (c.moveToNext()) {
                objects.add(objFromCursor(c));
            }
            c.close();
            return objects;
        } else {
            return Collections.emptyList();
        }
    }

    public @Nullable T queryForId(long id) {
        Cursor cursor = mResolver.query(getContent().forId(id), null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            T obj = objFromCursor(cursor);
            cursor.close();
            return obj;
        } else {
            return null;
        }
    }

    public @Nullable T queryForUri(Uri uri) {
        return queryForId(UriUtils.getLastSegmentAsLong(uri));
    }

    protected T objFromCursor(Cursor cursor) {
        try {
            return getModelClass().getConstructor(Cursor.class).newInstance(cursor);
        } catch (Exception e) {
            throw new AssertionError("Could not find constructor for resource.");
        }
    }

    public abstract Content getContent();

    // I'd like to keep this for now in order to verify DB interaction in tests;
    // Once we've removed ContentProvider, those interaction tests should be replaced
    // with mocked calls to the actual DB
    /* package */ ContentResolver getContentResolver() {
        return mResolver;
    }

    public @NotNull Class<T> getModelClass() {
        @SuppressWarnings("unchecked")
        Class<T> klass = (Class<T>) getContent().modelType;
        if (klass == null) throw new DAOException("No modelclass defined");
        return klass;
    }


    /**
     * @return a list of all ids for which objects are stored in the db.
     */
    public List<Long> getStoredIds(List<Long> ids) {
        return idCursorToList(
                mResolver.query(
                    getContent().uri,
                    new String[]{BaseColumns._ID},
                    getWhereInClause(BaseColumns._ID, ids.size()) + " AND " + DBHelper.ResourceTable.LAST_UPDATED + " > 0",
                    longListToStringArr(ids),
                    null
                )
        );
    }
}
