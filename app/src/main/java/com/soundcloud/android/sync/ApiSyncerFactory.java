package com.soundcloud.android.sync;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.content.SyncStrategy;
import com.soundcloud.android.sync.content.PlaylistSyncer;
import com.soundcloud.android.sync.content.SoundStreamSyncer;
import com.soundcloud.android.sync.content.UserAssociationSyncer;
import dagger.Lazy;

import android.content.Context;
import android.net.Uri;

import javax.inject.Inject;
import javax.inject.Provider;

public class ApiSyncerFactory {

    private final Provider<FollowingOperations> followingOpsProvider;
    private final Provider<AccountOperations> accountOpsProvider;
    private final FeatureFlags featureFlags;
    private final Lazy<SoundStreamSyncer> lazySoundStreamSyncer;

    @Inject
    public ApiSyncerFactory(Provider<FollowingOperations> followingOpsProvider, Provider<AccountOperations> accountOpsProvider,
                            FeatureFlags featureFlags, Lazy<SoundStreamSyncer> lazySoundStreamSyncer) {
        this.followingOpsProvider = followingOpsProvider;
        this.accountOpsProvider = accountOpsProvider;
        this.featureFlags = featureFlags;
        this.lazySoundStreamSyncer = lazySoundStreamSyncer;
    }

    public static final String TAG = ApiSyncService.LOG_TAG;

    public SyncStrategy forContentUri(Context context, Uri contentUri) {
        switch (Content.match(contentUri)) {
            case ME_SOUND_STREAM:
                if (featureFlags.isEnabled(Feature.API_MOBILE_STREAM)){
                    return lazySoundStreamSyncer.get();
                } else {
                    return new ApiSyncer(context, context.getContentResolver());
                }
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
