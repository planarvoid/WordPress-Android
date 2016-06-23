package com.soundcloud.android.sync;

import com.soundcloud.android.accounts.AccountOperations;
import rx.Observable;
import rx.Subscriber;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;

import javax.inject.Inject;

public class SyncInitiator {

    private final Context context;
    private final AccountOperations accountOperations;

    @Inject
    SyncInitiator(Context context, AccountOperations accountOperations) {
        this.context = context;
        this.accountOperations = accountOperations;
    }

    public Observable<SyncJobResult> sync(Syncable syncId) {
        final Intent intent = createIntent(syncId);

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

    public boolean requestSystemSync() {
        final Account soundCloudAccount = accountOperations.getSoundCloudAccount();
        if (soundCloudAccount != null) {
            ContentResolver.requestSync(soundCloudAccount, SyncConfig.AUTHORITY, new Bundle());
            return true;
        } else {
            return false;
        }
    }

    private Intent createIntent(Syncable syncId) {
        final Intent intent = new Intent(context, ApiSyncService.class);
        intent.putExtra(ApiSyncService.EXTRA_SYNCABLE, syncId);
        intent.putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true);
        return intent;
    }
}
