package com.soundcloud.android.sync;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.storage.provider.ScContentProvider;
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

public class SyncInitiator {

    public final Action0 requestSystemSyncAction = new Action0() {
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
                        new ResultReceiverAdapter(subscriber, Content.ME_SOUND_STREAM.uri));
            }
        });
    }

    private void requestSoundStreamSync(String action, ResultReceiverAdapter resultReceiver) {
        context.startService(new Intent(context, ApiSyncService.class)
                .setAction(action)
                .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiver)
                .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                .setData(Content.ME_SOUND_STREAM.uri));
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
                requestSoundStreamBackfill(new ResultReceiverAdapter(subscriber, Content.ME_SOUND_STREAM.uri));
            }
        });
    }

    private void requestSoundStreamBackfill(ResultReceiverAdapter resultReceiver) {
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
                requestPlaylistSync(new ResultReceiverAdapter(subscriber, contentUri));
            }
        });
    }

    private void requestPlaylistSync(ResultReceiverAdapter resultReceiver) {
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
                requestTrackSync(new ResultReceiverAdapter(subscriber, contentUri));
            }
        });
    }

    private void requestTrackSync(ResultReceiverAdapter resultReceiver) {
        context.startService(new Intent(context, ApiSyncService.class)
                .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiver)
                .setData(resultReceiver.contentUri));
    }

}
