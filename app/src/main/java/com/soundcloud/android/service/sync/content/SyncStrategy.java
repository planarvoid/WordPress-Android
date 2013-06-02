package com.soundcloud.android.service.sync.content;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.api.OldCloudAPI;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.service.sync.ApiSyncResult;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.service.sync.SyncStateManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;

public abstract class SyncStrategy {
    public static final String TAG = ApiSyncService.LOG_TAG;

    protected final AndroidCloudAPI mApi;
    protected final ContentResolver mResolver;
    protected final Context mContext;
    protected final SyncStateManager mSyncStateManager;

    protected SyncStrategy(Context context, ContentResolver resolver) {
        mApi = new OldCloudAPI(context);
        mResolver = resolver;
        mContext = context;
        mSyncStateManager = new SyncStateManager();
    }

    @NotNull
    public abstract ApiSyncResult syncContent(@NotNull Uri uri, @Nullable String action) throws IOException;

    protected static void log(String message) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, message);
            }
        }

        public static class IdHolder extends CollectionHolder<Long> {
        }

}
