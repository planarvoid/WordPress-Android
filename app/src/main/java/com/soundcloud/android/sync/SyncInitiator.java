package com.soundcloud.android.sync;

import com.google.common.collect.Lists;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.storage.provider.ScContentProvider;
import com.soundcloud.android.utils.CollectionUtils;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;

import javax.inject.Inject;
import java.util.List;

public class SyncInitiator {

    private final Action0 requestSystemSyncAction = new Action0() {
        @Override
        public void call() {
            requestSystemSync();
        }
    };

    private final Context context;
    private final AccountOperations accountOperations;
    private final SyncStateManager syncStateManager;

    private static final Func1<Boolean, SyncResult> LEGACY_RESULT_TO_SYNC_RESULT = new Func1<Boolean, SyncResult>() {
        @Override
        public SyncResult call(Boolean resultedInChange) {
            return SyncResult.success(SyncActions.SYNC_PLAYLIST, resultedInChange);
        }
    };

    @Inject
    public SyncInitiator(Context context, AccountOperations accountOperations, SyncStateManager syncStateManager) {
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
            ContentResolver.requestSync(account, ScContentProvider.AUTHORITY, extras);
            return true;
        } else {
            return false;
        }
    }

    public boolean requestSystemSync() {
        final Account soundCloudAccount = accountOperations.getSoundCloudAccount();
        if (soundCloudAccount != null) {
            ContentResolver.requestSync(soundCloudAccount, ScContentProvider.AUTHORITY, new Bundle());
            return true;
        } else {
            return false;
        }
    }

    public Observable<Boolean> initialSoundStream() {
        final Uri uri = SyncContent.MySoundStream.content.uri;
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                requestSoundStreamSync(null, new LegacyResultReceiverAdapter(subscriber, uri));
            }
        }).doOnNext(resetSyncMissesLegacy(uri));
    }

    public Observable<Boolean> refreshSoundStream() {
        final Uri uri = SyncContent.MySoundStream.content.uri;
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                requestSoundStreamSync(
                        ApiSyncService.ACTION_HARD_REFRESH,
                        new LegacyResultReceiverAdapter(subscriber, uri));
            }
        }).doOnNext(resetSyncMissesLegacy(uri));
    }

    public Observable<SyncResult> syncUser(final Urn userUrn) {
        return Observable.create(new Observable.OnSubscribe<SyncResult>() {
            @Override
            public void call(Subscriber<? super SyncResult> subscriber) {
                ResultReceiverAdapter resultReceiver = new ResultReceiverAdapter(subscriber, Looper.getMainLooper());
                context.startService(new Intent(context, ApiSyncService.class)
                        .setAction(SyncActions.SYNC_USERS)
                        .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                        .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiver)
                        .putParcelableArrayListExtra(SyncExtras.URNS, Lists.newArrayList(userUrn)));
            }
        });
    }

    private void requestSoundStreamSync(String action, LegacyResultReceiverAdapter resultReceiver) {
        context.startService(new Intent(context, ApiSyncService.class)
                .setAction(action)
                .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiver)
                .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                .setData(Content.ME_SOUND_STREAM.uri));
    }

    public Observable<Boolean> refreshPostedPlaylists() {
        final Uri uri = SyncContent.MyPlaylists.content.uri;
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                requestPostedPlaylistsSync(
                        ApiSyncService.ACTION_HARD_REFRESH,
                        new LegacyResultReceiverAdapter(subscriber, uri));
            }
        }).doOnNext(resetSyncMissesLegacy(uri));
    }

    private void requestPostedPlaylistsSync(String action, LegacyResultReceiverAdapter resultReceiver) {
        context.startService(new Intent(context, ApiSyncService.class)
                .setAction(action)
                .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiver)
                .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                .setData(Content.ME_PLAYLISTS.uri));
    }

    public Observable<SyncResult> syncTrackLikes() {
        return requestSyncObservable(SyncActions.SYNC_TRACK_LIKES)
                .doOnNext(resetSyncMisses(SyncContent.MyLikes.content.uri));
    }

    public Observable<SyncResult> syncPlaylistLikes() {
        return requestSyncObservable(SyncActions.SYNC_PLAYLIST_LIKES)
                .doOnNext(resetSyncMisses(SyncContent.MyLikes.content.uri));
    }

    public Observable<SyncResult> syncPlaylistPosts() {
        return requestSyncObservable(SyncActions.SYNC_PLAYLISTS);
    }

    public void requestTracksSync(List<PropertySet> tracks) {
        context.startService(new Intent(context, ApiSyncService.class)
                .setAction(SyncActions.SYNC_TRACKS)
                .putParcelableArrayListExtra(SyncExtras.URNS, CollectionUtils.extractUrnsFromEntities(tracks)));
    }

    public void requestPlaylistSync(List<PropertySet> playlists) {
        context.startService(new Intent(context, ApiSyncService.class)
                .setAction(SyncActions.SYNC_PLAYLISTS)
                .putParcelableArrayListExtra(SyncExtras.URNS, CollectionUtils.extractUrnsFromEntities(playlists)));
    }

    private Observable<SyncResult> requestSyncObservable(final String syncAction) {
        return Observable
                .create(new Observable.OnSubscribe<SyncResult>() {
                    @Override
                    public void call(Subscriber<? super SyncResult> subscriber) {
                        requestSync(syncAction, new ResultReceiverAdapter(subscriber, Looper.getMainLooper()));
                    }
                });
    }

    private Observable<SyncResult> requestSyncObservable(final String syncAction, final Urn urn) {
        return Observable
                .create(new Observable.OnSubscribe<SyncResult>() {
                    @Override
                    public void call(Subscriber<? super SyncResult> subscriber) {
                        requestSync(syncAction, urn, new ResultReceiverAdapter(subscriber, Looper.getMainLooper()));
                    }
                });
    }

    private void requestSync(String action, ResultReceiverAdapter resultReceiver) {
        context.startService(new Intent(context, ApiSyncService.class)
                .setAction(action)
                .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiver));
    }

    private void requestSync(String action, Urn urn, ResultReceiverAdapter resultReceiver) {
        context.startService(new Intent(context, ApiSyncService.class)
                .setAction(action)
                .putExtra(SyncExtras.URN, urn)
                .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiver));
    }

    /**
     * Triggers a backfill sync for the sound stream.
     * <p/>
     * This is a sync that will retrieve N more sound stream items /older/ than the oldest locally
     * available item. Used to lazily pull in more items when paging in the stream reverse chronologically.
     */
    public Observable<Boolean> backfillSoundStream() {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                requestSoundStreamBackfill(new LegacyResultReceiverAdapter(subscriber, Content.ME_SOUND_STREAM.uri));
            }
        });
    }

    private void requestSoundStreamBackfill(LegacyResultReceiverAdapter resultReceiver) {
        context.startService(new Intent(context, ApiSyncService.class)
                .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiver)
                .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                .setData(resultReceiver.contentUri)
                .setAction(ApiSyncService.ACTION_APPEND));
    }

    public void syncLocalPlaylists() {
        context.startService(getSyncLocalPlaylistsIntent());
    }

    private Intent getSyncLocalPlaylistsIntent() {
        return new Intent(context, ApiSyncService.class)
                .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                .setData(Content.ME_PLAYLISTS.uri);
    }

    public Observable<SyncResult> syncPlaylist(final Urn playlistUrn) {
        if (playlistUrn.getNumericId() < 0) {
            return Observable.create(new Observable.OnSubscribe<Boolean>() {
                        @Override
                        public void call(Subscriber<? super Boolean> subscriber) {
                            final Intent intent = getSyncLocalPlaylistsIntent().putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER,
                                    new LegacyResultReceiverAdapter(subscriber, Content.ME_PLAYLISTS.uri));
                            context.startService(intent);
                        }
                    }).map(LEGACY_RESULT_TO_SYNC_RESULT);
        } else {
            return requestSyncObservable(SyncActions.SYNC_PLAYLIST, playlistUrn);
        }
    }

    public Observable<Boolean> syncTrack(final Urn trackUrn) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                final Uri contentUri = Content.TRACKS.forId(trackUrn.getNumericId());
                requestTrackSync(new LegacyResultReceiverAdapter(subscriber, contentUri));
            }
        });
    }

    private void requestTrackSync(LegacyResultReceiverAdapter resultReceiver) {
        context.startService(new Intent(context, ApiSyncService.class)
                .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiver)
                .setData(resultReceiver.contentUri));
    }

    private Action1<Boolean> resetSyncMissesLegacy(final Uri uri) {
        return new Action1<Boolean>() {
            @Override
            public void call(Boolean changed) {
                if (changed){
                    syncStateManager.resetSyncMisses(uri);
                }
            }
        };
    }

    private Action1<SyncResult> resetSyncMisses(final Uri uri) {
        return new Action1<SyncResult>() {
            @Override
            public void call(SyncResult result) {
                if (result.wasChanged()){
                    syncStateManager.resetSyncMisses(uri);
                }
            }
        };
    }

}
