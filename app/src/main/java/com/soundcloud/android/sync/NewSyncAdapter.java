package com.soundcloud.android.sync;

import com.soundcloud.android.utils.Log;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.os.Looper;

import javax.inject.Inject;

public class NewSyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = "NewSyncAdapter";
    private Looper looper;

    private BackgroundSyncerFactory backgroundSyncerFactory;
    private BackgroundSyncResultReceiverFactory receiverFactory;

    @Inject
    public NewSyncAdapter(Context context,
                          BackgroundSyncerFactory backgroundSyncerFactory,
                          BackgroundSyncResultReceiverFactory receiverFactory) {
        super(context, false);
        this.backgroundSyncerFactory = backgroundSyncerFactory;
        this.receiverFactory = receiverFactory;
    }

    /**
     * Called by the framework to indicate a sync request.
     */
    @Override
    public void onPerformSync(Account account,
                              Bundle extras,
                              String authority,
                              ContentProviderClient provider,
                              SyncResult syncResult) {

        final boolean manual = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, false);
        // delegate to the ApiSyncService, use a looper + ResultReceiver to wait for the result
        Looper.prepare();
        looper = Looper.myLooper();

        final BackgroundSyncResultReceiver resultReceiver = receiverFactory.create(new SyncCleanupRunnable(looper),
                syncResult);
        final BackgroundSyncer backgroundSyncer = backgroundSyncerFactory.create(getContext(), resultReceiver);

        switch (backgroundSyncer.sync(manual)) {
            case SYNCING:
                Looper.loop(); // wait for results to come in
                return;
            case UNAUTHORIZED:
                syncResult.stats.numAuthExceptions++;
                return;
            default:
        }
    }

    @Override
    public void onSyncCanceled() {
        if (looper != null) {
            looper.quit();
        } // make sure sync thread exits
        super.onSyncCanceled();
    }


    public static class SyncCleanupRunnable implements Runnable {
        private final Looper looper;

        private SyncCleanupRunnable(Looper looper) {
            this.looper = looper;
        }

        @Override
        public void run() {
            Log.d(TAG, "sync finished");
            looper.quit();
        }
    }

}
