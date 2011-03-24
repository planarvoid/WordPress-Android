
package com.soundcloud.android.objects;

import com.soundcloud.android.provider.ScContentProvider;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;
import android.util.Log;

import java.lang.reflect.Field;

@SuppressWarnings({"UnusedDeclaration"})
@JsonIgnoreProperties(ignoreUnknown=true)
public class Recording extends BaseObj implements Parcelable {
    private static final String TAG = "Track";

    public long id;
    public long userid;
    public long timestamp;
    public double longitude;
    public double latitude;
    public String what_text;
    public String where_text;
    public String audio_path;
    public String artwork_path;
    public int audio_profile;
    public boolean uploaded;

    public static final class Recordings implements BaseColumns {
        private Recordings() {
        }

        public static final Uri CONTENT_URI = Uri.parse("content://"
                + ScContentProvider.AUTHORITY + "/Recordings");

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/soundcloud.recordings";
        public static final String ID = "_id";
        public static final String USER_ID = "user_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String LONGITUDE = "longitude";
        public static final String LATITUDE = "latitude";
        public static final String WHAT_TEXT = "what_text";
        public static final String WHERE_TEXT = "where_text";
        public static final String AUDIO_PATH = "audio_path";
        public static final String ARTWORK_PATH = "artwork_path";
        public static final String AUDIO_PROFILE = "audio_profile";
        public static final String UPLOADED = "uploaded";
    }

    public Recording() {
    }

    public Recording(Parcel in) {
        readFromParcel(in);
    }

    public Recording(Cursor cursor) {
        if (cursor.getCount() != 0) {
            cursor.moveToFirst();

            String[] keys = cursor.getColumnNames();
            for (String key : keys) {
                if (key.contentEquals("_id")) id = cursor.getLong(cursor.getColumnIndex(key));
                else
                try {
                    Field f = this.getClass().getDeclaredField(key);
                    if (f != null) {
                        if (f.getType() == String.class) {
                            f.set(this, cursor.getString(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Long.TYPE || f.getType() == Long.class){
                            f.set(this, cursor.getLong(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Integer.TYPE || f.getType() == Integer.class) {
                            f.set(this, cursor.getInt(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Boolean.TYPE) {
                            f.set(this, cursor.getInt(cursor.getColumnIndex(key)) != 0);
                        } else if (f.getType() == Double.TYPE) {
                            f.set(this, cursor.getDouble(cursor.getColumnIndex(key)) != 0);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "error", e);
                } catch (IllegalAccessException e) {
                    Log.e(TAG, "error", e);
                } catch (SecurityException e) {
                    Log.e(TAG, "error", e);
                } catch (NoSuchFieldException e) {
                    Log.e(TAG, "error", e);
                }
            }
        }
    }

    public static final Parcelable.Creator<Recording> CREATOR = new Parcelable.Creator<Recording>() {
        public Recording createFromParcel(Parcel in) {
            return new Recording(in);
        }

        public Recording[] newArray(int size) {
            return new Recording[size];
        }
    };

    @Override
    public void writeToParcel(Parcel out, int flags) {
        buildParcel(out,flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

}
