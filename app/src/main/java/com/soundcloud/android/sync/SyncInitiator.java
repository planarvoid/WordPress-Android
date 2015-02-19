package com.soundcloud.android.sync;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.storage.provider.ScContentProvider;
import com.soundcloud.android.utils.CollectionUtils;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

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

    @Inject
    public SyncInitiator(Context context, AccountOperations accountOperations) {
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

    public Observable<Boolean> refreshSoundStream() {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                requestSoundStreamSync(
                        ApiSyncService.ACTION_HARD_REFRESH,
                        new LegacyResultReceiverAdapter(subscriber, Content.ME_SOUND_STREAM.uri));
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
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                requestPostedPlaylistsSync(
                        ApiSyncService.ACTION_HARD_REFRESH,
                        new LegacyResultReceiverAdapter(subscriber, Content.ME_PLAYLISTS.uri));
            }
        });
    }

    private void requestPostedPlaylistsSync(String action, LegacyResultReceiverAdapter resultReceiver) {
        context.startService(new Intent(context, ApiSyncService.class)
                .setAction(action)
                .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiver)
                .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                .setData(Content.ME_PLAYLISTS.uri));
    }

    public Observable<SyncResult> syncTrackLikes() {
        return requestSyncObservable(SyncActions.SYNC_TRACK_LIKES);
    }

    public Observable<SyncResult> syncPlaylistLikes() {
        return requestSyncObservable(SyncActions.SYNC_PLAYLIST_LIKES);
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
        return Observable.create(new Observable.OnSubscribe<SyncResult>() {
            @Override
            public void call(Subscriber<? super SyncResult> subscriber) {
                requestSync(syncAction, new ResultReceiverAdapter(subscriber));
            }
        });
    }

    private void requestSync(String action, ResultReceiverAdapter resultReceiver) {
        context.startService(new Intent(context, ApiSyncService.class)
                .setAction(action)
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
        context.startService(new Intent(context, ApiSyncService.class)
                .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                .setData(Content.ME_PLAYLISTS.uri));
    }

    public Observable<Boolean> syncPlaylist(final Urn playlistUrn) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                final Uri contentUri = Content.PLAYLIST.forId(playlistUrn.getNumericId());
                requestPlaylistSync(new LegacyResultReceiverAdapter(subscriber, contentUri));
            }
        });
    }

    private void requestPlaylistSync(LegacyResultReceiverAdapter resultReceiver) {
        context.startService(new Intent(context, ApiSyncService.class)
                .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiver)
                .setData(resultReceiver.contentUri));
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

}
