package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.analytics.eventlogger.EventLoggerParams.Action;

import com.google.common.collect.Lists;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.model.Urn;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Pair;

import javax.inject.Inject;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class EventLoggerStorage {

    private final EventLoggerDbHelper mDbHelper;
    private final EventLoggerParamsBuilder mEventLoggerParamsBuilder;

    @Inject
    EventLoggerStorage(EventLoggerDbHelper eventLoggerDbHelper, EventLoggerParamsBuilder eventLoggerParamsBuilder) {
        mDbHelper = eventLoggerDbHelper;
        mEventLoggerParamsBuilder = eventLoggerParamsBuilder;
    }

    public long insertEvent(PlaybackEvent playbackEvent){
        final SQLiteDatabase database = mDbHelper.getWritableDatabase();
        return database.insertOrThrow(EventLoggerDbHelper.EVENTS_TABLE, null, createValuesFromPlaybackEvent(playbackEvent));
    }

    public List<Pair<Long, String>> getUnpushedEvents(EventLoggerApi api) {

        Cursor cursor = mDbHelper.getReadableDatabase().query(EventLoggerDbHelper.EVENTS_TABLE, null, null, null, null, null,
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

    public int deleteEventsById(String[] submitted){
        final SQLiteDatabase database = mDbHelper.getWritableDatabase();
        StringBuilder query = new StringBuilder(submitted.length * 22 - 1);
        query.append(EventLoggerDbHelper.TrackingEvents._ID).append(" IN (?");
        for (int i = 1; i < submitted.length; i++) query.append(",?");
        query.append(")");

        return database.delete(EventLoggerDbHelper.EVENTS_TABLE, query.toString(), submitted);
    }


    private ContentValues createValuesFromPlaybackEvent(PlaybackEvent params) {
        ContentValues values = new ContentValues();
        values.put(EventLoggerDbHelper.TrackingEvents.TIMESTAMP, params.getTimeStamp());
        values.put(EventLoggerDbHelper.TrackingEvents.ACTION, params.isPlayEvent() ? Action.PLAY : Action.STOP);
        values.put(EventLoggerDbHelper.TrackingEvents.SOUND_URN, Urn.forTrack(params.getTrack().getId()).toString());
        values.put(EventLoggerDbHelper.TrackingEvents.SOUND_DURATION, params.getTrack().duration);
        values.put(EventLoggerDbHelper.TrackingEvents.USER_URN, Urn.forUser(params.getUserId()).toString());
        values.put(EventLoggerDbHelper.TrackingEvents.SOURCE_INFO, mEventLoggerParamsBuilder.build(params.getTrackSourceInfo()));
        return values;
    }
}
