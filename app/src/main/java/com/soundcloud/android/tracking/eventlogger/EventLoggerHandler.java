package com.soundcloud.android.tracking.eventlogger;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.PlaybackEventData;
import com.soundcloud.android.model.ClientUri;
import com.soundcloud.android.utils.IOUtils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.Pair;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EventLoggerHandler extends Handler {

    private Context mContext;
    private EventLoggerDbHelper mTrackingDbHelper;
    private EventLoggerApi mTrackingApi;

    public EventLoggerHandler(Looper looper, Context context,
                              EventLoggerDbHelper eventLoggerDbHelper, EventLoggerApi trackingApi) {
        super(looper);
        mContext = context;
        mTrackingDbHelper = eventLoggerDbHelper;
        mTrackingApi = trackingApi;
    }

    @Override
    public void handleMessage(Message msg) {
        try {
            handleTrackingEvent(msg);
        } catch (Exception e) {
            SoundCloudApplication.handleSilentException("Error in tracking handler", e);
        }
    }

    private void handleTrackingEvent(Message msg) {
        switch (msg.what) {
            case EventLogger.INSERT_TOKEN:
                final PlaybackEventData params = (PlaybackEventData) msg.obj;

                mTrackingDbHelper.execute(new EventLoggerDbHelper.ExecuteBlock() {
                    @Override
                    public void call(SQLiteDatabase database) {
                        long id = database.insertOrThrow(EventLoggerDbHelper.EVENTS_TABLE, null,
                                createValuesFromPlaybackEvent(params));
                        if (id < 0) {
                            Log.w(EventLogger.TAG, "error inserting tracking event");
                        }
                    }
                });

                removeMessages(EventLogger.FLUSH_TOKEN);
                sendMessageDelayed(obtainMessage(EventLogger.FLUSH_TOKEN), EventLogger.FLUSH_DELAY);
                break;

            case EventLogger.FLUSH_TOKEN:
                flushPlaybackTrackingEvents();
                break;

            case EventLogger.FINISH_TOKEN:
                removeMessages(EventLogger.FLUSH_TOKEN);
                flushPlaybackTrackingEvents();

                if (Log.isLoggable(EventLogger.TAG, Log.DEBUG)) Log.d(EventLogger.TAG, "Shutting down.");
                getLooper().quit();
                break;
        }
    }

    /* package */ boolean flushPlaybackTrackingEvents() {
        if (Log.isLoggable(EventLogger.TAG, Log.DEBUG)) Log.d(EventLogger.TAG, "flushPlaybackTrackingEvents");

        if (!IOUtils.isConnected(mContext)) {
            if (Log.isLoggable(EventLogger.TAG, Log.DEBUG)) Log.d(EventLogger.TAG, "not connected, skipping flush");
            return true;
        }

        final List<Pair<Long, String>> urls = new ArrayList<Pair<Long, String>>();
        mTrackingDbHelper.execute(new EventLoggerDbHelper.ExecuteBlock() {
            @Override
            public void call(SQLiteDatabase database) {
                Cursor cursor = database.query(EventLoggerDbHelper.EVENTS_TABLE, null, null, null, null, null,
                        EventLoggerDbHelper.TrackingEvents.TIMESTAMP + " DESC",
                        String.valueOf(EventLogger.BATCH_SIZE));

                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        final long eventId = cursor.getLong(cursor.getColumnIndex(EventLoggerDbHelper.TrackingEvents._ID));
                        try {
                            urls.add(Pair.create(eventId, mTrackingApi.buildUrl(cursor)));
                        } catch (UnsupportedEncodingException e) {
                            Log.w(EventLogger.TAG, "Failed to encode play event ", e);
                        }
                    }
                    cursor.close();
                }
            }
        });

        if (!urls.isEmpty()) {
            final String[] submitted = mTrackingApi.pushToRemote(urls);
            if (submitted.length > 0) {

                mTrackingDbHelper.execute(new EventLoggerDbHelper.ExecuteBlock() {
                    @Override
                    public void call(SQLiteDatabase database) {
                        StringBuilder query = new StringBuilder(submitted.length * 22 - 1);
                        query.append(EventLoggerDbHelper.TrackingEvents._ID).append(" IN (?");
                        for (int i = 1; i < submitted.length; i++) query.append(",?");
                        query.append(")");

                        final int deleted = database.delete(EventLoggerDbHelper.EVENTS_TABLE, query.toString(), submitted);
                        if (deleted != submitted.length) {
                            Log.w(EventLogger.TAG, "error deleting events (deleted=" + deleted + ")");
                        } else {
                            if (Log.isLoggable(EventLogger.TAG, Log.DEBUG)) Log.d(EventLogger.TAG, "submitted " + deleted + " events");
                        }
                    }
                });

            }
        }

        return urls.size() < EventLogger.BATCH_SIZE;
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

    private static String buildUserUrn(final long userId) {
        if (userId < 0) {
            return "anonymous:" + UUID.randomUUID();
        } else {
            return ClientUri.forUser(userId).toString();
        }
    }

    @VisibleForTesting
    Cursor eventsCursor() {
        return mTrackingDbHelper.getWritableDatabase().query(EventLoggerDbHelper.EVENTS_TABLE, null, null, null, null, null, null);
    }

    @VisibleForTesting
    EventLoggerDbHelper getTrackingDbHelper() {
        return mTrackingDbHelper;
    }
}
