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
    private PlayEventTrackingDbHelper mTrackingDbHelper;
    private PlayEventTrackingApi mTrackingApi;

    public EventLoggerHandler(Looper looper, Context context,
                              PlayEventTrackingDbHelper playEventTrackingDbHelper, PlayEventTrackingApi trackingApi) {
        super(looper);
        mContext = context;
        mTrackingDbHelper = playEventTrackingDbHelper;
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
            case PlayEventTracker.INSERT_TOKEN:
                final PlaybackEventData params = (PlaybackEventData) msg.obj;

                mTrackingDbHelper.execute(new PlayEventTrackingDbHelper.ExecuteBlock() {
                    @Override
                    public void call(SQLiteDatabase database) {
                        long id = database.insertOrThrow(PlayEventTrackingDbHelper.EVENTS_TABLE, null,
                                createValuesFromPlaybackEvent(params));
                        if (id < 0) {
                            Log.w(PlayEventTracker.TAG, "error inserting tracking event");
                        }
                    }
                });

                removeMessages(PlayEventTracker.FLUSH_TOKEN);
                sendMessageDelayed(obtainMessage(PlayEventTracker.FLUSH_TOKEN), PlayEventTracker.FLUSH_DELAY);
                break;

            case PlayEventTracker.FLUSH_TOKEN:
                flushPlaybackTrackingEvents();
                break;

            case PlayEventTracker.FINISH_TOKEN:
                removeMessages(PlayEventTracker.FLUSH_TOKEN);
                flushPlaybackTrackingEvents();

                if (Log.isLoggable(PlayEventTracker.TAG, Log.DEBUG)) Log.d(PlayEventTracker.TAG, "Shutting down.");
                getLooper().quit();
                break;
        }
    }

    /* package */ boolean flushPlaybackTrackingEvents() {
        if (Log.isLoggable(PlayEventTracker.TAG, Log.DEBUG)) Log.d(PlayEventTracker.TAG, "flushPlaybackTrackingEvents");

        if (!IOUtils.isConnected(mContext)) {
            if (Log.isLoggable(PlayEventTracker.TAG, Log.DEBUG)) Log.d(PlayEventTracker.TAG, "not connected, skipping flush");
            return true;
        }

        final List<Pair<Long, String>> urls = new ArrayList<Pair<Long, String>>();
        mTrackingDbHelper.execute(new PlayEventTrackingDbHelper.ExecuteBlock() {
            @Override
            public void call(SQLiteDatabase database) {
                Cursor cursor = database.query(PlayEventTrackingDbHelper.EVENTS_TABLE, null, null, null, null, null,
                        PlayEventTrackingDbHelper.TrackingEvents.TIMESTAMP + " DESC",
                        String.valueOf(PlayEventTracker.BATCH_SIZE));

                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        final long eventId = cursor.getLong(cursor.getColumnIndex(PlayEventTrackingDbHelper.TrackingEvents._ID));
                        try {
                            urls.add(Pair.create(eventId, mTrackingApi.buildUrl(cursor)));
                        } catch (UnsupportedEncodingException e) {
                            Log.w(PlayEventTracker.TAG, "Failed to encode play event ", e);
                        }
                    }
                    cursor.close();
                }
            }
        });

        if (!urls.isEmpty()) {
            final String[] submitted = mTrackingApi.pushToRemote(urls);
            if (submitted.length > 0) {

                mTrackingDbHelper.execute(new PlayEventTrackingDbHelper.ExecuteBlock() {
                    @Override
                    public void call(SQLiteDatabase database) {
                        StringBuilder query = new StringBuilder(submitted.length * 22 - 1);
                        query.append(PlayEventTrackingDbHelper.TrackingEvents._ID).append(" IN (?");
                        for (int i = 1; i < submitted.length; i++) query.append(",?");
                        query.append(")");

                        final int deleted = database.delete(PlayEventTrackingDbHelper.EVENTS_TABLE, query.toString(), submitted);
                        if (deleted != submitted.length) {
                            Log.w(PlayEventTracker.TAG, "error deleting events (deleted=" + deleted + ")");
                        } else {
                            if (Log.isLoggable(PlayEventTracker.TAG, Log.DEBUG)) Log.d(PlayEventTracker.TAG, "submitted " + deleted + " events");
                        }
                    }
                });

            }
        }

        return urls.size() < PlayEventTracker.BATCH_SIZE;
    }

    private ContentValues createValuesFromPlaybackEvent(PlaybackEventData params) {
        ContentValues values = new ContentValues();
        values.put(PlayEventTrackingDbHelper.TrackingEvents.TIMESTAMP, params.getTimeStamp());
        values.put(PlayEventTrackingDbHelper.TrackingEvents.ACTION, params.getAction().toApiName());
        values.put(PlayEventTrackingDbHelper.TrackingEvents.SOUND_URN, ClientUri.forTrack(params.getTrack().getId()).toString());
        values.put(PlayEventTrackingDbHelper.TrackingEvents.SOUND_DURATION, params.getTrack().duration);
        values.put(PlayEventTrackingDbHelper.TrackingEvents.USER_URN, buildUserUrn(params.getUserId()));
        values.put(PlayEventTrackingDbHelper.TrackingEvents.SOURCE_INFO, params.getEventLoggerParams());
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
        return mTrackingDbHelper.getWritableDatabase().query(PlayEventTrackingDbHelper.EVENTS_TABLE, null, null, null, null, null, null);
    }

    @VisibleForTesting
    PlayEventTrackingDbHelper getTrackingDbHelper() {
        return mTrackingDbHelper;
    }
}
