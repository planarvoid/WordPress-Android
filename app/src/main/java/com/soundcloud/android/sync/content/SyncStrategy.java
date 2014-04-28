package com.soundcloud.android.sync.content;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.PublicApi;
import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.storage.LocalCollectionDAO;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.android.utils.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import java.io.IOException;

public abstract class SyncStrategy {
    public static final String TAG = ApiSyncService.LOG_TAG;

    protected final PublicCloudAPI api;
    protected final ContentResolver resolver;
    protected final Context context;
    protected final SyncStateManager syncStateManager;
    private final AccountOperations accountOperations;

    protected SyncStrategy(Context context, ContentResolver resolver) {
        this(context, resolver, new PublicApi(context), new SyncStateManager(resolver, new LocalCollectionDAO(resolver)), new AccountOperations(context));
    }

    protected SyncStrategy(Context context, ContentResolver resolver, AccountOperations accountOperations){
        this(context, resolver, new PublicApi(context), new SyncStateManager(resolver, new LocalCollectionDAO(resolver)), accountOperations);
    }

    protected SyncStrategy(Context context, ContentResolver resolver, PublicCloudAPI api, SyncStateManager syncStateManager,
                           AccountOperations accountOperations){
        this.context = context;
        this.api = api;
        this.resolver = resolver;
        this.syncStateManager = syncStateManager;
        this.accountOperations = accountOperations;
    }

    @NotNull
    public abstract ApiSyncResult syncContent(@NotNull Uri uri, @Nullable String action) throws IOException;

    protected static void log(String message) {
        Log.d(TAG, message);
    }

    protected boolean isLoggedIn(){
        return accountOperations.soundCloudAccountExists();
    }

    public static class IdHolder extends CollectionHolder<Long> {
    }

}
