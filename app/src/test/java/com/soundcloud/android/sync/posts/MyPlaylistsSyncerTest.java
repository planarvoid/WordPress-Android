package com.soundcloud.android.sync.posts;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.events.EntityMetadata;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.LoadPlaylistPendingRemovalCommand;
import com.soundcloud.android.playlists.LoadPlaylistTrackUrnsCommand;
import com.soundcloud.android.playlists.RemovePlaylistCommand;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.PropertySets;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.net.Uri;
import android.support.v4.util.ArrayMap;
import android.util.Pair;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public class MyPlaylistsSyncerTest extends AndroidUnitTest {

    private static final Uri URI = Uri.parse("/some/uri");

    private MyPlaylistsSyncer syncer;

    @Mock private PostsSyncer postsSyncer;
    @Mock private LoadLocalPlaylistsCommand loadLocalPlaylists;
    @Mock private LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrns;
    @Mock private ReplacePlaylistPostCommand replacePlaylist;
    @Mock private LoadPlaylistPendingRemovalCommand loadPlaylistPendingRemovalCommand;
    @Mock private RemovePlaylistCommand removePlaylistCommand;
    @Mock private ApiClient apiClient;
    @Mock private EventBus eventBus;

    @Before
    public void setUp() throws Exception {
        syncer = new MyPlaylistsSyncer(
                postsSyncer,
                loadLocalPlaylists,
                loadPlaylistTrackUrns,
                replacePlaylist,
                loadPlaylistPendingRemovalCommand,
                removePlaylistCommand,
                apiClient,
                eventBus
        );
    }

    @Test
    public void shouldReturnChangedResultIfPostsSyncerReturnsTrue() throws Exception {
        when(postsSyncer.call(anyListOf(Urn.class))).thenReturn(true);
        final ApiSyncResult syncResult = syncer.syncContent(URI, null);
        assertThat(syncResult.change).isEqualTo(ApiSyncResult.CHANGED);
        assertThat(syncResult.uri).isEqualTo(URI);
    }

    @Test
    public void shouldReturnUnchangedResultIfPostsSyncerReturnsTrue() throws Exception {
        when(postsSyncer.call(anyListOf(Urn.class))).thenReturn(false);
        final ApiSyncResult syncResult = syncer.syncContent(URI, null);
        assertThat(syncResult.change).isEqualTo(ApiSyncResult.UNCHANGED);
        assertThat(syncResult.uri).isEqualTo(URI);
    }

    @Test
    public void replacesOldPlaylistWithNewPlaylistAfterSuccessfulPush() throws Exception {
        final List<ApiPlaylist> playlists = ModelFixtures.create(ApiPlaylist.class, 2);
        final List<Urn> playlist1Tracks = Arrays.asList(Urn.forTrack(1), Urn.forTrack(2));
        final List<Urn> playlist2Tracks = Arrays.asList(Urn.forTrack(3), Urn.forTrack(4));
        final ApiPlaylist newPlaylist1 = ModelFixtures.create(ApiPlaylist.class);
        final ApiPlaylist newPlaylist2 = ModelFixtures.create(ApiPlaylist.class);

        when(loadLocalPlaylists.call()).thenReturn(PropertySets.toPropertySets(playlists));
        when(loadPlaylistTrackUrns.call()).thenReturn(playlist1Tracks, playlist2Tracks);
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("POST", ApiEndpoints.PLAYLISTS_CREATE.path())
                .withContent(createPushRequestBody(playlists.get(0), playlist1Tracks))), eq(ApiPlaylistWrapper.class)))
                .thenReturn(new ApiPlaylistWrapper(newPlaylist1));
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("POST", ApiEndpoints.PLAYLISTS_CREATE.path())
                .withContent(createPushRequestBody(playlists.get(1), playlist2Tracks))), eq(ApiPlaylistWrapper.class)))
                .thenReturn(new ApiPlaylistWrapper(newPlaylist2));

        syncer.syncContent(URI, null);

        verify(replacePlaylist, times(2)).call();
        assertThat(replacePlaylist.getInput()).isEqualTo(Pair.create(playlists.get(1).getUrn(), newPlaylist2)); // todo, check in put on first item too
    }

    @Test
    public void shouldPublishPlaylistCreatedEvent() throws Exception {
        final ApiPlaylist newPlaylist = setupNewPlaylistCreation();
        ArgumentCaptor<UIEvent> captor = ArgumentCaptor.forClass(UIEvent.class);

        syncer.syncContent(URI, null);

        verify(eventBus).publish(eq(EventQueue.TRACKING), captor.capture());
        UIEvent event = captor.getValue();
        assertThat(event.getKind()).isEqualTo(UIEvent.KIND_CREATE_PLAYLIST);
        assertThat(event.get(EntityMetadata.KEY_PLAYABLE_TITLE)).isEqualTo(newPlaylist.getTitle());
        assertThat(event.get(EntityMetadata.KEY_PLAYABLE_URN)).isEqualTo(newPlaylist.getUrn().toString());
    }

    @Test
    public void shouldPublishPlaylistPushedEventAfterReplacingPlaylist() throws Exception {
        final ApiPlaylist newPlaylist = setupNewPlaylistCreation();
        ArgumentCaptor<EntityStateChangedEvent> captor = ArgumentCaptor.forClass(EntityStateChangedEvent.class);

        syncer.syncContent(URI, null);

        InOrder inOrder = Mockito.inOrder(replacePlaylist, eventBus);
        inOrder.verify(replacePlaylist).call();
        inOrder.verify(eventBus).publish(eq(EventQueue.ENTITY_STATE_CHANGED), captor.capture());

        EntityStateChangedEvent event = captor.getValue();
        assertThat(event.getKind()).isEqualTo(EntityStateChangedEvent.PLAYLIST_PUSHED_TO_SERVER);
        assertThat(event.getChangeMap().get(newPlaylist.getUrn())).isEqualTo(newPlaylist.toPropertySet());
    }

    @Test
    public void shouldCallSyncerWithListOfUrnsPosted() throws Exception {
        final ApiPlaylist newPlaylist = setupNewPlaylistCreation();

        syncer.syncContent(URI, null);

        verify(postsSyncer).call(Collections.singletonList(newPlaylist.getUrn()));
    }

    @Test
    public void deleteLocallyPendingRemovalOnSuccess() throws Exception {
        Urn playlist = setupPlaylistRemoval(TestApiResponses.ok());

        syncer.syncContent(URI, null);

        verify(removePlaylistCommand).call(playlist);
    }

    @Test
    public void deleteLocallyPendingRemovalIfNotFound() throws Exception {
        Urn playlist = setupPlaylistRemoval(TestApiResponses.status(404));

        syncer.syncContent(URI, null);

        verify(removePlaylistCommand).call(playlist);
    }

    @Test
    public void doNotDeleteLocallyPendingRemovalIfRequestFailed() throws Exception {
        setupPlaylistRemoval(TestApiResponses.networkError());

        syncer.syncContent(URI, null);

        verifyZeroInteractions(removePlaylistCommand);
    }

    private Urn setupPlaylistRemoval(ApiResponse status) {
        final Urn playlist = Urn.forPlaylist(123L);
        when(loadPlaylistPendingRemovalCommand.call(null)).thenReturn(Collections.singletonList(playlist));
        when(apiClient.fetchResponse(argThat(isApiRequestTo("DELETE", ApiEndpoints.PLAYLISTS_DELETE.path(playlist)))))
                .thenReturn(status);
        return playlist;
    }

    private ApiPlaylist setupNewPlaylistCreation() throws Exception {
        final List<ApiPlaylist> playlists = ModelFixtures.create(ApiPlaylist.class, 1);
        final List<Urn> playlistTracks = Arrays.asList(Urn.forTrack(1), Urn.forTrack(2));
        final ApiPlaylist newPlaylist = ModelFixtures.create(ApiPlaylist.class);

        when(loadLocalPlaylists.call()).thenReturn(PropertySets.toPropertySets(playlists));
        when(loadPlaylistTrackUrns.call()).thenReturn(playlistTracks);
        when(apiClient.fetchMappedResponse(argThat(isApiRequestTo("POST", ApiEndpoints.PLAYLISTS_CREATE.path())
                .withContent(createPushRequestBody(playlists.get(0), playlistTracks))), eq(ApiPlaylistWrapper.class)))
                .thenReturn(new ApiPlaylistWrapper(newPlaylist));
        return newPlaylist;
    }

    private Map<String, Object> createPushRequestBody(ApiPlaylist apiPlaylist, List<Urn> playlistTracks) {
        final Map<String, Object> playlistBody = new ArrayMap<>(2);
        playlistBody.put("title", apiPlaylist.getTitle());
        playlistBody.put("public", apiPlaylist.isPublic());

        final Map<String, Object> requestBody = new ArrayMap<>(2);
        requestBody.put("playlist", playlistBody);
        requestBody.put("track_urns", Urns.toString(playlistTracks));
        return requestBody;
    }
}
