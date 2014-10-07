package com.soundcloud.android.storage;

import static com.soundcloud.android.storage.ResolverHelper.getWhereInClause;
import static com.soundcloud.android.storage.ResolverHelper.longListToStringArr;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.soundcloud.android.api.legacy.model.behavior.Identifiable;
import com.soundcloud.android.api.legacy.model.behavior.Persisted;
import com.soundcloud.android.storage.provider.BulkInsertMap;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.storage.provider.ScContentProvider;
import com.soundcloud.android.utils.UriUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@Deprecated
public abstract class BaseDAO<T extends Identifiable & Persisted> {
    public static final int RESOLVER_BATCH_SIZE = 500;

    protected final ContentResolver resolver;

    protected BaseDAO(ContentResolver contentResolver) {
        this.resolver = contentResolver;
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
        return map.insert(resolver);
    }

    @Deprecated
    public long create(ContentValues values) {
        return create(getContent().uri, values);
    }

    @Deprecated
    protected long create(Uri uri, ContentValues values) {
        Uri objUri = resolver.insert(uri, values);
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
        dependencies.insert(resolver);
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
            return resolver.delete(getContent().uri,
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
        return resolver.update(getContent().forId(id), values, null, null) == 1;
    }

    public boolean delete(T resource) {
        return delete(resource, null);
    }

    public boolean delete(T resource, @Nullable String where, String... whereArgs) {
        return delete(resource.toUri(), where, whereArgs);
    }

    public boolean deleteAll() {
        return delete(getContent().uri, null);
    }

    @Deprecated
    protected boolean delete(Uri uri, @Nullable String where, String... whereArgs) {
        return resolver.delete(uri, where, whereArgs) == 1;
    }

    @NotNull
    public QueryBuilder buildQuery() {
        return buildQuery(getContent().uri);
    }

    @NotNull
    public QueryBuilder buildQuery(Uri contentUri) {
        return new QueryBuilder(contentUri);
    }

    @NotNull
    public List<T> queryAll() {
        return queryAllByUri(getContent().uri);
    }

    @NotNull
    public List<Long> queryIdsByUri(Uri contentUri) {
        return new QueryBuilder(contentUri).queryIds();
    }

    @NotNull
    protected List<T> queryAllByUri(Uri contentUri) {
        return new QueryBuilder(contentUri).queryAll();
    }

    @NotNull
    private List<Long> queryIdsByUri(Uri contentUri, @Nullable String selection, @Nullable String[] selectionArgs) {
        Cursor cursor = resolver.query(contentUri, new String[]{BaseColumns._ID}, selection, selectionArgs, null);
        if (cursor == null) {
            return Collections.emptyList();
        }

        try {
            List<Long> ids = new ArrayList<Long>(cursor.getCount());
            while (cursor.moveToNext()) {
                ids.add(cursor.getLong(0));
            }
            return ids;
        } finally {
            cursor.close();
        }
    }

    @NotNull
    private List<T> queryAllByUri(Uri contentUri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String order) {
        Cursor cursor = resolver.query(contentUri, projection, selection, selectionArgs, order);
        if (cursor == null) {
            return Collections.emptyList();
        }

        try {
            List<T> objects = new ArrayList<T>(cursor.getCount());
            while (cursor.moveToNext()) {
                objects.add(objFromCursor(cursor));
            }
            return objects;
        } finally {
            cursor.close();
        }
    }

    @Nullable
    public T queryById(long id) {
        Cursor cursor = resolver.query(getContent().forId(id), null, null, null, null);
        if (cursor == null) {
            return null;
        }

        try {
            if (cursor.moveToFirst()) {
                return objFromCursor(cursor);
            }
            return null;
        } finally {
            cursor.close();
        }
    }

    @Nullable
    public T queryByUri(Uri uri) {
        return queryById(UriUtils.getLastSegmentAsLong(uri));
    }

    public int count() {
        return count(null);
    }

    public int count(@Nullable String where, String... whereArgs) {
        Cursor cursor = resolver.query(getContent().uri, null, where, whereArgs, null);
        if (cursor == null) {
            return 0;
        }

        try {
            return cursor.getCount();
        } finally {
            cursor.close();
        }
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
        return resolver;
    }

    @NotNull
    public Class<T> getModelClass() {
        @SuppressWarnings("unchecked")
        Class<T> klass = (Class<T>) getContent().modelType;
        if (klass == null) {
            throw new DAOException("No modelclass defined");
        }
        return klass;
    }

    @Deprecated
    public final class QueryBuilder {
        private static final int INITIAL_SELECTION_CAPACITY = 200;

        private final Uri contentUri;
        private @Nullable String[] projection;
        private @Nullable String order;
        private final StringBuilder selection;
        private final List<String> selectionArgs;
        private int limit;

        public QueryBuilder(Uri contentUri) {
            selection = new StringBuilder(INITIAL_SELECTION_CAPACITY);
            selectionArgs = new LinkedList<String>();
            this.contentUri = contentUri;
        }

        public QueryBuilder where(final String selection, final String... selectionArgs) {
            this.selection.append(selection);
            this.selectionArgs.addAll(Arrays.asList(selectionArgs));
            return this;
        }

        public QueryBuilder whereIn(final String column, final List<String> values) {
            selection.append(column).append(" IN (");
            List<String> wildcards = Collections.nCopies(values.size(), "?");
            Joiner.on(",").appendTo(selection, wildcards);
            selection.append(") ");
            selectionArgs.addAll(values);
            return this;
        }

        public QueryBuilder whereIn(String column, String... values) {
            return whereIn(column, Arrays.asList(values));
        }

        public QueryBuilder select(final String... projection) {
            this.projection = projection;
            return this;
        }

        public QueryBuilder order(final String order) {
            this.order = order;
            return this;
        }

        public QueryBuilder limit(final int limit) {
            this.limit = limit;
            return this;
        }

        public List<T> queryAll() {
            Uri contentUri = resolveContentUri();

            String selection = resolveSelection();
            String[] selectionArgs = resolveSelectionArgs();
            return queryAllByUri(contentUri, projection, selection, selectionArgs, order);
        }

        public List<Long> queryIds() {
            Uri contentUri = resolveContentUri();

            String selection = resolveSelection();
            String[] selectionArgs = resolveSelectionArgs();
            return queryIdsByUri(contentUri, selection, selectionArgs);
        }

        @Nullable
        public T first() {
            List<T> all = limit(1).queryAll();
            if (all.isEmpty()) {
                return null;
            } else {
                return all.get(0);
            }
        }

        private Uri resolveContentUri() {
            Uri contentUri = this.contentUri;
            if (limit > 0) {
                contentUri = this.contentUri.buildUpon().appendQueryParameter(ScContentProvider.Parameter.LIMIT, String.valueOf(limit)).build();
            }
            return contentUri;
        }

        @Nullable
        private String[] resolveSelectionArgs() {
            String[] selectionArgs = null;
            if (!this.selectionArgs.isEmpty()) {
                selectionArgs = Iterables.toArray(this.selectionArgs, String.class);
            }
            return selectionArgs;
        }

        @Nullable
        private String resolveSelection() {
            String selection = null;
            if (!TextUtils.isEmpty(this.selection)) {
                selection = this.selection.toString().trim();
            }
            return selection;
        }

    }
}
