package com.soundcloud.android.sync;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.storage.provider.Content;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;

@Deprecated
public class LegacySyncInitiator {
    private final Action0 requestSystemSyncAction = new Action0() {
        @Override
        public void call() {
            requestSystemSync();
        }
    };

    private final Context context;
    private final AccountOperations accountOperations;
    private final SyncStateManager syncStateManager;

    @Inject
    public LegacySyncInitiator(Context context,
                               AccountOperations accountOperations,
                               SyncStateManager syncStateManager) {
        this.syncStateManager = syncStateManager;
        this.context = context.getApplicationContext();
        this.accountOperations = accountOperations;
    }

    public Action0 requestSystemSyncAction() {
        return requestSystemSyncAction;
    }

    public boolean pushFollowingsToApi() {
        final Account account = accountOperations.getSoundCloudAccount();
        if (account != null) {
            final Bundle extras = new Bundle();
            extras.putBoolean(SyncAdapterService.EXTRA_SYNC_PUSH, true);
            extras.putString(SyncAdapterService.EXTRA_SYNC_PUSH_URI, Content.ME_FOLLOWINGS.uri.toString());
            ContentResolver.requestSync(account, SyncConfig.AUTHORITY, extras);
            return true;
        } else {
            return false;
        }
    }

    public boolean requestSystemSync() {
        final Account soundCloudAccount = accountOperations.getSoundCloudAccount();
        if (soundCloudAccount != null) {
            ContentResolver.requestSync(soundCloudAccount, SyncConfig.AUTHORITY, new Bundle());
            return true;
        } else {
            return false;
        }
    }

    public Observable<Boolean> refreshFollowings() {
        final Uri uri = LegacySyncContent.MyFollowings.content.uri;
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                requestFollowingsSync(
                        ApiSyncService.ACTION_HARD_REFRESH,
                        new LegacyResultReceiverAdapter(subscriber, uri));
            }
        }).doOnNext(resetSyncMissesLegacy(uri));
    }

    public Observable<Boolean> refreshPosts() {
        final Uri uri = LegacySyncContent.MySounds.content.uri;
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                requestPostsSync(
                        ApiSyncService.ACTION_HARD_REFRESH,
                        new LegacyResultReceiverAdapter(subscriber, uri));
            }
        }).doOnNext(resetSyncMissesLegacy(uri));
    }

    public Observable<Boolean> refreshLikes() {
        final Uri uri = LegacySyncContent.MyLikes.content.uri;
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                requestLikesSync(
                        ApiSyncService.ACTION_HARD_REFRESH,
                        new LegacyResultReceiverAdapter(subscriber, uri));
            }
        }).doOnNext(resetSyncMissesLegacy(uri));
    }

    public Observable<Boolean> refreshCollections() {
        final Uri[] collectionUris = {LegacySyncContent.MyLikes.content.uri,
                LegacySyncContent.MyPlaylists.content.uri};
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                requestCollectionsSync(
                        ApiSyncService.ACTION_HARD_REFRESH,
                        new LegacyResultReceiverAdapter(subscriber, collectionUris));
            }
        }).doOnNext(resetSyncMissesLegacy(collectionUris));
    }

    public Observable<Boolean> refreshMyPlaylists() {
        final Uri uri = LegacySyncContent.MyPlaylists.content.uri;
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                requestPostedPlaylistsSync(
                        ApiSyncService.ACTION_HARD_REFRESH,
                        new LegacyResultReceiverAdapter(subscriber, uri));
            }
        }).doOnNext(resetSyncMissesLegacy(uri));
    }

    private void requestFollowingsSync(String action, LegacyResultReceiverAdapter resultReceiver) {
        context.startService(new Intent(context, ApiSyncService.class)
                                     .setAction(action)
                                     .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiver)
                                     .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                                     .setData(Content.ME_FOLLOWING.uri));
    }

    private void requestPostsSync(String action, LegacyResultReceiverAdapter resultReceiver) {
        context.startService(new Intent(context, ApiSyncService.class)
                                     .setAction(action)
                                     .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiver)
                                     .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                                     .setData(Content.ME_SOUNDS.uri));
    }

    private void requestLikesSync(String action, LegacyResultReceiverAdapter resultReceiver) {
        context.startService(new Intent(context, ApiSyncService.class)
                                     .setAction(action)
                                     .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiver)
                                     .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                                     .setData(Content.ME_LIKES.uri));
    }

    private void requestCollectionsSync(String action, LegacyResultReceiverAdapter resultReceiver) {
        ArrayList<Uri> urisToSync = new ArrayList<>(Arrays.asList(LegacySyncContent.MyLikes.content.uri,
                                                                  LegacySyncContent.MyPlaylists.content.uri));

        context.startService(new Intent(context, ApiSyncService.class)
                                     .setAction(action)
                                     .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiver)
                                     .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                                     .putParcelableArrayListExtra(ApiSyncService.EXTRA_SYNC_URIS, urisToSync));
    }

    private void requestPostedPlaylistsSync(String action, LegacyResultReceiverAdapter resultReceiver) {
        context.startService(new Intent(context, ApiSyncService.class)
                                     .setAction(action)
                                     .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiver)
                                     .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                                     .setData(Content.ME_PLAYLISTS.uri));
    }

    public Observable<SyncJobResult> syncTrackLikes() {
        return legacyRequestSyncObservable(LegacySyncActions.SYNC_TRACK_LIKES)
                .doOnNext(resetSyncMisses(LegacySyncContent.MyLikes.content.uri));
    }

    public Observable<SyncJobResult> syncPlaylistLikes() {
        return legacyRequestSyncObservable(LegacySyncActions.SYNC_PLAYLIST_LIKES)
                .doOnNext(resetSyncMisses(LegacySyncContent.MyLikes.content.uri));
    }

    private Observable<SyncJobResult> requestSyncResultObservable(final Intent intent) {
        return Observable
                .create(new Observable.OnSubscribe<SyncJobResult>() {
                    @Override
                    public void call(Subscriber<? super SyncJobResult> subscriber) {
                        final ResultReceiverAdapter receiverAdapter = new ResultReceiverAdapter(subscriber,
                                                                                                Looper.getMainLooper());
                        context.startService(intent.putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, receiverAdapter));
                    }
                });
    }

    private Intent createIntent(String action) {
        return new Intent(context, ApiSyncService.class).setAction(action);
    }

    @Deprecated
    private Observable<SyncJobResult> legacyRequestSyncObservable(final String action) {
        return requestSyncResultObservable(createIntent(action));
    }

    public void syncLocalPlaylists() {
        context.startService(getSyncLocalPlaylistsIntent());
    }

    private Intent getSyncLocalPlaylistsIntent() {
        return new Intent(context, ApiSyncService.class)
                .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                .setData(Content.ME_PLAYLISTS.uri);
    }

    private Action1<Boolean> resetSyncMissesLegacy(final Uri... uris) {
        return new Action1<Boolean>() {
            @Override
            public void call(Boolean changed) {
                if (changed) {
                    for (Uri uri : uris) {
                        fireAndForget(syncStateManager.resetSyncMissesAsync(uri));
                    }

                }
            }
        };
    }

    private Action1<SyncJobResult> resetSyncMisses(final Uri uri) {
        return new Action1<SyncJobResult>() {
            @Override
            public void call(SyncJobResult result) {
                if (result.wasChanged()) {
                    fireAndForget(syncStateManager.resetSyncMissesAsync(uri));
                }
            }
        };
    }

}
