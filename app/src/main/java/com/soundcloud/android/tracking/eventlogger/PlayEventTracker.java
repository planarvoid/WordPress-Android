package com.soundcloud.android.tracking.eventlogger;

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
import com.soundcloud.android.R;
import com.soundcloud.android.model.ClientUri;
import com.soundcloud.android.model.Track;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static android.os.Process.THREAD_PRIORITY_LOWEST;
import static com.soundcloud.android.tracking.eventlogger.PlayEventTracker.TrackingDbHelper.EVENTS_TABLE;

public class PlayEventTracker {
    private static final String TAG = PlayEventTracker.class.getSimpleName();

    private static final int INSERT_TOKEN = 0;
    private static final int FINISH_TOKEN = 0xDEADBEEF;
    public static final int THREAD_LIFETIME = 20000;

    private SQLiteDatabase trackingDb;
    private TrackerHandler handler;

    private final Object lock = new Object();
    private Context mContext;



    public PlayEventTracker(Context context) {
        mContext = context;
    }

    public void trackEvent(final @Nullable Track track, final Action action, final long userId, final String originUrl,
                           final String level) {

        Log.d(TAG, "trackEvent("+track.id+", "+action+", "+userId+","+originUrl+","+level+")");

        if (track == null) return;

        synchronized (lock) {
            if (handler == null) {
                HandlerThread thread = new HandlerThread("PlayEvent-tracking", THREAD_PRIORITY_LOWEST);
                thread.start();
                handler = new TrackerHandler(thread.getLooper());
            }
            TrackingParams params = new TrackingParams(track, action, userId, originUrl, level);
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

    private void flushPlaybackTrackingEvents() {
        Log.d(TAG, "flushPlaybackTrackingEvents");

        PlayEventTrackingApi trackingApi = new PlayEventTrackingApi(mContext.getString(R.string.client_id));
        SQLiteDatabase db = getTrackingDb();

        db.beginTransaction();
        Cursor cursor = db.query(EVENTS_TABLE, null, null, null, null, null, null);
        if (cursor != null) {
            trackingApi.pushToRemote(cursor);
            cursor.close();
            db.delete(EVENTS_TABLE, null, null);
        }

        db.endTransaction();
    }

    private SQLiteDatabase getTrackingDb() {
        if (trackingDb == null) {
            TrackingDbHelper helper = new TrackingDbHelper(mContext);
            trackingDb = helper.getWritableDatabase();
        }
        return trackingDb;
    }

    public interface TrackingEvents extends BaseColumns {
        final String TIMESTAMP = "timestamp";
        final String ACTION = "action";
        final String SOUND_URN = "sound_urn";
        final String USER_URN = "user_urn";
        final String SOUND_DURATION = "sound_duration";
        final String ORIGIN_URL = "origin_url";
        final String LEVEL = "level";
    }

    static class TrackingParams {
        final Track track;
        final Action action;
        final long userId;
        final String originUrl;
        final String level;

        TrackingParams(Track track, Action action, long userId, String originUrl, String level) {
            this.track = track;
            this.action = action;
            this.userId = userId;
            this.originUrl = originUrl;
            this.level = level;
        }

        public ContentValues toContentValues() {
            ContentValues values = new ContentValues();
            values.put(TrackingEvents.TIMESTAMP, System.currentTimeMillis());
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
            switch (msg.what) {
                case INSERT_TOKEN: {
                    final TrackingParams params = (TrackingParams) msg.obj;
                    long id = getTrackingDb().insert(EVENTS_TABLE, null, params.toContentValues());

                    if (id < 0) {
                        Log.w(TAG, "error inserting tracking event");
                    }

                    synchronized (lock) {
                        if (handler != null) {
                            handler.sendMessageDelayed(handler.obtainMessage(FINISH_TOKEN), THREAD_LIFETIME);
                        }
                    }
                    break;
                }

                case FINISH_TOKEN: {
                    flushPlaybackTrackingEvents();

                    synchronized (lock) {
                        if (handler != null) {
                            handler.getLooper().quit();
                            handler = null;
                        }
                    }
                    break;
                }
            }

        }
    }
}
