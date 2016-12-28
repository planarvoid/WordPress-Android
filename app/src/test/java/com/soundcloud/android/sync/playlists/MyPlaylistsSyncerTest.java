package com.soundcloud.android.sync.playlists;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.LoadOfflinePlaylistsCommand;
import com.soundcloud.android.playlists.LoadPlaylistPendingRemovalCommand;
import com.soundcloud.android.playlists.LoadPlaylistTrackUrnsCommand;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.RemovePlaylistCommand;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.posts.PostsSyncer;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.support.v4.util.ArrayMap;
import android.util.Pair;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class MyPlaylistsSyncerTest extends AndroidUnitTest {

    private MyPlaylistsSyncer syncer;

    @Mock private PostsSyncer postsSyncer;
    @Mock private LoadLocalPlaylistsCommand loadLocalPlaylists;
    @Mock private LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrns;
    @Mock private ReplacePlaylistPostCommand replacePlaylist;
    @Mock private LoadPlaylistPendingRemovalCommand loadPlaylistPendingRemovalCommand;
    @Mock private RemovePlaylistCommand removePlaylistCommand;
    @Mock private LoadOfflinePlaylistsCommand loadOfflinePlaylistsCommand;
    @Mock private ApiClient apiClient;
    @Mock private SinglePlaylistSyncerFactory singlePlaylistSyncerFactory;
    @Mock private SinglePlaylistSyncer singlePlaylistSyncer;
    @Mock private EventBus eventBus;

    private Urn localPlaylistUrn;

    @Before
    public void setUp() throws Exception {
        when(singlePlaylistSyncerFactory.create(any(Urn.class))).thenReturn(null);
        when(loadOfflinePlaylistsCommand.call(any(Void.class))).thenReturn(Collections.<Urn>emptyList());

        syncer = new MyPlaylistsSyncer(
                postsSyncer,
                loadLocalPlaylists,
                loadPlaylistTrackUrns,
                replacePlaylist,
                loadPlaylistPendingRemovalCommand,
                removePlaylistCommand,
                apiClient,
                loadOfflinePlaylistsCommand,
                singlePlaylistSyncerFactory,
                eventBus,
                false
        );
    }

    @Test
    public void shouldReturnChangedResultIfPostsSyncerReturnsTrue() throws Exception {
        when(postsSyncer.call(anyListOf(Urn.class))).thenReturn(true);
        final boolean syncResult = syncer.call();
        assertThat(syncResult).isTrue();
    }

    @Test
    public void shouldReturnUnchangedResultIfPostsSyncerReturnsTrue() throws Exception {
        when(postsSyncer.call(anyListOf(Urn.class))).thenReturn(false);
        final boolean syncResult = syncer.call();
        assertThat(syncResult).isFalse();
    }

    @Test
    public void replacesOldPlaylistWithNewPlaylistAfterSuccessfulPush() throws Exception {
        final List<PlaylistItem> playlists = ModelFixtures.create(PlaylistItem.class, 2);
        final List<Urn> playlist1Tracks = Arrays.asList(Urn.forTrack(1), Urn.forTrack(2));
        final List<Urn> playlist2Tracks = Arrays.asList(Urn.forTrack(3), Urn.forTrack(4));
        final ApiPlaylist newPlaylist1 = ModelFixtures.create(ApiPlaylist.class);
        final ApiPlaylist newPlaylist2 = ModelFixtures.create(ApiPlaylist.class);

        when(loadLocalPlaylists.call()).thenReturn(playlists);
        when(loadPlaylistTrackUrns.call()).thenReturn(playlist1Tracks, playlist2Tracks);
        when(apiClient
                .fetchMappedResponse(argThat(isApiRequestTo("POST", ApiEndpoints.PLAYLISTS_CREATE.path())
                                                                     .withContent(createPushRequestBody(playlists.get(0), playlist1Tracks))), eq(ApiPlaylistWrapper.class)))
                .thenReturn(new ApiPlaylistWrapper(newPlaylist1));
        when(apiClient
                .fetchMappedResponse(argThat(isApiRequestTo("POST", ApiEndpoints.PLAYLISTS_CREATE.path())
                                                                     .withContent(createPushRequestBody(playlists.get(1), playlist2Tracks))), eq(ApiPlaylistWrapper.class)))
                .thenReturn(new ApiPlaylistWrapper(newPlaylist2));

        syncer.call();

        verify(replacePlaylist, times(2)).call();
        assertThat(replacePlaylist.getInput()).isEqualTo(Pair.create(playlists.get(1).getUrn(), newPlaylist2)); // todo, check in put on first item too
    }

    @Test
    public void shouldPublishPlaylistCreatedEvent() throws Exception {
        final ApiPlaylist newPlaylist = setupNewPlaylistCreation();
        ArgumentCaptor<UIEvent> captor = ArgumentCaptor.forClass(UIEvent.class);

        syncer.call();

        verify(eventBus).publish(eq(EventQueue.TRACKING), captor.capture());
        UIEvent event = captor.getValue();
        assertThat(event.kind()).isEqualTo(UIEvent.Kind.CREATE_PLAYLIST);
        assertThat(event.playableTitle().get()).isEqualTo(newPlaylist.getTitle());
        assertThat(event.playableUrn().get()).isEqualTo(newPlaylist.getUrn());
    }

    @Test
    public void shouldPublishPlaylistPushedEventAfterReplacingPlaylist() throws Exception {
        final ApiPlaylist newPlaylist = setupNewPlaylistCreation();
        ArgumentCaptor<EntityStateChangedEvent> captor = ArgumentCaptor.forClass(EntityStateChangedEvent.class);

        syncer.call();

        InOrder inOrder = Mockito.inOrder(replacePlaylist, eventBus);
        inOrder.verify(replacePlaylist).call();
        inOrder.verify(eventBus).publish(eq(EventQueue.ENTITY_STATE_CHANGED), captor.capture());

        EntityStateChangedEvent event = captor.getValue();
        assertThat(event.getKind()).isEqualTo(EntityStateChangedEvent.PLAYLIST_PUSHED_TO_SERVER);
        assertThat(event.getChangeMap().get(localPlaylistUrn).get(EntityProperty.URN)).isEqualTo(newPlaylist.getUrn());
    }

    @Test
    public void shouldCallSyncerWithListOfUrnsPosted() throws Exception {
        final ApiPlaylist newPlaylist = setupNewPlaylistCreation();

        syncer.call();

        verify(postsSyncer).call(singletonList(newPlaylist.getUrn()));
    }

    @Test
    public void deleteLocallyPendingRemovalOnSuccess() throws Exception {
        Urn playlist = setupPlaylistRemoval(TestApiResponses.ok());

        syncer.call();

        verify(removePlaylistCommand).call(playlist);
    }

    @Test
    public void deleteLocallyPendingRemovalIfNotFound() throws Exception {
        Urn playlist = setupPlaylistRemoval(TestApiResponses.status(404));

        syncer.call();

        verify(removePlaylistCommand).call(playlist);
    }

    @Test
    public void doNotDeleteLocallyPendingRemovalIfRequestFailed() throws Exception {
        setupPlaylistRemoval(TestApiResponses.networkError());

        syncer.call();

        verifyZeroInteractions(removePlaylistCommand);
    }

    @Test
    public void syncsOfflinePlaylists() throws Exception {
        final Urn offlinePlaylist = Urn.forPlaylist(1);
        when(postsSyncer.call(anyListOf(Urn.class))).thenReturn(true);
        when(loadOfflinePlaylistsCommand.call(null)).thenReturn(singletonList(offlinePlaylist));
        when(singlePlaylistSyncerFactory.create(offlinePlaylist)).thenReturn(singlePlaylistSyncer);
        when(singlePlaylistSyncer.call()).thenReturn(true);

        final Boolean syncResult = syncer.call();

        verify(singlePlaylistSyncer).call();
        assertThat(syncResult).isTrue();
    }

    @Test
    public void sendsOfflinePlaylistSyncedEvent() throws Exception {
        final Urn offlinePlaylist = Urn.forPlaylist(1);
        final Urn offlinePlaylistWithNoChange = Urn.forPlaylist(2);
        final SinglePlaylistSyncer syncWithNoChange = mock(SinglePlaylistSyncer.class);
        when(syncWithNoChange.call()).thenReturn(false);

        when(postsSyncer.call(anyListOf(Urn.class))).thenReturn(true);
        when(loadOfflinePlaylistsCommand.call(null)).thenReturn(Arrays.asList(offlinePlaylist, offlinePlaylistWithNoChange));
        when(singlePlaylistSyncerFactory.create(offlinePlaylist)).thenReturn(singlePlaylistSyncer);
        when(singlePlaylistSyncerFactory.create(offlinePlaylistWithNoChange)).thenReturn(syncWithNoChange);
        when(singlePlaylistSyncer.call()).thenReturn(true);

        syncer.call();

        ArgumentCaptor<SyncJobResult> captor = ArgumentCaptor.forClass(SyncJobResult.class);

        verify(eventBus).publish(eq(EventQueue.SYNC_RESULT), captor.capture());
        SyncJobResult event = captor.getValue();
        assertThat(event.getAction()).isEqualTo(Syncable.PLAYLIST.name());
        assertThat(event.getUrns()).containsExactly(offlinePlaylist);
    }

    @Test
    public void syncsOfflinePlaylistAfterException() throws Exception {
        final Urn offlinePlaylistWithException = Urn.forPlaylist(1);
        final Urn offlinePlaylistToSync = Urn.forPlaylist(2);
        when(postsSyncer.call(anyListOf(Urn.class))).thenReturn(true);
        when(loadOfflinePlaylistsCommand.call(null)).thenReturn(Arrays.asList(offlinePlaylistWithException, offlinePlaylistToSync));

        final SinglePlaylistSyncer syncWithException = mock(SinglePlaylistSyncer.class);
        when(syncWithException.call()).thenThrow(new IOException("Test"));
        when(singlePlaylistSyncerFactory.create(offlinePlaylistWithException)).thenReturn(syncWithException);
        when(singlePlaylistSyncerFactory.create(offlinePlaylistToSync)).thenReturn(singlePlaylistSyncer);
        when(singlePlaylistSyncer.call()).thenReturn(true);

        final boolean syncResult = syncer.call();

        verify(syncWithException).call();
        verify(singlePlaylistSyncer).call();
        assertThat(syncResult).isTrue();
    }


    private Urn setupPlaylistRemoval(ApiResponse status) {
        final Urn playlist = Urn.forPlaylist(123L);
        when(loadPlaylistPendingRemovalCommand.call(null)).thenReturn(singletonList(playlist));
        when(apiClient.fetchResponse(argThat(isApiRequestTo("DELETE", ApiEndpoints.PLAYLISTS_DELETE.path(playlist))))).thenReturn(status);
        return playlist;
    }

    private ApiPlaylist setupNewPlaylistCreation() throws Exception {
        final List<PlaylistItem> playlists = ModelFixtures.create(PlaylistItem.class, 1);
        localPlaylistUrn = playlists.get(0).getUrn();
        final List<Urn> playlistTracks = Arrays.asList(Urn.forTrack(1), Urn.forTrack(2));
        final ApiPlaylist newPlaylist = ModelFixtures.create(ApiPlaylist.class);

        when(loadLocalPlaylists.call()).thenReturn(playlists);
        when(loadPlaylistTrackUrns.call()).thenReturn(playlistTracks);
        when(apiClient
                .fetchMappedResponse(argThat(isApiRequestTo("POST", ApiEndpoints.PLAYLISTS_CREATE.path())
                                                                     .withContent(createPushRequestBody(playlists.get(0), playlistTracks))), eq(ApiPlaylistWrapper.class)))
                .thenReturn(new ApiPlaylistWrapper(newPlaylist));
        return newPlaylist;
    }

    private Map<String, Object> createPushRequestBody(PlaylistItem apiPlaylist, List<Urn> playlistTracks) {
        final Map<String, Object> playlistBody = new ArrayMap<>(2);
        playlistBody.put("title", apiPlaylist.getTitle());
        playlistBody.put("public", apiPlaylist.isPublic());

        final Map<String, Object> requestBody = new ArrayMap<>(2);
        requestBody.put("playlist", playlistBody);
        requestBody.put("track_urns", Urns.toString(playlistTracks));
        return requestBody;
    }
}
