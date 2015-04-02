package com.soundcloud.android.sync;

import com.soundcloud.android.Consts;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.PublicApiWrapper;
import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.ActivitiesStorage;
import com.soundcloud.android.playlists.PlaylistStorage;
import com.soundcloud.android.storage.UserAssociationStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.storage.provider.ScContentProvider;
import com.soundcloud.android.sync.likes.MyLikesStateProvider;
import com.soundcloud.android.utils.DebugUtils;
import com.soundcloud.android.utils.Log;
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

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Sync service - delegates to {@link ApiSyncService} for the actual syncing. This class is responsible for the setup
 * and handling of the background syncing.
 */
public class SyncAdapterService extends Service {

    /* package */  static final String TAG = SyncAdapterService.class.getSimpleName();
    private static final boolean DEBUG_CANCEL = Boolean.valueOf(System.getProperty("syncadapter.debug.cancel", null));
    public static final String SYNC_FINISHED = SyncAdapterService.class.getName() + ".syncFinished";

    public static final String EXTRA_SYNC_PUSH = "syncPush";
    public static final String EXTRA_SYNC_PUSH_URI = "syncPushUri";

    public static final int CLEAR_ALL = 1;
    public static final int REWIND_LAST_DAY = 2;

    private AbstractThreadedSyncAdapter syncAdapter;

    @Inject AccountOperations accountOperations;
    @Inject SyncServiceResultReceiver.Factory syncServiceResultReceiverFactory;
    @Inject MyLikesStateProvider myLikesStateProvider;
    @Inject PlaylistStorage playlistStorage;

