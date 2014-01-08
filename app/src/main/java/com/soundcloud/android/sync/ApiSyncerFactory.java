package com.soundcloud.android.sync;

import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.content.SyncStrategy;
import com.soundcloud.android.sync.content.UserAssociationSyncer;

import android.content.Context;
import android.net.Uri;

public class ApiSyncerFactory {
    public static final String TAG = ApiSyncService.LOG_TAG;

    public SyncStrategy forContentUri(Context context, Uri contentUri) {
        switch (Content.match(contentUri)) {
            case ME_FOLLOWINGS:
            case ME_FOLLOWERS:
                return new UserAssociationSyncer(context);

            default:
                return new ApiSyncer(context, context.getContentResolver());
        }
    }

}
