package com.soundcloud.android.dao;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import com.soundcloud.android.model.ModelLike;
import com.soundcloud.android.provider.Content;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
