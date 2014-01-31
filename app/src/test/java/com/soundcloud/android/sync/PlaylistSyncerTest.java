package com.soundcloud.android.sync;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isLegacyRequestToUrl;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.playlists.PlaylistSyncOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.storage.SoundAssociationStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.content.PlaylistSyncer;
import com.soundcloud.api.Request;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;

import android.content.ContentResolver;
import android.net.Uri;

import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistSyncerTest {

    @Mock
    ContentResolver resolver;
    @Mock
    SoundAssociationStorage soundAssociationStorage;
    @Mock
    PlaylistSyncOperations playlistOperations;
    @Mock
    PublicCloudAPI publicCloudAPI;
    @Mock
    SyncStateManager syncStateManager;
    @Mock
    AccountOperations accountOperations;
    @Mock
    Playlist playlist;
    @Mock
    LocalCollection localCollection;

    private PlaylistSyncer playlistSyncer;

    @Before
    public void setUp() throws Exception {
        playlistSyncer = new PlaylistSyncer(Robolectric.application, resolver,
                publicCloudAPI, syncStateManager, accountOperations, playlistOperations);
    }

    @Test
    public void shouldSkipMePlaylistsSyncWithNoAccount() throws Exception {
        when(accountOperations.soundCloudAccountExists()).thenReturn(false);
        playlistSyncer.syncContent(Content.ME_PLAYLISTS.uri);
        verifyZeroInteractions(soundAssociationStorage);
        verifyZeroInteractions(publicCloudAPI);
    }

    @Test
    public void syncMePlaylistsShouldNotFetchTracksWithNoPlaylists() throws Exception {
        when(accountOperations.soundCloudAccountExists()).thenReturn(true);
        final List<Playlist> playlists = Collections.emptyList();

        when(publicCloudAPI.readFullCollection(
                argThat(isLegacyRequestToUrl("/me/playlists?representation=compact&limit=200")),
                Matchers.<Class<CollectionHolder<Playlist>>>any())
        ).thenReturn(playlists);

        playlistSyncer.syncContent(Content.ME_PLAYLISTS.uri);
    }

    @Test
    public void syncMePlaylistsShouldNotSyncTracksOnUpToDatePlaylist() throws Exception {
        when(localCollection.hasSyncedBefore()).thenReturn(true);
        when(localCollection.shouldAutoRefresh()).thenReturn(false);
        setupMyPlaylistsRemote(Lists.newArrayList(playlist));

        playlistSyncer.syncContent(Content.ME_PLAYLISTS.uri);

        verify(publicCloudAPI, never()).readList(Matchers.<Request>any());
    }

    @Test
    public void syncMePlaylistsShouldNotSyncTracksOnStalePlaylistOffWifi() throws Exception {
        TestHelper.connectedViaWifi(false);
        when(localCollection.hasSyncedBefore()).thenReturn(true);
        when(localCollection.shouldAutoRefresh()).thenReturn(true);
        setupMyPlaylistsRemote(Lists.newArrayList(playlist));

        playlistSyncer.syncContent(Content.ME_PLAYLISTS.uri);

        verify(publicCloudAPI, never()).readList(Matchers.<Request>any());
    }

    @Test
    public void syncMePlaylistsShouldSyncTracksOnStalePlaylistOnWifi() throws Exception {
        TestHelper.connectedViaWifi(true);
        when(localCollection.hasSyncedBefore()).thenReturn(true);
        when(localCollection.shouldAutoRefresh()).thenReturn(true);
        when(playlist.getId()).thenReturn(123L);
        setupMyPlaylistsRemote(Lists.newArrayList(playlist));

        playlistSyncer.syncContent(Content.ME_PLAYLISTS.uri);

        verify(publicCloudAPI).readList(argThat(isLegacyRequestToUrl("/playlists/123/tracks")));
    }

    @Test
    public void syncMePlaylistsShouldSyncTracksOnPlaylistThatHasNeverBeenSynced() throws Exception {
        when(localCollection.hasSyncedBefore()).thenReturn(false);
        when(localCollection.shouldAutoRefresh()).thenReturn(false);
        when(playlist.getId()).thenReturn(123L);
        setupMyPlaylistsRemote(Lists.newArrayList(playlist));

        playlistSyncer.syncContent(Content.ME_PLAYLISTS.uri);

        verify(publicCloudAPI).readList(argThat(isLegacyRequestToUrl("/playlists/123/tracks")));
    }

    @Test
    public void syncMePlaylistsShouldNotSyncTracksOnPlaylistWithMoreThanMaxTracksToSync() throws Exception {
        when(localCollection.hasSyncedBefore()).thenReturn(false);
        when(localCollection.shouldAutoRefresh()).thenReturn(true);
        when(playlist.getId()).thenReturn(123L);
        when(playlist.getTrackCount()).thenReturn(PlaylistSyncer.MAX_MY_PLAYLIST_TRACK_COUNT_SYNC + 1);

        setupMyPlaylistsRemote(Lists.newArrayList(playlist));

        playlistSyncer.syncContent(Content.ME_PLAYLISTS.uri);

        verify(publicCloudAPI, never()).readList(Matchers.<Request>any());
    }

    private void setupMyPlaylistsRemote(List<Playlist> playlists) throws java.io.IOException {
        when(accountOperations.soundCloudAccountExists()).thenReturn(true);
        when(syncStateManager.fromContent(Matchers.<Uri>any())).thenReturn(localCollection);
        when(publicCloudAPI.readFullCollection(
                argThat(isLegacyRequestToUrl("/me/playlists?representation=compact&limit=200")),
                Matchers.<Class<CollectionHolder<Playlist>>>any())
        ).thenReturn(playlists);
    }

    @Test
    public void syncMePlaylistsShouldSyncChangedPlaylists() throws Exception {
        when(localCollection.hasSyncedBefore()).thenReturn(true);
        when(localCollection.shouldAutoRefresh()).thenReturn(false);
        when(playlistOperations.syncMyNewPlaylists(eq(Lists.newArrayList(new SoundAssociation(playlist))))).thenReturn(true);

        setupMyPlaylistsRemote(Lists.newArrayList(playlist));

        ApiSyncResult result = playlistSyncer.syncContent(Content.ME_PLAYLISTS.uri);
        expect(result.change).toEqual(ApiSyncResult.CHANGED);
        expect(result.synced_at).toBeGreaterThan(0L);
        expect(result.new_size).toBe(1);
        expect(result.success).toBeTrue();
    }

    @Test
    public void syncMePlaylistsShouldSyncUnchangedPlaylists() throws Exception {
        when(localCollection.hasSyncedBefore()).thenReturn(true);
        when(localCollection.shouldAutoRefresh()).thenReturn(false);

        setupMyPlaylistsRemote(Lists.newArrayList(playlist));

        ApiSyncResult result = playlistSyncer.syncContent(Content.ME_PLAYLISTS.uri);
        expect(result.change).toEqual(ApiSyncResult.UNCHANGED);
        expect(result.synced_at).toBeGreaterThan(0L);
        expect(result.new_size).toBe(1);
        expect(result.success).toBeTrue();
    }
}
