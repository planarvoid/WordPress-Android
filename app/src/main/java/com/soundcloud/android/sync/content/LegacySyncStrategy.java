package com.soundcloud.android.sync.content;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.model.CollectionHolder;
import com.soundcloud.android.storage.LocalCollectionDAO;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.android.utils.Log;

import android.content.ContentResolver;
import android.content.Context;

public abstract class LegacySyncStrategy implements SyncStrategy {
    public static final String TAG = ApiSyncService.LOG_TAG;

    protected final PublicApi api;
    protected final ContentResolver resolver;
    protected final Context context;
    protected final SyncStateManager syncStateManager;
    protected final AccountOperations accountOperations;

    protected LegacySyncStrategy(Context context, ContentResolver resolver) {
        this(context, resolver, PublicApi.getInstance(context), new SyncStateManager(resolver, new LocalCollectionDAO(resolver)),
                SoundCloudApplication.fromContext(context).getAccountOperations());
    }

    protected LegacySyncStrategy(Context context, ContentResolver resolver, AccountOperations accountOperations){
        this(context, resolver, PublicApi.getInstance(context), new SyncStateManager(resolver, new LocalCollectionDAO(resolver)), accountOperations);
    }

    protected LegacySyncStrategy(Context context, ContentResolver resolver, PublicApi api, SyncStateManager syncStateManager,
                                 AccountOperations accountOperations){
        this.context = context;
        this.api = api;
        this.resolver = resolver;
        this.syncStateManager = syncStateManager;
        this.accountOperations = accountOperations;
    }

    protected static void log(String message) {
        Log.d(TAG, message);
    }

    protected boolean isLoggedIn(){
        return accountOperations.isUserLoggedIn();
    }

    public static class IdHolder extends CollectionHolder<Long> {
    }

}
