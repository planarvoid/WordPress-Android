
package com.soundcloud.android.objects;

import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.Date;

public class BaseObj implements Parcelable {

    public enum Parcelables {
        track, user, comment
    }

    public BaseObj() {

    }

    public BaseObj(Parcel in) {
        readFromParcel(in);
    }

    public static final Parcelable.Creator<BaseObj> CREATOR = new Parcelable.Creator<BaseObj>() {
        public BaseObj createFromParcel(Parcel in) {
            return new BaseObj(in);
        }

        public BaseObj[] newArray(int size) {
            return new BaseObj[size];
        }
    };

    public void readFromParcel(Parcel in) {
        Field f;
        Bundle data = in.readBundle(this.getClass().getClassLoader());
        for (String key : data.keySet()) {
            try {
                f = this.getClass().getField(key);
                if (f != null) {
                    f.set(this, data.get(key));
                }
            } catch (SecurityException e1) {
                e1.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void buildParcel(Parcel out, int flags) {
        // XXX revise+test
        // data = in.readBundle();
        Bundle data = new Bundle();
        try {
            for (Field f : this.getClass().getDeclaredFields()) {
                    if (f.getType() == String.class)
                            data.putString(f.getName(),
                                    (String) f.get(this));
                    else if (f.getType() == Integer.TYPE || f.getType() == Integer.class)
                        data.putInt(f.getName(),
                                (Integer) f.get(this));
                    else if (f.getType() == Long.TYPE || f.getType() == Long.class)
                        data.putLong(f.getName(),
                                (Long) f.get(this));
                    else if (f.getType() == boolean.class)
                        data.putBoolean(f.getName(),
                                (Boolean) f.get(this));
                    else if (Parcelable.class.isAssignableFrom(f.getType())) {

                        Parcelable p = (Parcelable) f.get(this);
                        data
                                .putParcelable(f.getName(), p);
                    } else {
                        Log.i("BaseObj", "Ignoring " + f.getName() + " of type " + f.getType());
                    }
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        out.writeBundle(data);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        buildParcel(dest,flags);
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
            Log.e(p.getClass().getSimpleName(), "error", e);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.e(p.getClass().getSimpleName(), "error", e);
            e.printStackTrace();
        }
    }



}
