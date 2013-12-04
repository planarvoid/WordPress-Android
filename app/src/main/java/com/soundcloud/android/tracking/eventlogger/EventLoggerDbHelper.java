package com.soundcloud.android.tracking.eventlogger;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.soundcloud.android.events.PlaybackEventData;
import com.soundcloud.android.model.ClientUri;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;
import android.util.Pair;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class EventLoggerDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "SoundCloud-tracking.sqlite";
    private static final int DATABASE_VERSION = 2;
    static final String EVENTS_TABLE = "events";

    public interface ExecuteBlock {
        void call(SQLiteDatabase database) throws SQLException;
    }

    /**
     * Play duration tracking
     */
    static final String DATABASE_CREATE_PLAY_TRACKING = "CREATE TABLE IF NOT EXISTS " + EVENTS_TABLE + "(" +
            TrackingEvents._ID + " INTEGER PRIMARY KEY," +
            TrackingEvents.TIMESTAMP + " INTEGER NOT NULL," +
            TrackingEvents.ACTION + " TEXT NOT NULL," +
            TrackingEvents.SOUND_URN + " TEXT NOT NULL," +
            TrackingEvents.USER_URN + " TEXT NOT NULL," + // soundcloud:users:123 for logged in, anonymous:<UUID> for logged out
            TrackingEvents.SOUND_DURATION + " INTEGER NOT NULL," + // this it the total sound length in millis
            TrackingEvents.SOURCE_INFO + " TEXT NOT NULL," +
            "UNIQUE (" + TrackingEvents.TIMESTAMP + ", " + TrackingEvents.ACTION + ") ON CONFLICT IGNORE" +
            ")";

    @Inject
    public EventLoggerDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DATABASE_CREATE_PLAY_TRACKING);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int currentVersion) {
        // for now just recreate table and drop any local tracking events
        onRecreateDb(db);
    }

    public long insertEvent(PlaybackEventData playbackEventData){
        final SQLiteDatabase database = getWritableDatabase();
        return database.insertOrThrow(EventLoggerDbHelper.EVENTS_TABLE, null, createValuesFromPlaybackEvent(playbackEventData));
    }

    public List<Pair<Long, String>> getUnpushedEvents(EventLoggerApi api) {
        List<Pair<Long, String>> urls = Lists.newArrayListWithCapacity(EventLogger.BATCH_SIZE);
        final SQLiteDatabase database = getWritableDatabase();
        Cursor cursor = database.query(EventLoggerDbHelper.EVENTS_TABLE, null, null, null, null, null,
                EventLoggerDbHelper.TrackingEvents.TIMESTAMP + " DESC",
                String.valueOf(EventLogger.BATCH_SIZE));

        if (cursor != null) {
            while (cursor.moveToNext()) {
                final long eventId = cursor.getLong(cursor.getColumnIndex(EventLoggerDbHelper.TrackingEvents._ID));
                try {
                    urls.add(Pair.create(eventId, api.buildUrl(cursor)));
                } catch (UnsupportedEncodingException e) {
                    Log.w(EventLogger.TAG, "Failed to encode play event ", e);
                }
            }
            cursor.close();
        }
        return urls;
    }

    public int deleteEventsById(String[] submitted){
        final SQLiteDatabase database = getWritableDatabase();
        StringBuilder query = new StringBuilder(submitted.length * 22 - 1);
        query.append(EventLoggerDbHelper.TrackingEvents._ID).append(" IN (?");
        for (int i = 1; i < submitted.length; i++) query.append(",?");
        query.append(")");

        final int deleted = database.delete(EventLoggerDbHelper.EVENTS_TABLE, query.toString(), submitted);
        return deleted;
    }

    private void onRecreateDb(SQLiteDatabase db){
        db.execSQL("DROP TABLE IF EXISTS " + EVENTS_TABLE);
        onCreate(db);
    }

    public interface TrackingEvents extends BaseColumns {
        final String TIMESTAMP = "timestamp";
        final String ACTION    = "action";
        final String SOUND_URN = "sound_urn";
        final String USER_URN  = "user_urn";
        final String SOUND_DURATION = "sound_duration";
        final String SOURCE_INFO = "source_info";
    }

    private ContentValues createValuesFromPlaybackEvent(PlaybackEventData params) {
        ContentValues values = new ContentValues();
        values.put(EventLoggerDbHelper.TrackingEvents.TIMESTAMP, params.getTimeStamp());
        values.put(EventLoggerDbHelper.TrackingEvents.ACTION, params.getAction().toApiName());
        values.put(EventLoggerDbHelper.TrackingEvents.SOUND_URN, ClientUri.forTrack(params.getTrack().getId()).toString());
        values.put(EventLoggerDbHelper.TrackingEvents.SOUND_DURATION, params.getTrack().duration);
        values.put(EventLoggerDbHelper.TrackingEvents.USER_URN, buildUserUrn(params.getUserId()));
        values.put(EventLoggerDbHelper.TrackingEvents.SOURCE_INFO, params.getEventLoggerParams());
        return values;
    }

    @VisibleForTesting
    static String buildUserUrn(final long userId) {
        if (userId < 0) {
            return "anonymous:" + UUID.randomUUID();
        } else {
            return ClientUri.forUser(userId).toString();
        }
    }
}
