package com.soundcloud.android.sync;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.content.SyncStrategy;
import com.soundcloud.android.sync.content.UserAssociationSyncer;
import com.soundcloud.android.sync.likes.MyLikesSyncer;
import com.soundcloud.android.sync.playlists.PlaylistSyncer;
import com.soundcloud.android.sync.stream.SoundStreamSyncer;
import dagger.Lazy;

import android.content.Context;
import android.net.Uri;

import javax.inject.Inject;
import javax.inject.Provider;

@SuppressWarnings({"PMD.SingularField", "PMD.UnusedPrivateField"}) // remove this once we use playlist syncer
public class ApiSyncerFactory {

    private final Provider<FollowingOperations> followingOpsProvider;
    private final Provider<AccountOperations> accountOpsProvider;
    private final FeatureFlags featureFlags;
    private final Lazy<SoundStreamSyncer> lazySoundStreamSyncer;
    private final Lazy<MyLikesSyncer> lazyMyLikesSyncer;

    @Inject
    public ApiSyncerFactory(Provider<FollowingOperations> followingOpsProvider, Provider<AccountOperations> accountOpsProvider,
                            FeatureFlags featureFlags, Lazy<SoundStreamSyncer> lazySoundStreamSyncer,
                            Lazy<MyLikesSyncer> lazyMyLikesSyncer) {
        this.followingOpsProvider = followingOpsProvider;
        this.accountOpsProvider = accountOpsProvider;
        this.featureFlags = featureFlags;
        this.lazySoundStreamSyncer = lazySoundStreamSyncer;
        this.lazyMyLikesSyncer = lazyMyLikesSyncer;
    }

    public static final String TAG = ApiSyncService.LOG_TAG;

    public SyncStrategy forContentUri(Context context, Uri contentUri) {
        switch (Content.match(contentUri)) {
            case ME_SOUND_STREAM:
                if (featureFlags.isEnabled(Flag.API_MOBILE_STREAM)){
                    return lazySoundStreamSyncer.get();
                } else {
                    return new ApiSyncer(context, context.getContentResolver());
                }
            case ME_LIKES:
                if (featureFlags.isEnabled(Flag.NEW_LIKES_END_TO_END)) {
                    return lazyMyLikesSyncer.get();
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