    public SyncAdapterService() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        syncAdapter = new AbstractThreadedSyncAdapter(this, false) {
            private Looper looper;

            /**
             * Called by the framework to indicate a sync request.
             */
            @Override
            public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
                PublicApiWrapper.setBackgroundMode(true);

                // delegate to the ApiSyncService, use a looper + ResultReceiver to wait for the result
                Looper.prepare();
                looper = Looper.myLooper();
                if (performSync((SoundCloudApplication) getApplication(), extras, syncResult, accountOperations.getSoundCloudToken(), new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "sync finished");
                        sendBroadcast(new Intent(SYNC_FINISHED));

                        looper.quit();
                    }
                }, syncServiceResultReceiverFactory, myLikesStateProvider, playlistStorage)) {
                    Looper.loop(); // wait for results to come in
                }
                PublicApiWrapper.setBackgroundMode(false);
            }

            @Override
            public void onSyncCanceled() {
                if (DEBUG_CANCEL) {
                    Log.d(TAG, "sync canceled, dumping stack");
                    DebugUtils.dumpStack(getContext());
                    new Thread() {
                        @Override
                        public void run() {
                            DebugUtils.dumpLog(getContext());
                        }
                    }.start();
                }

                if (looper != null) {
                    looper.quit();
                } // make sure sync thread exits
                super.onSyncCanceled();
            }
        };
    }

    @Override
    public IBinder onBind(Intent intent) {
        return syncAdapter.getSyncAdapterBinder();
    }

    /**
     * Perform sync, already called aon a background thread.
     *
     * @return true if a sync has been started.
     */
    /* package */
    static boolean performSync(final SoundCloudApplication app,
                               Bundle extras,
                               final SyncResult syncResult,
                               @Nullable final Token token,
                               @Nullable final Runnable onResult,
                               final SyncServiceResultReceiver.Factory syncServiceResultReceiverFactory,
                               MyLikesStateProvider myLikesStateProvider, PlaylistStorage playlistStorage) {
        if (token == null || !token.valid()) {
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

        final SyncStateManager syncStateManager = new SyncStateManager(app);

        final Intent syncIntent = getSyncIntent(app, extras, syncStateManager, playlistStorage, myLikesStateProvider);
        if (syncIntent.getData() != null || syncIntent.hasExtra(ApiSyncService.EXTRA_SYNC_URIS)) {
            // ServiceResultReceiver does most of the work
            final SyncServiceResultReceiver syncServiceResultReceiver = syncServiceResultReceiverFactory.create(syncResult, extras, new SyncServiceResultReceiver.OnResultListener() {
                @Override
                public void onResultReceived() {
                    // make sure the looper quits in any case - otherwise sync just hangs, holding wakelock
                    if (onResult != null) {
                        onResult.run();
                    }
                }
            });
            syncIntent.putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, syncServiceResultReceiver);
            app.startService(syncIntent);
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings("PMD.ModifiedCyclomaticComplexity")
    private static Intent getSyncIntent(SoundCloudApplication app, Bundle extras,
                                        SyncStateManager syncStateManager,
                                        PlaylistStorage playlistStorage,
                                        MyLikesStateProvider myLikesStateProvider) {


        final Intent syncIntent = new Intent(app, ApiSyncService.class);
        if (extras.getBoolean(EXTRA_SYNC_PUSH)) {
            syncIntent.setData(Uri.parse(extras.getString(EXTRA_SYNC_PUSH_URI)));
            syncIntent.setAction(ApiSyncService.ACTION_PUSH);
            return syncIntent;
        }


        final boolean manual = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
        final ArrayList<Uri> urisToSync = new ArrayList<>();
        if (SyncConfig.shouldUpdateDashboard(app)) {
            if (SyncConfig.isIncomingEnabled(app) &&
                    (manual || syncStateManager.isContentDueForSync(SyncContent.MySoundStream))) {
                urisToSync.add(Content.ME_SOUND_STREAM.uri);
            }
            if (SyncConfig.isActivitySyncEnabled(app, extras) &&
                    (manual || syncStateManager.isContentDueForSync(SyncContent.MyActivities))) {
                urisToSync.add(Content.ME_ACTIVITIES.uri);
            }
        }

        if (manual || SyncConfig.shouldSyncCollections(app)) {
            final List<Uri> dueForSync = syncStateManager.getCollectionsDueForSync(SyncContent.NON_ACTIVITIES, manual);
            Log.d(TAG, "collection due for sync:" + dueForSync);
            urisToSync.addAll(dueForSync);
        } else {
            Log.d(TAG, "skipping collection sync, no wifi");
        }

        // see if there are any local playlists that need to be pushed
        if (new UserAssociationStorage(app).hasFollowingsNeedingSync()) {
            urisToSync.add(Content.ME_FOLLOWINGS.uri);
        }

        if (myLikesStateProvider.hasLocalChanges()){
            urisToSync.add(Content.ME_LIKES.uri);
        }

        // see if there are any local playlists that need to be pushed
        if (playlistStorage.hasLocalPlaylists()) {
            urisToSync.add(Content.ME_PLAYLISTS.uri);
        }

        // see if there are any playlists with un-pushed track changes
        final Set<Urn> playlistsDueForSync = playlistStorage.getPlaylistsDueForSync();
        if (playlistsDueForSync != null) {
            for (Urn playlistDueForSync : playlistsDueForSync){
                urisToSync.add(Content.PLAYLIST.forId(playlistDueForSync.getNumericId()));
            }
        }

        if (SyncConfig.shouldSync(app, Consts.PrefKeys.LAST_USER_SYNC, SyncConfig.USER_STALE_TIME) || manual) {
            urisToSync.add(Content.ME.uri);
        }
        if (!urisToSync.isEmpty()) {
            syncIntent.putParcelableArrayListExtra(ApiSyncService.EXTRA_SYNC_URIS, urisToSync);
        }

        return syncIntent;
    }


    // only used for debugging
    public static void requestNewSync(SoundCloudApplication app, int clearMode) {
        switch (clearMode) {
            case CLEAR_ALL:
                ContentStats.clear(app);
                clearActivities();
                break;
            case REWIND_LAST_DAY:
                final long rewindTime = 24 * 3600000L; // 1d
                ContentStats.rewind(app, rewindTime);
                clearActivities();
                break;
            default:
        }

        final Bundle extras = new Bundle();
        extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(app.getAccountOperations().getSoundCloudAccount(), ScContentProvider.AUTHORITY, extras);
    }

    private static void clearActivities() {
        // drop all activities before re-sync
        int deleted = new ActivitiesStorage().clear(null);
        Log.d(TAG, "deleted " + deleted + " activities");
    }
}
