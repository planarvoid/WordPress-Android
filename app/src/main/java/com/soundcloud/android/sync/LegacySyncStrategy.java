package com.soundcloud.android.sync;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.utils.Log;

import android.content.Context;

public abstract class LegacySyncStrategy implements SyncStrategy {
    public static final String TAG = ApiSyncService.LOG_TAG;

    protected final PublicApi api;
    protected final Context context;
    protected final AccountOperations accountOperations;

    protected LegacySyncStrategy(Context context, PublicApi api,
                                 AccountOperations accountOperations){
        this.context = context;
        this.api = api;
        this.accountOperations = accountOperations;
    }

    protected static void log(String message) {
        Log.d(TAG, message);
    }

    protected boolean isLoggedIn(){
        return accountOperations.isUserLoggedIn();
    }
}
