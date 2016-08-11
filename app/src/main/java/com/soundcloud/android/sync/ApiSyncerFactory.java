package com.soundcloud.android.sync;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.affiliations.MyFollowingsSyncer;
import com.soundcloud.android.sync.entities.MeSyncer;
import com.soundcloud.android.sync.likes.MyLikesSyncer;
import com.soundcloud.android.sync.playlists.LegacySinglePlaylistSyncer;
import com.soundcloud.android.sync.playlists.SinglePlaylistSyncerFactory;
import com.soundcloud.android.sync.posts.MyPostsSyncer;
import dagger.Lazy;

import android.net.Uri;

import javax.inject.Inject;

public class ApiSyncerFactory {

    private final Lazy<MyLikesSyncer> lazyMyLikesSyncer;
    private final Lazy<MyPostsSyncer> lazyMyPostsSyncer;
    private final Lazy<MyFollowingsSyncer> lazyMyFollowingsSyncerLazy;
    private final Lazy<MeSyncer> lazyMeSyncer;
    private final SinglePlaylistSyncerFactory singlePlaylistSyncerFactory;

    @Inject
    public ApiSyncerFactory(Lazy<MyLikesSyncer> lazyMyLikesSyncer,
                            Lazy<MyPostsSyncer> lazyMyPostsSyncer,
                            Lazy<MyFollowingsSyncer> lazyMyFollowingsSyncerLazy,
                            Lazy<MeSyncer> lazyMeSyncer,
                            SinglePlaylistSyncerFactory singlePlaylistSyncerFactory) {
        this.lazyMyLikesSyncer = lazyMyLikesSyncer;
        this.lazyMyPostsSyncer = lazyMyPostsSyncer;
        this.lazyMyFollowingsSyncerLazy = lazyMyFollowingsSyncerLazy;
        this.lazyMeSyncer = lazyMeSyncer;
        this.singlePlaylistSyncerFactory = singlePlaylistSyncerFactory;
    }

    public static final String TAG = ApiSyncService.LOG_TAG;

    public SyncStrategy forContentUri(Uri contentUri) {
        switch (Content.match(contentUri)) {
            case ME_LIKES:
                return lazyMyLikesSyncer.get();

            case ME_FOLLOWINGS:
                return lazyMyFollowingsSyncerLazy.get();

            case PLAYLIST:
                return new LegacySinglePlaylistSyncer(singlePlaylistSyncerFactory,
                                                      getPlaylistUrnFromLegacyContentUri(contentUri));

            case ME_SOUNDS:
                return lazyMyPostsSyncer.get();

            case ME:
                return lazyMeSyncer.get();

            default:
                throw new IllegalArgumentException("Unhandled content uri for sync " + contentUri);
        }
    }

    // this might be useful outside of this class, but not sure yet
    private Urn getPlaylistUrnFromLegacyContentUri(Uri contentUri) {
        return Urn.forPlaylist(Long.valueOf(contentUri.getLastPathSegment()));
    }

}
