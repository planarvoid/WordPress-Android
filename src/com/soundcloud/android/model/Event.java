
package com.soundcloud.android.model;

import com.soundcloud.android.provider.DatabaseHelper;
import com.soundcloud.android.provider.DatabaseHelper.Events;
import com.soundcloud.android.provider.DatabaseHelper.Tables;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Event extends BaseObj implements Parcelable {
    public long id;
    public Date created_at;
    public String type;
    public String tags;
    public String label;

    // locally used field
    public long user_id;
    // stored in db
    public long origin_id;
    public boolean exclusive;
    public String next_cursor;

    public Track track;
    public Comment comment;
    public User user;


    public Event() {
    }

    public Event(Parcel in) {
        readFromParcel(in);
    }


    public Event(Cursor cursor, boolean aliasesOnly) {
        String[] keys = cursor.getColumnNames();
        for (String key : keys) {
            if (aliasesOnly && !key.contains(Tables.EVENTS + "_")) continue;
            if (key.contentEquals(aliasesOnly ? Events.ALIAS_ID : Events.ID)) {
                id = cursor.getLong(cursor.getColumnIndex(key));
            } else {
                try {
                    setFieldFromCursor(this,
                            this.getClass().getDeclaredField(aliasesOnly ? key.substring(7) : key),
                            cursor, key);
                } catch (SecurityException e) {
                    Log.e(getClass().getSimpleName(), "error", e);
                    e.printStackTrace();
                } catch (NoSuchFieldException e) {
                    Log.e(getClass().getSimpleName(), "error", e);
                    e.printStackTrace();
                }
            }
        }
    }

    public void updateEventObjectsFromDb(ContentResolver contentResolver, Long currentUserId) {
        if (track != null) track.updateUserPlayedFromDb(contentResolver,currentUserId);
    }

    public static final Parcelable.Creator<Event> CREATOR = new Parcelable.Creator<Event>() {
        public Event createFromParcel(Parcel in) {
            return new Event(in);
        }

        public Event[] newArray(int size) {
            return new Event[size];
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

    public ContentValues buildContentValues(long user_id, boolean exclusive){
        ContentValues cv = new ContentValues();
        cv.put(Events.TYPE, type);
        cv.put(Events.EXCLUSIVE, exclusive);
        cv.put(Events.CREATED_AT, created_at.getTime());
        cv.put(Events.TAGS, tags);
        cv.put(Events.LABEL, label);
        cv.put(Events.ORIGIN_ID, origin_id);
        cv.put(Events.USER_ID, user_id);
        if (!TextUtils.isEmpty(next_cursor)) cv.put(Events.NEXT_CURSOR, next_cursor);
        return cv;
    }
}
