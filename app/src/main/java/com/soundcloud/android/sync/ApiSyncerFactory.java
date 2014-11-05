package com.soundcloud.android.sync;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.content.PlaylistSyncer;
import com.soundcloud.android.sync.content.SyncStrategy;
import com.soundcloud.android.sync.content.UserAssociationSyncer;

import android.content.Context;
import android.net.Uri;

import javax.inject.Inject;
import javax.inject.Provider;

public class ApiSyncerFactory {

    private final Provider<FollowingOperations> followingOpsProvider;
    private final Provider<AccountOperations> accountOpsProvider;

    @Inject
    public ApiSyncerFactory(Provider<FollowingOperations> followingOpsProvider, Provider<AccountOperations> accountOpsProvider) {
        this.followingOpsProvider = followingOpsProvider;
        this.accountOpsProvider = accountOpsProvider;
    }

    public static final String TAG = ApiSyncService.LOG_TAG;

    public SyncStrategy forContentUri(Context context, Uri contentUri) {
        switch (Content.match(contentUri)) {
            case ME_FOLLOWINGS:
            case ME_FOLLOWERS:
                return new UserAssociationSyncer(context, accountOpsProvider.get(), followingOpsProvider.get());

            case ME_PLAYLISTS:
            case PLAYLIST:
                return new PlaylistSyncer(context, context.getContentResolver());

            default:
                return new ApiSyncer(context, context.getContentResolver());
        }
    }

}
