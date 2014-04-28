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
import android.os.ResultReceiver;

import javax.inject.Inject;

public class SyncInitiator {

    public final Action0 requestSystemSyncAction = new Action0() {
        @Override
        public void call() {
            requestSystemSync();
        }
    };

    private final Context mContext;
    private final AccountOperations mAccountOperations;

    @Inject
    public SyncInitiator(Context context, AccountOperations accountOperations) {
        mContext = context.getApplicationContext();
        mAccountOperations = accountOperations;
    }

    public boolean pushFollowingsToApi() {
        final Account account = mAccountOperations.getSoundCloudAccount();
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
        final Account soundCloudAccount = mAccountOperations.getSoundCloudAccount();
        if (soundCloudAccount != null) {
            ContentResolver.requestSync(soundCloudAccount, ScContentProvider.AUTHORITY, new Bundle());
            return true;
        } else {
            return false;
        }
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
        mContext.startService(new Intent(mContext, ApiSyncService.class)
                .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiver)
                .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                .setData(resultReceiver.mContentUri)
                .setAction(ApiSyncService.ACTION_APPEND));
    }

    public void syncLocalPlaylists() {
        final Intent intent = new Intent(mContext, ApiSyncService.class)
                .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, (ResultReceiver) null)
                .setData(Content.ME_PLAYLISTS.uri);
        mContext.startService(intent);
    }

    public Observable<Boolean> syncPlaylist(final Urn playlistUrn) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                final Uri contentUri = Content.PLAYLIST.forId(playlistUrn.numericId);
                requestPlaylistSync(new ResultReceiverAdapter(subscriber, contentUri));
            }
        });
    }

    private void requestPlaylistSync(ResultReceiverAdapter resultReceiver) {
        mContext.startService(new Intent(mContext, ApiSyncService.class)
                .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiver)
                .setData(resultReceiver.mContentUri));
    }

    public Observable<Boolean> syncTrack(final Urn trackUrn) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                final Uri contentUri = Content.TRACKS.forId(trackUrn.numericId);
                requestTrackSync(new ResultReceiverAdapter(subscriber, contentUri));
            }
        });
    }

    private void requestTrackSync(ResultReceiverAdapter resultReceiver) {
        mContext.startService(new Intent(mContext, ApiSyncService.class)
                .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiver)
                .setData(resultReceiver.mContentUri));
    }

}
