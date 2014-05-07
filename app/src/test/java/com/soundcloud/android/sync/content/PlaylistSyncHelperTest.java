package com.soundcloud.android.sync.content;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isLegacyRequestToUrl;
import static org.mockito.AdditionalMatchers.gt;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.model.CollectionHolder;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Sharing;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.UnknownResource;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.PlaylistStorage;
import com.soundcloud.android.storage.SoundAssociationStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.android.sync.exception.UnknownResourceException;
import com.soundcloud.api.Request;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observer;

import android.accounts.Account;
import android.content.Context;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistSyncHelperTest {

    private PlaylistSyncHelper playlistSyncHelper;

    @Mock
    private PlaylistStorage playlistStorage;
    @Mock
    private PublicCloudAPI publicCloudAPI;
    @Mock
    private SoundAssociationStorage soundAssociationStorage;
    @Mock
    private SyncStateManager syncStateManager;
    @Mock
    private AccountOperations accountOperations;
    @Mock
    private ScModelManager modelManager;
    @Mock
    private Account account;
    @Mock
    private Observer observer;
    @Mock
    private Context context;
    @Mock
    private Playlist playlist;
    @Mock
    private LocalCollection localCollection;

    private Playlist remotePlaylist = new Playlist(123L);
    private Track remoteTrack = new Track(123L);

    @Before
    public void setup() throws IOException {
        playlistSyncHelper = new PlaylistSyncHelper(playlistStorage, soundAssociationStorage, modelManager);
        when(playlist.getId()).thenReturn(123L);
        when(playlist.getTitle()).thenReturn("Skrillex goes to Deleware");
        when(playlist.getSharing()).thenReturn(Sharing.PRIVATE);

        Observable<Playlist> storageObservable = Observable.from(playlist);
        when(playlistStorage.loadPlaylistAsync(123L)).thenReturn(storageObservable);
        when(playlistStorage.store(playlist)).thenReturn(playlist);

        when(publicCloudAPI.readList(argThat(isLegacyRequestToUrl("/playlists/123/tracks"))))
                        .thenReturn(Lists.<ScResource>newArrayList(remoteTrack));

        when(syncStateManager.fromContent(Matchers.<Uri>any())).thenReturn(localCollection);
    }

    @Test
    public void syncMePlaylistsShouldPostLocalPlaylistToApi() throws Exception {
        when(publicCloudAPI.readFullCollection(
                        argThat(isLegacyRequestToUrl("/me/playlists?representation=compact&limit=200")),
                        Matchers.<Class<CollectionHolder<Playlist>>>any())
        ).thenReturn(Collections.<Playlist>emptyList());

        when(playlistStorage.getLocalPlaylists()).thenReturn(Lists.newArrayList(playlist));
        playlistSyncHelper.pushLocalPlaylists(context, publicCloudAPI, syncStateManager);

        ArgumentCaptor<Request> captor = ArgumentCaptor.forClass(Request.class);
        verify(publicCloudAPI).create(captor.capture());
        final Request request = captor.getValue();
        expect(request.toUrl()).toEqual("/playlists");

        final HttpPost httpPost = request.buildRequest(HttpPost.class);
        expect(EntityUtils.toString(httpPost.getEntity())).toEqual("{\"playlist\":{\"title\":\"Skrillex goes to Deleware\",\"sharing\":\"private\",\"tracks\":[]}}");
        expect(httpPost.getFirstHeader("Content-Type").getValue()).toEqual("application/json");

    }

    @Test
    public void syncMePlaylistsShouldNotRemoveLocalPlaylistAfterUnknownResourceResponse() throws Exception {
        when(accountOperations.isUserLoggedIn()).thenReturn(true);
        when(publicCloudAPI.readFullCollection(
                        argThat(isLegacyRequestToUrl("/me/playlists?representation=compact&limit=200")),
                        Matchers.<Class<CollectionHolder<Playlist>>>any())
        ).thenReturn(Collections.<Playlist>emptyList());

        when(playlistStorage.getLocalPlaylists()).thenReturn(Lists.newArrayList(playlist));

        final Uri oldPlaylistUri = Uri.parse("/old/playlist/uri");
        when(playlist.toUri()).thenReturn(oldPlaylistUri);

        final UnknownResource apiPlaylist = Mockito.mock(UnknownResource.class);
        when(publicCloudAPI.create(any(Request.class))).thenReturn(apiPlaylist);
        playlistSyncHelper.pushLocalPlaylists(context, publicCloudAPI, syncStateManager);

        verify(playlist, never()).updateFrom(any(Playlist.class), any(ScResource.CacheUpdateMode.class));
        verify(modelManager, never()).removeFromCache(oldPlaylistUri);
        verify(playlistStorage, never()).removePlaylist(oldPlaylistUri);
    }

    @Test
    public void syncMePlaylistsShouldUpdateLocalStateAfterPlaylistPush() throws Exception {
        when(accountOperations.isUserLoggedIn()).thenReturn(true);
        when(publicCloudAPI.readFullCollection(
                        argThat(isLegacyRequestToUrl("/me/playlists?representation=compact&limit=200")),
                        Matchers.<Class<CollectionHolder<Playlist>>>any())
        ).thenReturn(Collections.<Playlist>emptyList());

        when(playlistStorage.getLocalPlaylists()).thenReturn(Lists.newArrayList(playlist));

        final Uri oldPlaylistUri = Uri.parse("/old/playlist/uri");
        when(playlist.toUri()).thenReturn(oldPlaylistUri);

        final Playlist apiPlaylist = Mockito.mock(Playlist.class);
        when(publicCloudAPI.create(any(Request.class))).thenReturn(apiPlaylist);
        playlistSyncHelper.pushLocalPlaylists(context, publicCloudAPI, syncStateManager);

        InOrder inOrder = Mockito.inOrder(playlist, modelManager, playlistStorage, soundAssociationStorage, syncStateManager, playlistStorage);

        inOrder.verify(playlist).updateFrom(apiPlaylist, ScResource.CacheUpdateMode.FULL);
        inOrder.verify(modelManager).removeFromCache(oldPlaylistUri);
        inOrder.verify(playlistStorage).store(apiPlaylist);
        inOrder.verify(soundAssociationStorage).addCreation(apiPlaylist);
        inOrder.verify(syncStateManager).updateLastSyncSuccessTime(eq(oldPlaylistUri), gt(0L));
        inOrder.verify(playlistStorage).removePlaylist(oldPlaylistUri);
    }

    @Test
    public void syncPlaylistCachesAndStoresUpdatedPlaylist() throws IOException {
        when(publicCloudAPI.read(argThat(isLegacyRequestToUrl("/playlists/123")))).thenReturn(playlist);
        when(playlistStorage.getUnpushedTracksForPlaylist(123L)).thenReturn(Collections.<Long>emptyList());
        playlistSyncHelper.syncPlaylist(Content.PLAYLIST.forId(123L), publicCloudAPI);
        verify(modelManager).cache(playlist, ScResource.CacheUpdateMode.FULL);
        verify(playlistStorage).store(playlist);
    }

    @Test(expected = UnknownResourceException.class)
    public void pushUnpushedTracksShouldThrowUnknownResourceExceptionWithUnexpectedResourceResponse() throws IOException {
        when(publicCloudAPI.read(argThat(isLegacyRequestToUrl("/playlists/123")))).thenReturn(Mockito.mock(UnknownResource.class));
        playlistSyncHelper.syncPlaylist(Content.PLAYLIST.forId(123L), publicCloudAPI);
    }

    @Test
    public void pushUnpushedTracksShouldReturnOriginalPlaylistIfNoPendingPushes() throws IOException {
        when(publicCloudAPI.read(argThat(isLegacyRequestToUrl("/playlists/123")))).thenReturn(playlist);
        when(playlistStorage.getUnpushedTracksForPlaylist(123L)).thenReturn(Collections.<Long>emptyList());
        expect(playlistSyncHelper.syncPlaylist(Content.PLAYLIST.forId(123L), publicCloudAPI)).toEqual(playlist);
    }

    @Test
    public void pushUnpushedTracksShouldReturnOriginalPlaylistAfterUnsuccessfulPush() throws IOException {
        when(publicCloudAPI.read(argThat(isLegacyRequestToUrl("/playlists/123")))).thenReturn(playlist);
        when(playlistStorage.getUnpushedTracksForPlaylist(123L)).thenReturn(Lists.newArrayList(4L, 5L, 6L));
        expect(playlistSyncHelper.syncPlaylist(Content.PLAYLIST.forId(123L), publicCloudAPI)).toEqual(playlist);
    }

    @Test
    public void pushUnpushedTracksShouldReturnNewPlaylistAfterSuccessfulPush() throws IOException {
        when(publicCloudAPI.read(argThat(isLegacyRequestToUrl("/playlists/123")))).thenReturn(playlist);
        when(playlistStorage.getUnpushedTracksForPlaylist(123L)).thenReturn(Lists.newArrayList(4L, 5L, 6L));

        Playlist updatedPlaylist = Mockito.mock(Playlist.class);
        when(publicCloudAPI.update(any(Request.class))).thenReturn(updatedPlaylist);
        when(playlistStorage.store(updatedPlaylist)).thenReturn(updatedPlaylist);

        final Playlist actual = playlistSyncHelper.syncPlaylist(Content.PLAYLIST.forId(123L), publicCloudAPI);
        expect(actual).not.toEqual(playlist);
    }

    @Test
    public void pushUnpushedTracksShouldCallCreateOnApiWithUpdatedPlaylistRequest() throws IOException {
        when(publicCloudAPI.read(argThat(isLegacyRequestToUrl("/playlists/123")))).thenReturn(playlist);
        when(playlist.getTracks()).thenReturn(Lists.newArrayList(new Track(1L), new Track(2L), new Track(3L)));
        when(playlistStorage.getUnpushedTracksForPlaylist(123L)).thenReturn(Lists.newArrayList(4L, 5L, 6L));

        playlistSyncHelper.syncPlaylist(Content.PLAYLIST.forId(123L), publicCloudAPI);

        ArgumentCaptor<Request> captor = new ArgumentCaptor<Request>();
        verify(publicCloudAPI).update(captor.capture());
        Request request = captor.getValue();
        expect(request.toUrl()).toEqual("/playlists/123");

        final HttpPost httpPost = request.buildRequest(HttpPost.class);
        expect(EntityUtils.toString(httpPost.getEntity())).toEqual("{\"playlist\":{\"tracks\":[{\"id\":1},{\"id\":2},{\"id\":3},{\"id\":4},{\"id\":5},{\"id\":6}]}}");
        expect(httpPost.getFirstHeader("Content-Type").getValue()).toEqual("application/json");
    }

    @Test
    public void syncMePlaylistsShouldSyncNoRemotePlaylists() throws Exception {
        setupMyPlaylistsRemote(Collections.<Playlist>emptyList());

        playlistSyncHelper.pullRemotePlaylists(publicCloudAPI);

        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(soundAssociationStorage).syncToLocal(captor.capture(), eq(Content.ME_PLAYLISTS.uri));
        expect(captor.getValue()).toEqual(Collections.emptyList());
    }

    @Test
    public void syncMePlaylistsShouldStoreMiniRepresentationInModelManager() throws Exception {
        setupMyPlaylistsRemote(Lists.newArrayList(playlist));

        playlistSyncHelper.pullRemotePlaylists(publicCloudAPI);
        verify(modelManager).cache(playlist, ScResource.CacheUpdateMode.MINI);
    }

    public void syncMePlaylistsSyncsRemotePlaylist() throws Exception {
        setupMyPlaylistsRemote(Lists.newArrayList(remotePlaylist));

        playlistSyncHelper.pullRemotePlaylists(publicCloudAPI);

        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(soundAssociationStorage).syncToLocal(captor.capture(), eq(Content.ME_PLAYLISTS.uri));

        List<SoundAssociation> syncedAssociations = captor.getValue();
        final Playlist syncedPlaylist = (Playlist) syncedAssociations.get(0).playable;
        expect(syncedPlaylist).toBe(remotePlaylist);
    }

    @Test
    public void syncMePlaylistsShouldNotFetchPlaylistTracksWhenRemoteAndLocalTrackCountsAreEqual() throws Exception {
        when(playlistStorage.getPlaylistTrackIds(playlist.getId())).thenReturn(Lists.newArrayList(1L));
        remotePlaylist.setTrackCount(1);
        setupMyPlaylistsRemote(Lists.newArrayList(remotePlaylist));

        playlistSyncHelper.pullRemotePlaylists(publicCloudAPI);

        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(soundAssociationStorage).syncToLocal(captor.capture(), eq(Content.ME_PLAYLISTS.uri));

        List<SoundAssociation> syncedAssociations = captor.getValue();
        final Playlist syncedPlaylist = (Playlist) syncedAssociations.get(0).playable;
        expect(syncedPlaylist.getTracks()).toBeEmpty();
    }

    @Test
    public void syncMePlaylistsShouldSyncRemoteTracksWhenLocalAndRemoteTrackCountsDiffer() throws Exception {
        when(playlistStorage.getPlaylistTrackIds(playlist.getId())).thenReturn(null);
        remotePlaylist.setTrackCount(1);
        setupMyPlaylistsRemote(Lists.newArrayList(remotePlaylist));

        playlistSyncHelper.pullRemotePlaylists(publicCloudAPI);

        final ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(soundAssociationStorage).syncToLocal(captor.capture(), eq(Content.ME_PLAYLISTS.uri));

        List<SoundAssociation> syncedAssociations = captor.getValue();
        final Playlist syncedPlaylist = (Playlist) syncedAssociations.get(0).playable;
        expect(syncedPlaylist.getTracks().get(0)).toBe(remoteTrack);
    }

    @Test
    public void syncMePlaylistsShouldNotFetchRemoteTracksOnPlaylistWithMoreThanMaxTracksToSync() throws Exception {
        when(playlist.getId()).thenReturn(123L);
        when(playlist.getTrackCount()).thenReturn(PlaylistSyncer.MAX_MY_PLAYLIST_TRACK_COUNT_SYNC + 1);
        when(playlistStorage.getPlaylistTrackIds(playlist.getId())).thenReturn(Lists.newArrayList(1L));
        setupMyPlaylistsRemote(Lists.newArrayList(playlist));

        playlistSyncHelper.pullRemotePlaylists(publicCloudAPI);
        verify(publicCloudAPI, never()).readList(Matchers.<Request>any());
    }

    @Test
    public void syncMePlaylistsShouldSyncChangedPlaylists() throws Exception {
        when(playlist.getTrackCount()).thenReturn(1);
        when(playlistStorage.getPlaylistTrackIds(playlist.getId())).thenReturn(Collections.<Long>emptyList());
        setupMyPlaylistsRemote(Lists.newArrayList(playlist));

        final ArrayList<SoundAssociation> associations = Lists.newArrayList(new SoundAssociation(playlist));
        when(soundAssociationStorage.syncToLocal(eq(associations), eq(Content.ME_PLAYLISTS.uri))).thenReturn(true);

        ApiSyncResult result = playlistSyncHelper.pullRemotePlaylists(publicCloudAPI);
        expect(result.change).toEqual(ApiSyncResult.CHANGED);
        expect(result.synced_at).toBeGreaterThan(0L);
        expect(result.new_size).toBe(1);
        expect(result.success).toBeTrue();
    }


    private void setupMyPlaylistsRemote(List<Playlist> remotePlaylists) throws java.io.IOException {
        when(publicCloudAPI.readFullCollection(
                        argThat(isLegacyRequestToUrl("/me/playlists?representation=compact&limit=200")),
                        Matchers.<Class<CollectionHolder<Playlist>>>any())
        ).thenReturn(remotePlaylists);
    }

}
