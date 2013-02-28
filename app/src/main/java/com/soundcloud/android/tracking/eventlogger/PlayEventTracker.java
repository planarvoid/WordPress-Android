package com.soundcloud.android.tracking.eventlogger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.BaseColumns;
import android.util.Log;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.ClientUri;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.utils.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static android.os.Process.THREAD_PRIORITY_LOWEST;
import static com.soundcloud.android.tracking.eventlogger.PlayEventTracker.TrackingDbHelper.EVENTS_TABLE;

public class PlayEventTracker {
    private static final String TAG = PlayEventTracker.class.getSimpleName();

    private static final int INSERT_TOKEN = 0;
    private static final int FINISH_TOKEN = 0xDEADBEEF;
    public static final int FLUSH_DELAY   = 60 * 1000;
    public static final int BATCH_SIZE    = 10;

    private SQLiteDatabase trackingDb;
    private TrackerHandler handler;

    private final Object lock = new Object();
    private Context mContext;

    private final PlayEventTrackingApi mTrackingApi;

    public PlayEventTracker(Context context, PlayEventTrackingApi api) {
        mContext = context;
        mTrackingApi = api;
    }

    public void trackEvent(final @Nullable Track track, final Action action, final long userId, final String originUrl,
                           final String level) {

        if (track == null) return;

        synchronized (lock) {
            if (handler == null) {
                HandlerThread thread = new HandlerThread("PlayEvent-tracking", THREAD_PRIORITY_LOWEST);
                thread.start();
                handler = new TrackerHandler(thread.getLooper());
            }
            TrackingParams params = new TrackingParams(track, action, userId, originUrl, level);
            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "new tracking event: " + params.toString());

            Message insert = handler.obtainMessage(INSERT_TOKEN, params);

            handler.removeMessages(FINISH_TOKEN);
            handler.sendMessage(insert);
        }
    }

    public Cursor eventsCursor() {
        return getTrackingDb().query(EVENTS_TABLE, null, null, null, null, null, null);
    }

    public void stop() {
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

        SQLiteDatabase db = getTrackingDb();

        db.beginTransaction();
        Cursor cursor = db.query(EVENTS_TABLE, null, null, null, null, null,
                TrackingEvents.TIMESTAMP+" DESC",
                String.valueOf(BATCH_SIZE));

        if (cursor != null && cursor.getCount() > 0) {
            String[] submitted = mTrackingApi.pushToRemote(cursor);
            if (submitted.length > 0) {
                StringBuilder query = new StringBuilder(submitted.length * 2 - 1);
                query.append(TrackingEvents._ID).append(" IN (?");
                for (int i = 1; i < submitted.length; i++) query.append(",?");
                query.append(")");

                final int deleted = db.delete(EVENTS_TABLE, query.toString(), submitted);
                if (deleted != submitted.length) {
                    Log.w(TAG, "error deleting events (deleted="+deleted+")");
                } else {
                    if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "submitted "+deleted+ " events");
                }
            }
        }
        boolean flushedAll = false;
        if (cursor != null) {
            flushedAll = cursor.getCount() < BATCH_SIZE;
            cursor.close();
        }

        db.setTransactionSuccessful();
        db.endTransaction();
        return flushedAll;
    }

    private SQLiteDatabase getTrackingDb() throws SQLiteException {
        if (trackingDb == null) {
            TrackingDbHelper helper = new TrackingDbHelper(mContext);
            trackingDb = helper.getWritableDatabase();
        }
        return trackingDb;
    }

    public interface TrackingEvents extends BaseColumns {
        final String TIMESTAMP = "timestamp";
        final String ACTION    = "action";
        final String SOUND_URN = "sound_urn";
        final String USER_URN  = "user_urn";
        final String SOUND_DURATION = "sound_duration";
        final String ORIGIN_URL     = "origin_url";
        final String LEVEL          = "level";
    }

    static class TrackingParams {
        final Track track;
        final Action action;
        final long timestamp;
        final long userId;
        final String originUrl;
        final String level;

        TrackingParams(Track track, Action action, long userId, String originUrl, String level) {
            this.track = track;
            this.action = action;
            this.userId = userId;
            this.originUrl = originUrl;
            this.level = level;
            this.timestamp = System.currentTimeMillis();
        }

        public ContentValues toContentValues() {
            ContentValues values = new ContentValues();
            values.put(TrackingEvents.TIMESTAMP, timestamp);
            values.put(TrackingEvents.ACTION, action.toApiName());
            values.put(TrackingEvents.SOUND_URN, ClientUri.forTrack(track.id).toString());
            values.put(TrackingEvents.SOUND_DURATION, track.duration);
            values.put(TrackingEvents.USER_URN, buildUserUrn(userId));
            values.put(TrackingEvents.ORIGIN_URL, originUrl);
            values.put(TrackingEvents.LEVEL, level);
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
                    "track_id=" + track.id +
                    ", action=" + action.name() +
                    ", timestamp=" + timestamp +
                    ", userId=" + userId +
                    ", originUrl='" + originUrl + '\'' +
                    ", level='" + level + '\'' +
                    '}';
        }
    }


    public static class TrackingDbHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "SoundCloud-tracking.sqlite";
        private static final int DATABASE_VERSION = 1;
        static final String EVENTS_TABLE = "events";

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
                TrackingEvents.ORIGIN_URL + " TEXT NOT NULL," + // figure out what this means in our app
                TrackingEvents.LEVEL + " TEXT NOT NULL," +
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
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int currentVersion) {
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
                    long id = getTrackingDb().insert(EVENTS_TABLE, null, params.toContentValues());

                    if (id < 0) {
                        Log.w(TAG, "error inserting tracking event");
                    }

                    synchronized (lock) {
                        if (handler != null) {
                            handler.removeMessages(FINISH_TOKEN);
                            handler.sendMessageDelayed(handler.obtainMessage(FINISH_TOKEN), FLUSH_DELAY);
                        }
                    }
                    break;
                case FINISH_TOKEN:
                    flushPlaybackTrackingEvents();
                    synchronized (lock) {
                        if (handler != null) {
                            if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Shutting down.");
                            handler.getLooper().quit();
                            handler = null;
                            if (trackingDb != null) {
                                trackingDb.close();
                                trackingDb = null;
                            }
                        }
                    }
                    break;
            }
        }
    }
}
