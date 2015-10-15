package com.soundcloud.android.sync.posts;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClient;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayableMetadata;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.LoadPlaylistTrackUrnsCommand;
import com.soundcloud.android.sync.ApiSyncResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.PropertySets;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.net.Uri;
import android.support.v4.util.ArrayMap;
import android.util.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class MyPlaylistsSyncerTest extends AndroidUnitTest {

    private static final Uri URI = Uri.parse("/some/uri");

    private MyPlaylistsSyncer syncer;

    @Mock private PostsSyncer postsSyncer;
    @Mock private LoadLocalPlaylistsCommand loadLocalPlaylists;
    @Mock private LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrns;
    @Mock private ReplacePlaylistPostCommand replacePlaylist;
    @Mock private ApiClient apiClient;
    @Mock private EventBus eventBus;

    @Before
    public void setUp() throws Exception {
        syncer = new MyPlaylistsSyncer(postsSyncer, loadLocalPlaylists, loadPlaylistTrackUrns, replacePlaylist, apiClient, eventBus);
    }

    @Test
    public void shouldReturnChangedResultIfPostsSyncerReturnsTrue() throws Exception {
        when(postsSyncer.call()).thenReturn(true);
        final ApiSyncResult syncResult = syncer.syncContent(URI, null);
        assertThat(syncResult.change).isEqualTo(ApiSyncResult.CHANGED);
        assertThat(syncResult.uri).isEqualTo(URI);
    }

    @Test
    public void shouldReturnUnchangedResultIfPostsSyncerReturnsTrue() throws Exception {
        when(postsSyncer.call()).thenReturn(false);
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
        assertThat(event.get(PlayableMetadata.KEY_PLAYABLE_TITLE)).isEqualTo(newPlaylist.getTitle());
        assertThat(event.get(PlayableMetadata.KEY_PLAYABLE_URN)).isEqualTo(newPlaylist.getUrn().toString());
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
