package com.soundcloud.android.model;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.json.Views;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.List;

import static com.soundcloud.android.SoundCloudApplication.TAG;
import static com.soundcloud.android.SoundCloudApplication.TRACK_CACHE;
import static com.soundcloud.android.SoundCloudApplication.USER_CACHE;

public abstract class ScModel implements Parcelable {
    public static final int NOT_SET = -1;

    @JsonView(Views.Mini.class) public long id = NOT_SET;
    @JsonIgnore public long last_updated       = NOT_SET;

    public ScModel() {
    }

    public static CollectionHolder getCollectionFromStream(InputStream is,
                                                           ObjectMapper mapper,
                                                           Class<?> loadModel,
                                                           List<? super Parcelable> items) throws IOException {
        CollectionHolder holder = null;
        if (Track.class.equals(loadModel)) {
            holder = mapper.readValue(is, TracklistItemHolder.class);
            for (TracklistItem t : (TracklistItemHolder) holder) {
                items.add(TRACK_CACHE.fromListItem(t));
            }
        } else if (User.class.equals(loadModel)) {
            holder = mapper.readValue(is, UserlistItemHolder.class);
            for (UserlistItem u : (UserlistItemHolder) holder) {
                items.add(USER_CACHE.fromListItem(u));
            }
        } else if (Activity.class.equals(loadModel)) {
            holder = mapper.readValue(is, Activities.class);
            for (Activity e : (Activities) holder) {
                items.add(e);
            }
        } else if (Friend.class.equals(loadModel)) {
            holder = mapper.readValue(is, FriendHolder.class);
            for (Friend f : (FriendHolder) holder) {
                items.add(f);
            }
        } else if (Comment.class.equals(loadModel)) {
            holder = mapper.readValue(is, CommentHolder.class);
            for (Comment f : (CommentHolder) holder) {
                items.add(f);
            }
        }
        return holder;
    }

    @Deprecated // XXX this is slow (reflection)
    protected void readFromParcel(Parcel in) {
        Bundle data = in.readBundle(getClass().getClassLoader());
        for (String key : data.keySet()) {
            try {
                setFieldFromBundle(this, getClass().getDeclaredField(key), data, key);
            } catch (SecurityException e) {
                Log.e(TAG, "error ", e);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "error ", e);
            } catch (NoSuchFieldException e) {
                try {
                    setFieldFromBundle(this, getClass().getField(key), data, key);
                } catch (NoSuchFieldException ignored) {
                    Log.e(TAG, "error ", ignored);
                }
            }
        }
        this.id = data.getLong("id");
    }

    @Override // XXX AVOID THIS: slow (reflection)
    public void writeToParcel(Parcel out, int flags) {
        Bundle data = new Bundle();
        for (Field f : getClass().getDeclaredFields()) {
            try {
                if (!Modifier.isStatic(f.getModifiers()) && f.get(this) != null) {
                    setBundleFromField(data, f.getName(), f.getType(), f.get(this));
                }
            } catch (IllegalAccessException e) {
                Log.e(TAG, "error ", e);
            }
        }
        data.putLong("id", id);
        out.writeBundle(data);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Deprecated
    protected static void setBundleFromField(Bundle bundle, String fieldName, Class fieldType, Object fieldValue) {
        if (fieldType == String.class && !TextUtils.isEmpty(String.valueOf(fieldValue)))
            bundle.putString(fieldName, (String) fieldValue);
        else if (fieldType == Integer.TYPE || fieldType == Integer.class)
            bundle.putInt(fieldName, (Integer) fieldValue);
        else if (fieldType == Long.TYPE || fieldType == Long.class)
            bundle.putLong(fieldName, (Long) fieldValue);
        else if (fieldType == Double.TYPE || fieldType == Double.class)
            bundle.putDouble(fieldName, (Double) fieldValue);
        else if (fieldType == boolean.class)
            bundle.putBoolean(fieldName, (Boolean) fieldValue);
        else if (fieldType == Date.class)
            bundle.putLong(fieldName, ((Date) fieldValue).getTime());
        else if (Parcelable.class.isAssignableFrom(fieldType)) {
            bundle.putParcelable(fieldName, (Parcelable) fieldValue);
        } else if (File.class.isAssignableFrom(fieldType)) {
            bundle.putString(fieldName, ((File)fieldValue).getAbsolutePath());
        } else {
            Log.i(TAG, "Ignoring " + fieldName + " of type " + fieldType);
        }
    }

    @Deprecated
    protected static void setFieldFromBundle(Parcelable p, Field field, Bundle bundle, String key) {
        try {
            if (field != null) {
                if (field.getType() == String.class) {
                    field.set(p, bundle.getString(key));
                } else if (field.getType() == Long.TYPE || field.getType() == Long.class) {
                    field.set(p, bundle.getLong(key));
                } else if (field.getType() == Integer.TYPE || field.getType() == Integer.class) {
                    field.set(p, bundle.getInt(key));
                } else if (field.getType() == Double.TYPE || field.getType() == Double.class) {
                    field.set(p, bundle.getDouble(key));
                } else if (field.getType() == Boolean.TYPE) {
                    field.set(p, bundle.getBoolean(key));
                } else if (field.getType() == Date.class) {
                    field.set(p, new Date(bundle.getLong(key)));
                } else if (field.getType() == File.class) {
                    field.set(p, new File(bundle.getString(key)));
                } else if (Parcelable.class.isAssignableFrom(field.getType())) {
                    field.set(p, bundle.<Parcelable>getParcelable(key));
                } else {
                    Log.i(p.getClass().getSimpleName(), "Ignoring " + field.getName() + " of type " + field.getType());
                }
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "error ", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "error ", e);
        }
    }

    @Deprecated
    protected static void setFieldFromCursor(Parcelable p, Field field, Cursor cursor, String key) {
        try {
            if (field != null) {
                if (field.getType() == String.class) {
                    field.set(p, cursor.getString(cursor.getColumnIndex(key)));
                } else if (field.getType() == Long.TYPE || field.getType() == Long.class) {
                    field.set(p, cursor.getLong(cursor.getColumnIndex(key)));
                } else if (field.getType() == Integer.TYPE || field.getType() == Integer.class) {
                    field.set(p, cursor.getInt(cursor.getColumnIndex(key)));
                } else if (field.getType() == Boolean.TYPE) {
                    field.set(p, cursor.getInt(cursor.getColumnIndex(key)) == 1);
                } else if (field.getType() == Date.class) {
                    field.set(p, new Date(cursor.getLong(cursor.getColumnIndex(key))));
                }
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "error ", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "error ", e);
        }
    }

    public void resolve(SoundCloudApplication application) {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScModel)) return false;

        ScModel modelBase = (ScModel) o;
        return id == modelBase.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    public ContentValues buildContentValues() {
        ContentValues cv = new ContentValues();
        if (id != -1) cv.put(BaseColumns._ID, id);
        return cv;
    }

    /**
     * @return whether this object has been saved to the database.
     */
    public boolean isSaved() {
        return id >= 0;
    }

    public static class TracklistItemHolder extends CollectionHolder<TracklistItem> {}
    public static class UserlistItemHolder extends CollectionHolder<UserlistItem> {}
    public static class FriendHolder extends CollectionHolder<Friend> {}
    public static class CommentHolder extends CollectionHolder<Comment> {}
}
