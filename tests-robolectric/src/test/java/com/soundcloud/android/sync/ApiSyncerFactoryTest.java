package com.soundcloud.android.sync;

import static com.pivotallabs.greatexpectations.Expect.expect;
import static org.mockito.Mockito.mock;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.content.SyncStrategy;
import com.soundcloud.android.sync.content.UserAssociationSyncer;
import com.soundcloud.android.sync.likes.MyLikesSyncer;
import com.soundcloud.android.sync.playlists.PlaylistSyncer;
import com.soundcloud.android.sync.posts.MyPlaylistsSyncer;
import com.soundcloud.android.sync.stream.SoundStreamSyncer;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import android.app.NotificationManager;

@RunWith(SoundCloudTestRunner.class)
public class ApiSyncerFactoryTest {

    @Test
    public void shouldReturnUserAssociationSyncerForFollowings() throws Exception {
        expect(getSyncer(Content.ME_FOLLOWINGS)).toBeInstanceOf(UserAssociationSyncer.class);
    }

    @Test
    public void shouldReturnUserAssociationSyncerForFollowers() throws Exception {
        expect(getSyncer(Content.ME_FOLLOWERS)).toBeInstanceOf(UserAssociationSyncer.class);
    }

    @Test
    public void shouldReturnPlaylistSyncerForMePlaylists() throws Exception {
        expect(getSyncer(Content.ME_PLAYLISTS)).toBeInstanceOf(PlaylistSyncer.class);
    }

    @Test
    public void shouldReturnPlaylistSyncerPlaylist() throws Exception {
        expect(getSyncer(Content.PLAYLIST)).toBeInstanceOf(PlaylistSyncer.class);
    }

    private SyncStrategy getSyncer(Content content) {
        return new ApiSyncerFactory(
                InjectionSupport.providerOf(mock(FollowingOperations.class)),
                InjectionSupport.providerOf(mock(AccountOperations.class)),
                InjectionSupport.providerOf(mock(NotificationManager.class)),
                Mockito.mock(FeatureFlags.class),
                InjectionSupport.lazyOf(mock(SoundStreamSyncer.class)),
                InjectionSupport.lazyOf(mock(MyPlaylistsSyncer.class)),
                InjectionSupport.lazyOf(mock(MyLikesSyncer.class)),
                mock(JsonTransformer.class)
        ).forContentUri(Robolectric.application, content.uri);
    }
}
