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
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.Sharing;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.UnknownResource;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.PlaylistStorage;
import com.soundcloud.android.storage.SoundAssociationStorage;
import com.soundcloud.android.storage.provider.Content;
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
import java.util.Collections;

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

    @Before
    public void setup() {
        playlistSyncHelper = new PlaylistSyncHelper(playlistStorage, soundAssociationStorage, modelManager);
        when(playlist.getId()).thenReturn(123L);
        when(playlist.getTitle()).thenReturn("Skrillex goes to Deleware");
        when(playlist.getSharing()).thenReturn(Sharing.PRIVATE);

        Observable<Playlist> storageObservable = Observable.from(playlist);
        when(playlistStorage.loadPlaylistAsync(123L)).thenReturn(storageObservable);
        when(playlistStorage.store(playlist)).thenReturn(playlist);
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
        when(accountOperations.soundCloudAccountExists()).thenReturn(true);
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

        verify(playlist, never()).localToGlobal(any(Context.class), any(Playlist.class));
        verify(modelManager, never()).removeFromCache(oldPlaylistUri);
        verify(playlistStorage, never()).removePlaylist(oldPlaylistUri);
    }

    @Test
    public void syncMePlaylistsShouldUpdateLocalStateAfterPlaylistPush() throws Exception {
        when(accountOperations.soundCloudAccountExists()).thenReturn(true);
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

        inOrder.verify(playlist).localToGlobal(context, apiPlaylist);
        inOrder.verify(modelManager).removeFromCache(oldPlaylistUri);
        inOrder.verify(playlistStorage).store(apiPlaylist);
        inOrder.verify(soundAssociationStorage).addCreation(apiPlaylist);
        inOrder.verify(syncStateManager).updateLastSyncSuccessTime(eq(oldPlaylistUri), gt(0L));
        inOrder.verify(playlistStorage).removePlaylist(oldPlaylistUri);
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

}
