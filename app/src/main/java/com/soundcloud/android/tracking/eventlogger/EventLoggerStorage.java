package com.soundcloud.android.tracking.eventlogger;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.soundcloud.android.events.PlaybackEventData;
import com.soundcloud.android.model.ClientUri;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Pair;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.UUID;

public class EventLoggerStorage {

    private EventLoggerDbHelper mDbHelper;

    @Inject
    public EventLoggerStorage(Context context) {
        mDbHelper = new EventLoggerDbHelper(context);
    }

    public long insertEvent(PlaybackEventData playbackEventData){
        final SQLiteDatabase database = mDbHelper.getWritableDatabase();
        return database.insertOrThrow(EventLoggerDbHelper.EVENTS_TABLE, null, createValuesFromPlaybackEvent(playbackEventData));
    }

    public List<Pair<Long, String>> getUnpushedEvents(EventLoggerApi api) {
        List<Pair<Long, String>> urls = Lists.newArrayListWithCapacity(EventLogger.BATCH_SIZE);
        final SQLiteDatabase database = mDbHelper.getWritableDatabase();
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
        final SQLiteDatabase database = mDbHelper.getWritableDatabase();
        StringBuilder query = new StringBuilder(submitted.length * 22 - 1);
        query.append(EventLoggerDbHelper.TrackingEvents._ID).append(" IN (?");
        for (int i = 1; i < submitted.length; i++) query.append(",?");
        query.append(")");

        final int deleted = database.delete(EventLoggerDbHelper.EVENTS_TABLE, query.toString(), submitted);
        return deleted;
    }


    @VisibleForTesting
    static String buildUserUrn(final long userId) {
        if (userId < 0) {
            return "anonymous:" + UUID.randomUUID();
        } else {
            return ClientUri.forUser(userId).toString();
        }
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
}
