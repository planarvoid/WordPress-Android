package com.soundcloud.android.sync;

import static com.pivotallabs.greatexpectations.Expect.expect;
import static org.mockito.Mockito.mock;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.content.PlaylistSyncer;
import com.soundcloud.android.sync.content.SyncStrategy;
import com.soundcloud.android.sync.content.UserAssociationSyncer;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Provider;

@RunWith(DefaultTestRunner.class)
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
        return new ApiSyncerFactory(new Provider<FollowingOperations>() {
            @Override
            public FollowingOperations get() {
                return mock(FollowingOperations.class);
            }
        }, new Provider<AccountOperations>() {
            @Override
            public AccountOperations get() {
                return mock(AccountOperations.class);
            }
        }).forContentUri(Robolectric.application, content.uri);
    }
}
