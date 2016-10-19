package com.soundcloud.android.sync;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.affiliations.MyFollowingsSyncerFactory;
import com.soundcloud.android.sync.likes.MyLikesSyncer;
import com.soundcloud.android.sync.me.MeSyncer;
import com.soundcloud.android.sync.playlists.LegacySinglePlaylistSyncer;
import com.soundcloud.android.sync.playlists.MyPlaylistsSyncerFactory;
import com.soundcloud.android.sync.playlists.SinglePlaylistSyncerFactory;
import com.soundcloud.android.sync.posts.MyPostsSyncer;
import dagger.Lazy;

import android.net.Uri;

import javax.inject.Inject;

public class ApiSyncerFactory {

    private final MyPlaylistsSyncerFactory lazyPlaylistsSyncer;
    private final Lazy<MyLikesSyncer> lazyMyLikesSyncer;
    private final Lazy<MyPostsSyncer> lazyMyPostsSyncer;
    private final MyFollowingsSyncerFactory myFollowingsSyncerLazyFactory;
    private final Lazy<MeSyncer> lazyMeSyncer;
    private final SinglePlaylistSyncerFactory singlePlaylistSyncerFactory;

    @Inject
    public ApiSyncerFactory(MyPlaylistsSyncerFactory lazyPlaylistsSyncer,
                            Lazy<MyLikesSyncer> lazyMyLikesSyncer,
                            Lazy<MyPostsSyncer> lazyMyPostsSyncer,
                            MyFollowingsSyncerFactory myFollowingsSyncerLazyFactory,
                            Lazy<MeSyncer> lazyMeSyncer,
                            SinglePlaylistSyncerFactory singlePlaylistSyncerFactory) {
        this.lazyPlaylistsSyncer = lazyPlaylistsSyncer;
        this.lazyMyLikesSyncer = lazyMyLikesSyncer;
        this.lazyMyPostsSyncer = lazyMyPostsSyncer;
        this.myFollowingsSyncerLazyFactory = myFollowingsSyncerLazyFactory;
        this.lazyMeSyncer = lazyMeSyncer;
        this.singlePlaylistSyncerFactory = singlePlaylistSyncerFactory;
    }

    public static final String TAG = ApiSyncService.LOG_TAG;

    public SyncStrategy forContentUri(Uri contentUri) {
        switch (Content.match(contentUri)) {
            case ME_LIKES:
                return lazyMyLikesSyncer.get();

            case ME_FOLLOWINGS:
                return myFollowingsSyncerLazyFactory.create("" /* not used */ );

            case ME_PLAYLISTS:
                return lazyPlaylistsSyncer.create(false);

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
