package com.soundcloud.android.tracking.eventlogger;

import static android.os.Process.THREAD_PRIORITY_LOWEST;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.Event;
import com.soundcloud.android.events.PlaybackEventData;
import com.soundcloud.android.model.ClientUri;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.utils.IOUtils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.BaseColumns;
import android.util.Log;
import android.util.Pair;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayEventTracker {
    private static final String TAG = PlayEventTracker.class.getSimpleName();

    public interface EventLoggerKeys {
        String ORIGIN_URL = "context";
        String TRIGGER = "trigger";
        String SOURCE = "source";
        String SOURCE_VERSION = "source_version";
        String SET_ID = "set_id";
        String SET_POSITION = "set_position";
    }

    private static final int INSERT_TOKEN = 0;
    private static final int FLUSH_TOKEN = 1;
    private static final int FINISH_TOKEN = 0xDEADBEEF;

    // service stop delay is 60s, this is bigger to avoid simultaneous flushes
    public static final int FLUSH_DELAY   = 90 * 1000;
    public static final int BATCH_SIZE    = 10;

    private TrackerHandler handler;

    private TrackingDbHelper trackingDbHelper;
    private final Object lock = new Object();
    private Context mContext;

    private final PlayEventTrackingApi mTrackingApi;

    public PlayEventTracker(Context context) {
        this(context, new PlayEventTrackingApi(context.getString(R.string.app_id)));
    }

    public PlayEventTracker(Context context, PlayEventTrackingApi api) {
        mContext = context;
        mTrackingApi = api;
        trackingDbHelper = new TrackingDbHelper(mContext);

        Event.PLAYBACK_SERVICE_DESTROYED.subscribe(new PlaybackServiceDestroyedObserver());
    }

    public void trackEvent(PlaybackEventData playbackEventData) {

        synchronized (lock) {
            if (handler == null) {
                HandlerThread thread = new HandlerThread("PlayEvent-tracking", THREAD_PRIORITY_LOWEST);
                thread.start();
                handler = new TrackerHandler(thread.getLooper());
            }
            TrackingParams params = new TrackingParams(playbackEventData);
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "new tracking event: " + params.toString());

            Message insert = handler.obtainMessage(INSERT_TOKEN, params);

            handler.removeMessages(FINISH_TOKEN);
            handler.sendMessage(insert);
        }
    }

    @VisibleForTesting
    Cursor eventsCursor() {
        return trackingDbHelper.getWritableDatabase().query(TrackingDbHelper.EVENTS_TABLE, null, null, null, null, null, null);
    }

    @VisibleForTesting
    void stop() {
        synchronized (lock) {
            if (handler != null) {
                handler.obtainMessage(FINISH_TOKEN).sendToTarget();
            }
        }
    }

    /* package */ boolean flushPlaybackTrackingEvents() {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "flushPlaybackTrackingEvents");

        if (!IOUtils.isConnected(mContext)) {
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "not connected, skipping flush");
            return true;
        }

        final List<Pair<Long, String>> urls = new ArrayList<Pair<Long, String>>();
        trackingDbHelper.execute(new TrackingDbHelper.ExecuteBlock() {
            @Override
            public void call(SQLiteDatabase database) {
                Cursor cursor = database.query(TrackingDbHelper.EVENTS_TABLE, null, null, null, null, null,
                        TrackingEvents.TIMESTAMP + " DESC",
                        String.valueOf(BATCH_SIZE));

                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        final long eventId = cursor.getLong(cursor.getColumnIndex(TrackingEvents._ID));
                        try {
                            urls.add(Pair.create(eventId, mTrackingApi.buildUrl(cursor)));
                        } catch (UnsupportedEncodingException e) {
                            Log.w(TAG, "Failed to encode play event ", e);
                        }
                    }
                    cursor.close();
                }
            }
        });

        if (!urls.isEmpty()) {
            final String[] submitted = mTrackingApi.pushToRemote(urls);
            if (submitted.length > 0) {

                trackingDbHelper.execute(new TrackingDbHelper.ExecuteBlock() {
                    @Override
                    public void call(SQLiteDatabase database) {
                        StringBuilder query = new StringBuilder(submitted.length * 22 - 1);
                        query.append(TrackingEvents._ID).append(" IN (?");
                        for (int i = 1; i < submitted.length; i++) query.append(",?");
                        query.append(")");

                        final int deleted = database.delete(TrackingDbHelper.EVENTS_TABLE, query.toString(), submitted);
                        if (deleted != submitted.length) {
                            Log.w(TAG, "error deleting events (deleted=" + deleted + ")");
                        } else {
                            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "submitted " + deleted + " events");
                        }
                    }
                });

            }
        }

        return urls.size() < BATCH_SIZE;
    }

    /* package */ TrackingDbHelper getTrackingDbHelper() {
        return trackingDbHelper;
    }

    public interface TrackingEvents extends BaseColumns {
        final String TIMESTAMP = "timestamp";
        final String ACTION    = "action";
        final String SOUND_URN = "sound_urn";
        final String USER_URN  = "user_urn";
        final String SOUND_DURATION = "sound_duration";
        final String SOURCE_INFO = "source_info";
    }

    static class TrackingParams {
        final PlaybackEventData mPlaybackEventData;
        final long mTimeStamp;

        TrackingParams(PlaybackEventData playbackEventData) {
            mPlaybackEventData = playbackEventData;
            mTimeStamp = System.currentTimeMillis();
        }

        public ContentValues toContentValues() {
            ContentValues values = new ContentValues();
            values.put(TrackingEvents.TIMESTAMP, mTimeStamp);
            values.put(TrackingEvents.ACTION, mPlaybackEventData.getAction().toApiName());
            values.put(TrackingEvents.SOUND_URN, ClientUri.forTrack(mPlaybackEventData.getTrack().getId()).toString());
            values.put(TrackingEvents.SOUND_DURATION, mPlaybackEventData.getTrack().duration);
            values.put(TrackingEvents.USER_URN, buildUserUrn(mPlaybackEventData.getUserId()));
            values.put(TrackingEvents.SOURCE_INFO, mPlaybackEventData.getEventLoggerParams());
            return values;

        }

        private String buildUserUrn(final long userId) {
            if (userId < 0) {
                return "anonymous:" + UUID.randomUUID();
            } else {
                return ClientUri.forUser(userId).toString();
            }
        }

        @Override
        public String toString() {
            return "TrackingParams{" +
                    "playback_event_data=" + mPlaybackEventData +
                    ", timestamp=" + mTimeStamp +
                    '}';
        }
    }


    public static class TrackingDbHelper extends SQLiteOpenHelper {
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


        public TrackingDbHelper(Context context) {
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

        private void onRecreateDb(SQLiteDatabase db){
            db.execSQL("DROP TABLE IF EXISTS " + EVENTS_TABLE);
            onCreate(db);
        }

        public void execute(ExecuteBlock block) {
            final SQLiteDatabase writableDatabase = getWritableDatabase();
            try {
                block.call(writableDatabase);
            } catch (SQLException ex){
                Log.i(TAG, "Sql exception " , ex);
            } finally {
                close();
            }

        }
    }

    public class TrackerHandler extends Handler {
        public TrackerHandler(Looper looper) {
            super(looper);
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
                case INSERT_TOKEN:
                    final TrackingParams params = (TrackingParams) msg.obj;

                    trackingDbHelper.execute(new TrackingDbHelper.ExecuteBlock() {
                        @Override
                        public void call(SQLiteDatabase database) {
                            long id = database.insertOrThrow(TrackingDbHelper.EVENTS_TABLE, null, params.toContentValues());
                            if (id < 0) {
                                Log.w(TAG, "error inserting tracking event");
                            }
                        }
                    });

                    removeMessages(FLUSH_TOKEN);
                    sendMessageDelayed(obtainMessage(FLUSH_TOKEN), FLUSH_DELAY);
                    break;

                case FLUSH_TOKEN:
                    flushPlaybackTrackingEvents();
                    break;

                case FINISH_TOKEN:
                    removeMessages(FLUSH_TOKEN);
                    flushPlaybackTrackingEvents();

                    if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Shutting down.");
                    getLooper().quit();
                    break;
            }
        }
    }

    private class PlaybackServiceDestroyedObserver extends DefaultObserver<Integer> {
        @Override
        public void onNext(Integer args) {
            stop();
        }
    }
}
