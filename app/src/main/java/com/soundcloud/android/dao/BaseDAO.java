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
        return create(resource, true);
    }

    public long create(T resource, boolean storeDependencies) {
        if (storeDependencies) {
            createDependencies(resource);
        }
        long recordId = create(resource.buildContentValues());
        resource.setId(recordId);
        return recordId;
    }

    public int createCollection(Collection<T> resources) {
        BulkInsertMap map = new BulkInsertMap();
        for (T r : resources) {
            r.putFullContentValues(map);
        }
        return map.insert(mResolver);
    }

    @Deprecated
    public long create(ContentValues values) {
        return create(getContent().uri, values);
    }

    @Deprecated
    protected long create(Uri uri, ContentValues values) {
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

    protected void createDependencies(T resource) {
        final BulkInsertMap dependencies = new BulkInsertMap();
        resource.putDependencyValues(dependencies);
        dependencies.insert(mResolver);
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
        T obj = queryById(id);
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
        return delete(resource, null);
    }

    public boolean delete(T resource, @Nullable String where, String... whereArgs) {
        return delete(resource.toUri(), where, whereArgs);
    }

    @Deprecated
    public boolean delete(Uri uri, @Nullable String where, String... whereArgs) {
        return mResolver.delete(uri, where, whereArgs) == 1;
    }


    public QueryBuilder buildQuery() {
        return buildQuery(getContent().uri);
    }

    public QueryBuilder buildQuery(Uri contentUri) {
        return new QueryBuilder(contentUri);
    }

    public List<T> queryAll() {
        return queryAllByUri(getContent().uri);
    }

    protected List<T> queryAllByUri(Uri contentUri) {
        return new QueryBuilder(contentUri).queryAll();
    }

    private List<T> queryAllByUri(Uri contentUri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String order) {
        Cursor c = mResolver.query(contentUri, projection, selection, selectionArgs, order);
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

    public @Nullable T queryById(long id) {
        Cursor cursor = mResolver.query(getContent().forId(id), null, null, null, null);
        try {
            if (cursor != null && cursor.moveToFirst()) {
                return objFromCursor(cursor);
            } else {
                return null;
            }
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public @Nullable T queryByUri(Uri uri) {
        return queryById(UriUtils.getLastSegmentAsLong(uri));
    }

    public int count() {
        return count(null);
    }

    public int count(@Nullable String where, String... whereArgs) {
        Cursor cursor = mResolver.query(getContent().uri, null, where, whereArgs, null);
        int count = 0;
        if (cursor != null) {
            count = cursor.getCount();
            cursor.close();
        }
        return count;
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

    public final class QueryBuilder {
        private final Uri mContentUri;
        private String[] mProjection, mSelectionArgs;
        private String mSelection, mOrder;
        private int mLimit;

        public QueryBuilder(Uri contentUri) {
            mContentUri = contentUri;
        }

        public QueryBuilder where(@Nullable final String selection, @Nullable final String... selectionArgs) {
            mSelection = selection;
            mSelectionArgs = selectionArgs;
            return this;
        }

        public QueryBuilder select(@Nullable final String... projection) {
            mProjection = projection;
            return this;
        }

        public QueryBuilder order(@Nullable final String order) {
            mOrder = order;
            return this;
        }

        public QueryBuilder limit(final int limit) {
            mLimit = limit;
            return this;
        }

        public List<T> queryAll() {
            String orderAndLimitClause = null;
            if (mOrder != null || mLimit > 0) {
                StringBuilder sb = new StringBuilder();
                if (mOrder != null) sb.append(mOrder);
                if (mLimit > 0) sb.append(" LIMIT " + mLimit);
                orderAndLimitClause = sb.toString().trim();
            }

            return queryAllByUri(mContentUri, mProjection, mSelection, mSelectionArgs, orderAndLimitClause);
        }

        public @Nullable T first() {
            List<T> all = limit(1).queryAll();
            if (all.isEmpty()) {
                return null;
            } else {
                return all.get(0);
            }
        }
    }
}
