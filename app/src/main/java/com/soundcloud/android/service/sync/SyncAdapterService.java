package com.soundcloud.android.service.sync;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.c2dm.PushEvent;
import com.soundcloud.android.model.ContentStats;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.act.Activities;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.utils.DebugUtils;
import com.soundcloud.api.Endpoints;
import com.soundcloud.api.Request;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.Nullable;

import android.accounts.Account;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
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

/**
 * Sync service - delegates to {@link ApiSyncService} for the actual syncing. This class is responsible for the setup
 * and handling of the background syncing.
 */
public class SyncAdapterService extends Service {
    /* package */  static final String TAG = SyncAdapterService.class.getSimpleName();
    private static final boolean DEBUG_CANCEL = Boolean.valueOf(System.getProperty("syncadapter.debug.cancel", null));

    public static final int MAX_ARTWORK_PREFETCH = 40; // only prefetch N amount of artwork links

    public static final String EXTRA_CLEAR_MODE     = "clearMode";
    public static final String EXTRA_PUSH_EVENT     = "pushEvent";
    public static final String EXTRA_PUSH_EVENT_URI = "pushEventUri";

    public static final int CLEAR_ALL       = 1;
    public static final int REWIND_LAST_DAY = 2;

    private AbstractThreadedSyncAdapter mSyncAdapter;

