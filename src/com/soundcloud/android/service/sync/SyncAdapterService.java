package com.soundcloud.android.service.sync;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.c2dm.PushEvent;
import com.soundcloud.android.model.Activities;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.provider.SoundCloudDB;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;

import android.accounts.Account;
import android.app.Service;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

public class SyncAdapterService extends Service {
    /* package */  static final String TAG = SyncAdapterService.class.getSimpleName();
    private ScSyncAdapter mSyncAdapter;

    public static final int MAX_ARTWORK_PREFETCH = 40; // only prefetch N amount of artwork links

    public static final String EXTRA_CLEAR_MODE     = "clearMode";
    public static final String EXTRA_PUSH_EVENT     = "pushEvent";
    public static final String EXTRA_PUSH_EVENT_URI = "pushEventUri";

    public static final int CLEAR_ALL       = 1;
    public static final int REWIND_LAST_DAY = 2;

    @Override public void onCreate() {
        super.onCreate();
        mSyncAdapter = new ScSyncAdapter((SoundCloudApplication) getApplication());
    }

    @Override public IBinder onBind(Intent intent) {
        return mSyncAdapter.getSyncAdapterBinder();
    }

    /** @noinspection UnusedParameters*/
    /* package */ static Intent performSync(final SoundCloudApplication app,
                                            Account account,
                                            Bundle extras,
                                            ContentProviderClient provider,
                                            final SyncResult syncResult) {
        if (!app.useAccount(account).valid()) {
            Log.w(TAG, "no valid token, skip sync");
            syncResult.stats.numAuthExceptions++;
            return null;
        }

        final boolean force = extras.getBoolean(ContentResolver.SYNC_EXTRAS_FORCE, false);
        final Intent syncIntent = new Intent(app, ApiSyncService.class);
        final ArrayList<Uri> urisToSync = new ArrayList<Uri>();

        // for first sync set all last seen flags to "now"
        if (app.getAccountDataLong(User.DataKeys.LAST_INCOMING_SEEN) <= 0) {
            final long now = System.currentTimeMillis();
            app.setAccountData(User.DataKeys.LAST_INCOMING_SEEN, now);
            app.setAccountData(User.DataKeys.LAST_OWN_SEEN, now);
            app.setAccountData(User.DataKeys.LAST_INCOMING_NOTIFIED_AT, now);
        }

        PushEvent evt = PushEvent.fromExtras(extras);
        if (evt == PushEvent.FOLLOWER) {
            if (!handleFollowerEvent(app, extras)) {
                Log.w(TAG, "unhandled follower event:"+extras);
            }
            syncIntent.setData(Content.ME_FOLLOWERS.uri);
        } else {
             if (SyncConfig.shouldUpdateDashboard(app)) {
                if (SyncConfig.isIncomingEnabled(app, extras)) urisToSync.add(Content.ME_SOUND_STREAM.uri);
                if (SyncConfig.isExclusiveEnabled(app, extras)) urisToSync.add(Content.ME_EXCLUSIVE_STREAM.uri);
                if (SyncConfig.isActivitySyncEnabled(app, extras)) urisToSync.add(Content.ME_ACTIVITIES.uri);
            }

            if (SyncConfig.shouldSyncCollections(app)) {
                SyncContent.configureSyncExtras(app, urisToSync, force);
            }

            if (SyncConfig.shouldSync(app, SyncConfig.PREF_LAST_SYNC_CLEANUP, SyncConfig.CLEANUP_DELAY) || force) {
                urisToSync.add(Content.TRACK_CLEANUP.uri);
                urisToSync.add(Content.USERS_CLEANUP.uri);
            }

            if (SyncConfig.shouldSync(app, SyncConfig.PREF_LAST_USER_SYNC, SyncConfig.CLEANUP_DELAY) || force) {
                urisToSync.add(Content.ME.uri);
            }

            syncIntent.putParcelableArrayListExtra(ApiSyncService.EXTRA_SYNC_URIS, urisToSync);
        }

        Looper.prepare();
        syncIntent.putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, new ServiceResultReceiver(app, syncResult, extras));
        app.startService(syncIntent);
        Looper.loop();
        return syncIntent;
    }


    private static boolean handleFollowerEvent(SoundCloudApplication app, Bundle extras) {
        if (PreferenceManager.getDefaultSharedPreferences(app).getBoolean("notificationsFollowers", true)
                && extras.containsKey(SyncAdapterService.EXTRA_PUSH_EVENT_URI)) {
            final long id = PushEvent.getIdFromUri(extras.getString(SyncAdapterService.EXTRA_PUSH_EVENT_URI));
            if (id != -1) {
                User u = SoundCloudApplication.USER_CACHE.containsKey(id) ? SoundCloudApplication.USER_CACHE.get(id)
                        : SoundCloudDB.getUserById(app.getContentResolver(), id);
                if (u != null && !u.isStale()){
                    Message.showNewFollower(app, u);
                    return true;
                } else {
                    try {
                        HttpResponse resp = app.get(Request.to(Endpoints.USERS + "/" + id));
                        if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                            u = app.getMapper().readValue(resp.getEntity().getContent(), User.class);
                            SoundCloudDB.insertUser(app.getContentResolver(), u);
                            SoundCloudApplication.USER_CACHE.put(u);
                            Message.showNewFollower(app, u);
                            return true;
                        }
                    } catch (IOException e) {
                        Log.w(TAG, "error fetching user", e);
                    }
                }
            }
        }
        return false;
    }


    // only used for debugging
    public static void requestNewSync(SoundCloudApplication app, int clearMode) {
        switch (clearMode) {
            case CLEAR_ALL:
                app.setAccountData(User.DataKeys.LAST_INCOMING_SEEN, 1);
                app.setAccountData(User.DataKeys.LAST_OWN_SEEN, 1);
                app.setAccountData(User.DataKeys.LAST_OWN_NOTIFIED_ITEM, 1);
                app.setAccountData(User.DataKeys.LAST_INCOMING_NOTIFIED_ITEM, 1);
                app.setAccountData(User.DataKeys.LAST_INCOMING_NOTIFIED_AT, 1);
                clearActivities(app.getContentResolver());
                break;
            case REWIND_LAST_DAY:
                final long rewindTime = 24 * 3600000L; // 1d
                rewind(app, User.DataKeys.LAST_INCOMING_SEEN, null, rewindTime);
                rewind(app, User.DataKeys.LAST_OWN_SEEN, null, rewindTime);
                rewind(app, User.DataKeys.LAST_OWN_NOTIFIED_ITEM, null,  rewindTime);
                rewind(app, User.DataKeys.LAST_INCOMING_NOTIFIED_ITEM, null,  rewindTime);
                rewind(app, User.DataKeys.LAST_INCOMING_NOTIFIED_AT, null, rewindTime);
                clearActivities(app.getContentResolver());
                break;
            default:
        }

        final Bundle extras = new Bundle();
        extras.putInt(EXTRA_CLEAR_MODE, clearMode);
        ContentResolver.requestSync(app.getAccount(), ScContentProvider.AUTHORITY, extras);
    }

    private static void clearActivities(ContentResolver resolver){
        // drop all activities before re-sync
        int deleted = Activities.clear(null, resolver);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "deleted "+deleted+ " activities");
        }
    }

    private static void rewind(SoundCloudApplication app, String key1, String key2, long amount) {
        app.setAccountData(key1, app.getAccountDataLong(key2 == null ? key1 : key2) - amount);
    }
}
