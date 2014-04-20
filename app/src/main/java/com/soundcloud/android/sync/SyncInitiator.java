package com.soundcloud.android.sync;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.storage.provider.ScContentProvider;
import com.soundcloud.android.utils.Log;
import org.jetbrains.annotations.Nullable;
import rx.Subscriber;
import rx.functions.Action0;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

    public void syncLocalPlaylists() {
        syncLocalPlaylists(null);
    }

    public void syncLocalPlaylists(@Nullable ResultReceiver resultReceiver) {
        final Intent intent = new Intent(mContext, ApiSyncService.class)
                .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                .setData(Content.ME_PLAYLISTS.uri);

        if (resultReceiver != null) {
            intent.putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiver);
        }
        mContext.startService(intent);
    }

    public void syncContentUri(Uri contentUri, ResultReceiver resultReceiver) {
        mContext.startService(new Intent(mContext, ApiSyncService.class)
                .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                .putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, resultReceiver)
                .setData(contentUri));
    }

    public static class ResultReceiverAdapter<T> extends ResultReceiver {

        private final Subscriber<? super T> mSubscriber;
        @Nullable
        private final T mOptionalResult;

        public ResultReceiverAdapter(final Subscriber<? super T> subscriber, @Nullable final T optionalResult) {
            super(new Handler(Looper.getMainLooper()));
            mSubscriber = subscriber;
            mOptionalResult = optionalResult;
        }

        @Override
        public void onReceiveResult(int resultCode, Bundle resultData) {
            switch (resultCode) {
                case ApiSyncService.STATUS_SYNC_FINISHED:
                    mSubscriber.onNext(mOptionalResult);
                    mSubscriber.onCompleted();
                    break;
                case ApiSyncService.STATUS_SYNC_ERROR:
                    mSubscriber.onError(new SyncFailedException(resultData));
                    break;
                default:
                    throw new IllegalStateException("Unexpected sync state: " + resultCode);
            }
        }
    }

    public static class SyncFailedException extends Exception {
        public SyncFailedException(Bundle resultData) {
            super("Sync failed with result " + resultData.getParcelable(ApiSyncService.EXTRA_SYNC_RESULT));
        }
    }
}
