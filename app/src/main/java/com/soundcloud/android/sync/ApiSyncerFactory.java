package com.soundcloud.android.sync;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.content.SyncStrategy;
import com.soundcloud.android.sync.content.UserAssociationSyncer;
import com.soundcloud.android.sync.likes.MyLikesSyncer;
import com.soundcloud.android.sync.posts.MyPlaylistsSyncer;
import com.soundcloud.android.sync.posts.MyPostsSyncer;
import com.soundcloud.android.sync.stream.SoundStreamSyncer;
import dagger.Lazy;

import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;

import javax.inject.Inject;
import javax.inject.Provider;

@SuppressWarnings({"PMD.SingularField", "PMD.UnusedPrivateField"}) // remove this once we use playlist syncer
public class ApiSyncerFactory {

    private final Provider<FollowingOperations> followingOpsProvider;
    private final Provider<AccountOperations> accountOpsProvider;
    private final Provider<NotificationManager> notificationManagerProvider;
    private final FeatureFlags featureFlags;
    private final Lazy<SoundStreamSyncer> lazySoundStreamSyncer;
    private final Lazy<MyPlaylistsSyncer> lazyPlaylistsSyncer;
    private final Lazy<MyLikesSyncer> lazyMyLikesSyncer;
    private final Lazy<MyPostsSyncer> lazyMyPostsSyncer;
    private final JsonTransformer jsonTransformer;

    @Inject
    public ApiSyncerFactory(Provider<FollowingOperations> followingOpsProvider, Provider<AccountOperations> accountOpsProvider,
                            Provider<NotificationManager> notificationManagerProvider,
                            FeatureFlags featureFlags, Lazy<SoundStreamSyncer> lazySoundStreamSyncer,
                            Lazy<MyPlaylistsSyncer> lazyPlaylistsSyncer, Lazy<MyLikesSyncer> lazyMyLikesSyncer,
                            Lazy<MyPostsSyncer> lazyMyPostsSyncer, JsonTransformer jsonTransformer) {
        this.followingOpsProvider = followingOpsProvider;
        this.accountOpsProvider = accountOpsProvider;
        this.notificationManagerProvider = notificationManagerProvider;
        this.featureFlags = featureFlags;
        this.lazySoundStreamSyncer = lazySoundStreamSyncer;
        this.lazyPlaylistsSyncer = lazyPlaylistsSyncer;
        this.lazyMyLikesSyncer = lazyMyLikesSyncer;
        this.lazyMyPostsSyncer = lazyMyPostsSyncer;
        this.jsonTransformer = jsonTransformer;
    }

    public static final String TAG = ApiSyncService.LOG_TAG;

    public SyncStrategy forContentUri(Context context, Uri contentUri) {
        switch (Content.match(contentUri)) {
            case ME_SOUND_STREAM:
                if (featureFlags.isEnabled(Flag.API_MOBILE_STREAM)) {
                    return lazySoundStreamSyncer.get();
                } else {
                    return new ApiSyncer(context, context.getContentResolver());
                }
            case ME_LIKES:
                return lazyMyLikesSyncer.get();

            case ME_FOLLOWINGS:
            case ME_FOLLOWERS:
                return new UserAssociationSyncer(
                        context, accountOpsProvider.get(), followingOpsProvider.get(), notificationManagerProvider.get(), jsonTransformer);

            case ME_PLAYLISTS:
                return lazyPlaylistsSyncer.get();

            case ME_SOUNDS:
                return lazyMyPostsSyncer.get();

            default:
                return new ApiSyncer(context, context.getContentResolver());
        }
    }

}
