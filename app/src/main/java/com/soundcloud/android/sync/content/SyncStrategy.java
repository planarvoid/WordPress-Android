package com.soundcloud.android.sync.content;

import com.soundcloud.android.SoundCloudApplication;
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

    protected final PublicCloudAPI mApi;
    protected final ContentResolver mResolver;
    protected final Context mContext;
    protected final SyncStateManager mSyncStateManager;

    protected SyncStrategy(Context context, ContentResolver resolver) {
        mApi = new PublicApi(context);
        mResolver = resolver;
        mContext = context;
        mSyncStateManager = new SyncStateManager(resolver, new LocalCollectionDAO(resolver));
    }

    @NotNull
    public abstract ApiSyncResult syncContent(@NotNull Uri uri, @Nullable String action) throws IOException;

    protected static void log(String message) {
        Log.d(TAG, message);
    }

    protected boolean isLoggedIn(){
        return SoundCloudApplication.getUserId() > 0;
    }

    public static class IdHolder extends CollectionHolder<Long> {
    }

}
