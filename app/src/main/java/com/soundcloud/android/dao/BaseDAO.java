package com.soundcloud.android.dao;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import com.soundcloud.android.model.ModelLike;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public abstract class BaseDAO<T extends ModelLike> {
    protected final ContentResolver mResolver;

    protected BaseDAO(ContentResolver contentResolver) {
        this.mResolver = contentResolver;
    }

    public long create(T resource) {
        return create(resource.buildContentValues());
    }

    @Deprecated
    public long create(ContentValues values) {
        Uri uri = mResolver.insert(getContent().uri, values);
        if (uri != null) {
            try {
                return Long.parseLong(uri.getLastPathSegment());
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
                ResolverHelper.getWhereInClause(BaseColumns._ID, toRemove.size()),
                ResolverHelper.longListToStringArr(toRemove));
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

    public @Nullable T queryForId(long id) {
        Cursor cursor = mResolver.query(getContent().forId(id), null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            try {
                return getModelClass().getConstructor(Cursor.class).newInstance(cursor);
            } catch (Exception e) {
                throw new AssertionError("Could not find constructor for resource. id: " + id);
            }
        } else {
            return null;
        }
    }

    public abstract Content getContent();

    public @NotNull Class<T> getModelClass() {
        @SuppressWarnings("unchecked")
        Class<T> klass = (Class<T>) getContent().modelType;
        if (klass == null) throw new DAOException("No modelclass defined");
        return klass;
    }
}
