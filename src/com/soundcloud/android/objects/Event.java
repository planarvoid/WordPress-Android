
package com.soundcloud.android.objects;

import com.soundcloud.android.provider.DatabaseHelper.Events;
import com.soundcloud.android.provider.DatabaseHelper.Tables;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Event extends BaseObj implements Parcelable {

    public long id;
    public Date created_at;
    public String type;
    public String tags;
    public String label;
    public Origin origin;

    // locally used field
    public long user_id;
    public long origin_id;
    public boolean exclusive;
    public String next_cursor;
    public Track track;

    public static class Origin extends Track {
        public Track track;
    }


    public Event() {
    }

    public Event(Parcel in) {
        readFromParcel(in);
    }

    public Event(Cursor cursor, boolean concreteOnly) {
        String[] keys = cursor.getColumnNames();
        for (String key : keys) {
            // Events Cursors are preceded by the tablename, as they are often times
            // part of a join with another object
            if (concreteOnly){
                if (!key.contains(Tables.EVENTS+".")) continue;
                key = key.substring(7);
            }



            if (key.contentEquals("_id"))
                id = cursor.getLong(cursor.getColumnIndex(key));
            else
                try {
                    Field f = this.getClass().getDeclaredField(key);
                    if (f != null) {
                        if (f.getType() == String.class) {
                            f.set(this, cursor.getString(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Long.TYPE || f.getType() == Long.class) {
                            f.set(this, cursor.getLong(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Integer.TYPE || f.getType() == Integer.class) {
                            f.set(this, cursor.getInt(cursor.getColumnIndex(key)));
                        } else if (f.getType() == Boolean.TYPE) {
                            f.set(this, cursor.getInt(cursor.getColumnIndex(key)) != 0);
                        } else if (f.getType() == Double.TYPE) {
                            f.set(this, cursor.getDouble(cursor.getColumnIndex(key)));
                        }
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(getClass().getSimpleName(), "error", e);
                } catch (IllegalAccessException e) {
                    Log.e(getClass().getSimpleName(), "error", e);
                } catch (SecurityException e) {
                    Log.e(getClass().getSimpleName(), "error", e);
                } catch (NoSuchFieldException e) {
                    Log.e(getClass().getSimpleName(), "error", e);
                }
        }
    }

    public Track getTrack() {
        if (track != null)
            return track;
        if (type.equalsIgnoreCase("track"))
            return origin;
        else if (type.equalsIgnoreCase("track-sharing"))
            return origin.track;
        return null;
    }

    public long getOriginId() {
        if (track != null)
            return track.id;
        if (type.equalsIgnoreCase("track"))
            return ((Track) origin).id;
        else if (type.equalsIgnoreCase("track-sharing"))
            return (origin.track).id;
        return 0;
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
        Log.i("EventsAdapter","ADDING AN EVENT " + user_id);
        ContentValues cv = new ContentValues();
        cv.put(Events.TYPE, type);
        cv.put(Events.EXCLUSIVE, exclusive);
        cv.put(Events.CREATED_AT, created_at.getTime());
        cv.put(Events.TAGS, tags);
        cv.put(Events.LABEL, label);
        cv.put(Events.ORIGIN_ID, getOriginId());
        cv.put(Events.BELONGS_TO_USER, user_id);
        cv.put(Events.NEXT_CURSOR, next_cursor);
        return cv;
    }


    public static class CompareCreatedAt implements Comparator<Event>{
        @Override
        public int compare(Event c1, Event c2) {
            return c2.created_at.compareTo(c1.created_at);
        }
    }

}
