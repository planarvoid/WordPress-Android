package com.soundcloud.android.model;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.fasterxml.jackson.annotation.JsonView;
import com.soundcloud.android.json.Views;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;

public class ScModel implements Parcelable {

    public static final int NOT_SET = -1;
    @JsonView(Views.Mini.class) public long id = NOT_SET;

    @Deprecated
    protected static void setBundleFromField(Bundle bundle, String fieldName, Class fieldType, Object fieldValue) {
        if (fieldType == String.class && !TextUtils.isEmpty(String.valueOf(fieldValue)))
            bundle.putString(fieldName, (String) fieldValue);
        else if (fieldType.equals(Integer.TYPE) || fieldType.equals(Integer.class))
            bundle.putInt(fieldName, (Integer) fieldValue);
        else if (fieldType.equals(Long.TYPE) || fieldType.equals(Long.class))
            bundle.putLong(fieldName, (Long) fieldValue);
        else if (fieldType.equals(Double.TYPE) || fieldType.equals(Double.class))
            bundle.putDouble(fieldName, (Double) fieldValue);
        else if (fieldType.equals(boolean.class))
            bundle.putBoolean(fieldName, (Boolean) fieldValue);
        else if (fieldType.equals(Date.class))
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
                if (field.getType().equals(String.class)) {
                    field.set(p, bundle.getString(key));
                } else if (field.getType().equals(Long.TYPE) || field.getType().equals(Long.class)) {
                    field.set(p, bundle.getLong(key));
                } else if (field.getType().equals(Integer.TYPE) || field.getType().equals(Integer.class)) {
                    field.set(p, bundle.getInt(key));
                } else if (field.getType().equals(Double.TYPE) || field.getType().equals(Double.class)) {
                    field.set(p, bundle.getDouble(key));
                } else if (field.getType().equals(Boolean.TYPE)) {
                    field.set(p, bundle.getBoolean(key));
                } else if (field.getType().equals(Date.class)) {
                    field.set(p, new Date(bundle.getLong(key)));
                } else if (field.getType().equals(File.class)) {
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
                if (field.getType().equals(String.class)) {
                    field.set(p, cursor.getString(cursor.getColumnIndex(key)));
                } else if (field.getType().equals(Long.TYPE) || field.getType().equals(Long.class)) {
                    field.set(p, cursor.getLong(cursor.getColumnIndex(key)));
                } else if (field.getType().equals(Integer.TYPE) || field.getType().equals(Integer.class)) {
                    field.set(p, cursor.getInt(cursor.getColumnIndex(key)));
                } else if (field.getType().equals(Boolean.TYPE)) {
                    field.set(p, cursor.getInt(cursor.getColumnIndex(key)) == 1);
                } else if (field.getType().equals(Date.class)) {
                    field.set(p, new Date(cursor.getLong(cursor.getColumnIndex(key))));
                }
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "error ", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "error ", e);
        }
    }

    @Override @Deprecated // XXX AVOID THIS: slow (reflection)
    public void writeToParcel(Parcel out, int flags) {
        Bundle data = new Bundle();
        for (Field f : getClass().getDeclaredFields()) {
            try {
                if (!Modifier.isStatic(f.getModifiers()) && f.get(this) != null) {
                    setBundleFromField(data, f.getName(), f.getType(), f.get(this));
                }
            } catch (IllegalAccessException e) {
                //Log.e(TAG, "error ", e);
            }
        }
        data.putLong("id", id);
        out.writeBundle(data);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public ContentValues buildContentValues() {
        ContentValues cv = new ContentValues();
        if (id != ScResource.NOT_SET) cv.put(BaseColumns._ID, id);
        return cv;
    }

    public void resolve(Context context) {

    }
}
