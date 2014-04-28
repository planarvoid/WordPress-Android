package com.soundcloud.android.analytics.eventlogger;

import com.google.common.collect.Lists;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Pair;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class EventLoggerStorage {

    private final EventLoggerDbHelper dbHelper;

    @Inject
    EventLoggerStorage(EventLoggerDbHelper eventLoggerDbHelper) {
        dbHelper = eventLoggerDbHelper;
    }

    public long insertEvent(EventLoggerEvent eventObject) throws UnsupportedEncodingException {
        final SQLiteDatabase database = dbHelper.getWritableDatabase();
        return database.insertOrThrow(EventLoggerDbHelper.EVENTS_TABLE, null, createValuesFromEvent(eventObject));
    }

    public List<Pair<Long, String>> getUnpushedEvents(EventLoggerApi api) {

        Cursor cursor = dbHelper.getReadableDatabase().query(EventLoggerDbHelper.EVENTS_TABLE, null, null, null, null, null,
                EventLoggerDbHelper.TrackingEvents.TIMESTAMP + " DESC",
                String.valueOf(EventLoggerHandler.BATCH_SIZE));

        List<Pair<Long, String>> urls = Lists.newArrayListWithCapacity(EventLoggerHandler.BATCH_SIZE);
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

    public int deleteEventsById(String[] submitted) {
        final SQLiteDatabase database = dbHelper.getWritableDatabase();
        StringBuilder query = new StringBuilder(submitted.length * 22 - 1);
        query.append(EventLoggerDbHelper.TrackingEvents._ID).append(" IN (?");
        for (int i = 1; i < submitted.length; i++) query.append(",?");
        query.append(")");

        return database.delete(EventLoggerDbHelper.EVENTS_TABLE, query.toString(), submitted);
    }


    private ContentValues createValuesFromEvent(EventLoggerEvent event) throws UnsupportedEncodingException {
        ContentValues values = new ContentValues();
        values.put(EventLoggerDbHelper.TrackingEvents.TIMESTAMP, event.getTimeStamp());
        values.put(EventLoggerDbHelper.TrackingEvents.PATH, event.getPath());
        values.put(EventLoggerDbHelper.TrackingEvents.PARAMS, event.getParams());
        return values;
    }
}
