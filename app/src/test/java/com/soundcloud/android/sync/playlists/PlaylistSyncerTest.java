package com.soundcloud.android.sync.playlists;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.SoundAssociationStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.SyncStateManager;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.content.ContentResolver;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistSyncerTest {

    @Mock
    ContentResolver resolver;
    @Mock
    SoundAssociationStorage soundAssociationStorage;
    @Mock
    PlaylistSyncHelper syncHelper;
    @Mock
    PublicCloudAPI publicCloudAPI;
    @Mock
    SyncStateManager syncStateManager;
    @Mock
    AccountOperations accountOperations;


    private PlaylistSyncer playlistSyncer;

    @Before
    public void setUp() throws Exception {
        playlistSyncer = new PlaylistSyncer(Robolectric.application, resolver,
                publicCloudAPI, syncStateManager, accountOperations, syncHelper);
    }

    @Test
    public void shouldSkipMePlaylistsSyncWithNoAccount() throws Exception {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);
        playlistSyncer.syncContent(Content.ME_PLAYLISTS.uri);
        verifyZeroInteractions(soundAssociationStorage);
        verifyZeroInteractions(publicCloudAPI);
    }

    @Test
    public void syncMePlaylistsShouldReturnResultFromSyncHelper() throws Exception {
        final ApiSyncResult result = Mockito.mock(ApiSyncResult.class);
        when(syncHelper.pullRemotePlaylists(publicCloudAPI)).thenReturn(result);
        when(accountOperations.isUserLoggedIn()).thenReturn(true);
        expect(playlistSyncer.syncContent(Content.ME_PLAYLISTS.uri)).toBe(result);
    }
}