    @Override public void onCreate() {
        super.onCreate();
        mSyncAdapter = new AbstractThreadedSyncAdapter(this, false) {
            private Looper looper;

            @Override
            public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
                if (DEBUG_CANCEL) DebugUtils.setLogLevels();

                AndroidCloudAPI.Wrapper.setBackgroundMode(true);

                Looper.prepare();
                looper = Looper.myLooper();
                if (performSync((SoundCloudApplication) getApplication(), account, extras, syncResult, new Runnable() {
                    @Override public void run() {
                        looper.quit();
                    }
                })) {
                    Looper.loop(); // wait for results to come in
                }
                AndroidCloudAPI.Wrapper.setBackgroundMode(false);
            }

            @Override
            public void onSyncCanceled() {
                if (DEBUG_CANCEL) {
                    Log.d(TAG, "sync canceled, dumping stack");
                    DebugUtils.dumpStack(getContext());
                    new Thread() {
                        @Override public void run() {
                            DebugUtils.dumpLog(getContext());
                        }
                    }.start();
                }

                if (looper != null) looper.quit(); // make sure  sync thread exits
                super.onSyncCanceled();
            }
        };
    }

    @Override public IBinder onBind(Intent intent) {
        return mSyncAdapter.getSyncAdapterBinder();
    }

    /**
     * @return true if a sync has been started
     */
    /* package */ static boolean performSync(final SoundCloudApplication app,
                                            Account account,
                                            Bundle extras,
                                            final SyncResult syncResult,
                                            final @Nullable Runnable onResult) {
        if (!app.useAccount(account).valid()) {
            Log.w(TAG, "no valid token, skip sync");
            syncResult.stats.numAuthExceptions++;
            return false;
        }

        // for first sync set all last seen flags to "now"
        if (ContentStats.getLastSeen(app, Content.ME_SOUND_STREAM) <= 0) {
            final long now = System.currentTimeMillis();
            ContentStats.setLastSeen(app, Content.ME_SOUND_STREAM, now);
            ContentStats.setLastNotified(app, Content.ME_SOUND_STREAM, now);
            ContentStats.setLastSeen(app, Content.ME_ACTIVITIES, now);
        }

        final Intent syncIntent = getSyncIntent(app, extras);
        if (syncIntent.getData() != null || syncIntent.hasExtra(ApiSyncService.EXTRA_SYNC_URIS)) {
            syncIntent.putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, new ServiceResultReceiver(app, syncResult, extras) {
                @Override
                protected void onReceiveResult(int resultCode, Bundle resultData) {
                    try {
                        super.onReceiveResult(resultCode, resultData);
                    } finally {
                        // make sure the looper quits in any case - otherwise sync just hangs, holding wakelock
                        if (onResult != null) onResult.run();
                    }
                }
            });
            app.startService(syncIntent);
            return true;
        } else {
            return false;
        }
    }

    private static Intent getSyncIntent(SoundCloudApplication app, Bundle extras) {
        final Intent syncIntent = new Intent(app, ApiSyncService.class);
        switch (PushEvent.fromExtras(extras)) {
            case FOLLOWER:
                if (!handleFollowerEvent(app, extras)) {
                    Log.w(TAG, "unhandled follower event:" + extras);
                }

                if (SyncConfig.shouldSyncCollections(app)) {
                    syncIntent.setData(Content.ME_FOLLOWERS.uri); // refresh follower list
                } else {
                    // set last sync time to 0 so it auto-refreshes on next load
                    final LocalCollection lc = LocalCollection.fromContent(Content.ME_FOLLOWERS, app.getContentResolver(), false);
                    if (lc != null) lc.updateLastSyncSuccessTime(0, app.getContentResolver());
                }

                break;
            case COMMENT:
            case LIKE:
                if (SyncConfig.shouldUpdateDashboard(app) && SyncConfig.isActivitySyncEnabled(app, extras)) {
                    syncIntent.setData(Content.ME_ACTIVITIES.uri);
                } else {
                    // set last sync time to 0 so it auto-refreshes on next load
                    final LocalCollection lc = LocalCollection.fromContent(Content.ME_ACTIVITIES, app.getContentResolver(), false);
                    if (lc != null) lc.updateLastSyncSuccessTime(0, app.getContentResolver());
                }
                break;

            case NONE:
                final boolean manual = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
                final ArrayList<Uri> urisToSync = new ArrayList<Uri>();
                if (SyncConfig.shouldUpdateDashboard(app)) {
                    if (SyncConfig.isIncomingEnabled(app, extras)) urisToSync.add(Content.ME_SOUND_STREAM.uri);
                    if (SyncConfig.isActivitySyncEnabled(app, extras)) urisToSync.add(Content.ME_ACTIVITIES.uri);
                }

                if (SyncConfig.shouldSyncCollections(app)) {
                    urisToSync.addAll(SyncContent.getCollectionsDueForSync(app, manual));
                }

                if (SyncConfig.shouldSync(app, Consts.PrefKeys.LAST_SYNC_CLEANUP, SyncConfig.CLEANUP_DELAY) || manual) {
                    urisToSync.add(Content.TRACK_CLEANUP.uri);
                    urisToSync.add(Content.USERS_CLEANUP.uri);
                }

                if (SyncConfig.shouldSync(app, Consts.PrefKeys.LAST_USER_SYNC, SyncConfig.CLEANUP_DELAY) || manual) {
                    urisToSync.add(Content.ME.uri);
                }

                if (SyncConfig.shouldSync(app, Consts.PrefKeys.LAST_SHORTCUT_SYNC, SyncConfig.SHORTCUT_DELAY) || manual) {
                    urisToSync.add(Content.ME_SHORTCUTS.uri);
                }

                if (!urisToSync.isEmpty()) {
                    syncIntent.putParcelableArrayListExtra(ApiSyncService.EXTRA_SYNC_URIS, urisToSync);
                }
                break;

              default: /* unknown push event, just don't do anything */
        }
        return syncIntent;
    }


    private static boolean handleFollowerEvent(SoundCloudApplication app, Bundle extras) {
        if (PreferenceManager.getDefaultSharedPreferences(app).getBoolean(Consts.PrefKeys.NOTIFICATIONS_FOLLOWERS, true)
                && extras.containsKey(SyncAdapterService.EXTRA_PUSH_EVENT_URI)) {
            final long id = PushEvent.getIdFromUri(extras.getString(SyncAdapterService.EXTRA_PUSH_EVENT_URI));
            if (id != -1) {
                User u = SoundCloudApplication.MODEL_MANAGER.getUser(id);
                if (u != null && !u.isStale()){
                    Message.showNewFollower(app, u);
                    return true;
                } else {
                    try {
                        HttpResponse resp = app.get(Request.to(Endpoints.USERS + "/" + id));
                        if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                            u = app.getMapper().readValue(resp.getEntity().getContent(), User.class);
                            SoundCloudApplication.MODEL_MANAGER.write(u);
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
                ContentStats.clear(app);
                clearActivities(app.getContentResolver());
                break;
            case REWIND_LAST_DAY:
                final long rewindTime = 24 * 3600000L; // 1d
                ContentStats.rewind(app, rewindTime);
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
}
