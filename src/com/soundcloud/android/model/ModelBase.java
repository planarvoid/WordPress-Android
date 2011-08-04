package com.soundcloud.android.model;

import static com.soundcloud.android.SoundCloudApplication.TAG;

import com.soundcloud.android.SoundCloudApplication;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;

public abstract class ModelBase implements Parcelable {
    public long id = -1;

    public ModelBase() {
    }

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
                Log.e(TAG, "error ", e);
            }
        }
        this.id = data.getLong("id");
    }

    @Override
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


    protected static void setBundleFromField(Bundle bundle, String fieldName, Class fieldType, Object fieldValue) {
        if (fieldType == String.class && !TextUtils.isEmpty(String.valueOf(fieldValue)))
            bundle.putString(fieldName, (String) fieldValue);
        else if (fieldType == Integer.TYPE || fieldType == Integer.class)
            bundle.putInt(fieldName, (Integer) fieldValue);
        else if (fieldType == Long.TYPE || fieldType == Long.class)
            bundle.putLong(fieldName, (Long) fieldValue);
        else if (fieldType == boolean.class)
            bundle.putBoolean(fieldName, (Boolean) fieldValue);
        else if (fieldType == Date.class)
            bundle.putLong(fieldName, ((Date) fieldValue).getTime());
        else if (Parcelable.class.isAssignableFrom(fieldType)) {
            bundle.putParcelable(fieldName, (Parcelable) fieldValue);
        } else {
            Log.i(TAG, "Ignoring " + fieldName + " of type " + fieldType);
        }
    }

    protected static void setFieldFromBundle(Parcelable p, Field field, Bundle bundle, String key) {
        try {
            if (field != null) {
                if (field.getType() == String.class) {
                    field.set(p, bundle.getString(key));
                } else if (field.getType() == Long.TYPE || field.getType() == Long.class) {
                    field.set(p, bundle.getLong(key));
                } else if (field.getType() == Integer.TYPE || field.getType() == Integer.class) {
                    field.set(p, bundle.getInt(key));
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
                    field.set(p, cursor.getInt(cursor.getColumnIndex(key)) != 0);
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
        if (!(o instanceof ModelBase)) return false;

        ModelBase modelBase = (ModelBase) o;
        return id == modelBase.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
