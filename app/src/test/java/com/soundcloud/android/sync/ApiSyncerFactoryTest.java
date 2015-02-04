package com.soundcloud.android.sync;

import static com.pivotallabs.greatexpectations.Expect.expect;
import static org.mockito.Mockito.mock;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.playlists.PlaylistSyncer;
import com.soundcloud.android.sync.content.SyncStrategy;
import com.soundcloud.android.sync.content.UserAssociationSyncer;
import com.xtremelabs.robolectric.Robolectric;
import dagger.Lazy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import android.app.NotificationManager;

import javax.inject.Provider;

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
        }, new Provider<NotificationManager>() {
            @Override
            public NotificationManager get() {
                return mock(NotificationManager.class);
            }
        }, Mockito.mock(FeatureFlags.class),
                Mockito.mock(Lazy.class),
                Mockito.mock(Lazy.class),
                mock(JsonTransformer.class)
        ).forContentUri(Robolectric.application, content.uri);
    }
}
