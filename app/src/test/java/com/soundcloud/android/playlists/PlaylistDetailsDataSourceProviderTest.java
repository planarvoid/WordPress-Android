package com.soundcloud.android.playlists;


import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaylistEntityChangedEvent;
import com.soundcloud.android.events.PlaylistMarkedForOfflineStateChangedEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.observers.AssertableSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.util.Collections;
import java.util.List;

public class PlaylistDetailsDataSourceProviderTest extends AndroidUnitTest {

    private NewPlaylistDetailsPresenter.DataSourceProvider dataSourceProvider;

    @Mock private PlaylistRepository playlistRepository;
    @Mock private TrackRepository trackRepository;

    private TestEventBus eventBus = new TestEventBus();

    private Playlist playlist = ModelFixtures.playlist();
    private Playlist updatedPlaylist = playlist.toBuilder().title("new-title").build();
    private Playlist pushedPlaylist = ModelFixtures.playlist(); // new urn
    private Track track1 = ModelFixtures.track();
    private Track track2 = ModelFixtures.track();
    private List<Track> trackItems = singletonList(track1);
    private List<Track> updatedTrackItems = asList(track1, track2);

    private NewPlaylistDetailsPresenter.PlaylistWithTracks initialPlaylistWithoutTracks = NewPlaylistDetailsPresenter.PlaylistWithTracks.create(playlist, Collections.emptyList());
    private NewPlaylistDetailsPresenter.PlaylistWithTracks initialPlaylistWithTracks = NewPlaylistDetailsPresenter.PlaylistWithTracks.create(playlist, trackItems);
    private NewPlaylistDetailsPresenter.PlaylistWithTracks updatedPlaylistWithoutTracks = NewPlaylistDetailsPresenter.PlaylistWithTracks.create(updatedPlaylist, Collections.emptyList());
    private NewPlaylistDetailsPresenter.PlaylistWithTracks updatedPlaylistWithTracks = NewPlaylistDetailsPresenter.PlaylistWithTracks.create(updatedPlaylist, updatedTrackItems);
    private NewPlaylistDetailsPresenter.PlaylistWithTracks pushedPlaylistWithoutTracks = NewPlaylistDetailsPresenter.PlaylistWithTracks.create(pushedPlaylist, emptyList());
    private NewPlaylistDetailsPresenter.PlaylistWithTracks pushedPlaylistWithTracks = NewPlaylistDetailsPresenter.PlaylistWithTracks.create(pushedPlaylist, updatedTrackItems);

    private PublishSubject<List<Track>> tracklistSubject = PublishSubject.create();

    @Before
    public void setUp() throws Exception {
        when(playlistRepository.withUrn(playlist.urn())).thenReturn(just(playlist), just(updatedPlaylist));
        when(trackRepository.forPlaylist(playlist.urn())).thenReturn(tracklistSubject);

        when(playlistRepository.withUrn(pushedPlaylist.urn())).thenReturn(just(pushedPlaylist));
        when(trackRepository.forPlaylist(pushedPlaylist.urn())).thenReturn(just(updatedTrackItems));

        dataSourceProvider = new NewPlaylistDetailsPresenter.DataSourceProvider(playlist.urn(), Schedulers.immediate(), playlistRepository, trackRepository, eventBus);
    }

    @Test
    public void emitsInitialPlaylistWithoutTracks() throws Exception {
        AssertableSubscriber<NewPlaylistDetailsPresenter.PlaylistWithTracks> test = dataSourceProvider.data().test();

        test.assertValues(initialPlaylistWithoutTracks)
            .assertNotCompleted();
    }

    @Test
    public void emitsInitialPlaylistWithTracks() throws Exception {
        AssertableSubscriber<NewPlaylistDetailsPresenter.PlaylistWithTracks> test = dataSourceProvider.data().test();

        tracklistSubject.onNext(trackItems);

        assertInitialValues(test);
    }

    @Test
    public void emitsUpdatedPlaylistWithTracksOnPlaylistUpdateEvent() throws Exception {
        AssertableSubscriber<NewPlaylistDetailsPresenter.PlaylistWithTracks> test = dataSourceProvider.data().test();
        tracklistSubject.onNext(trackItems);

        eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistEntityChangedEvent.forUpdate(singleton(updatedPlaylist)));
        tracklistSubject.onNext(updatedTrackItems);

        assertUpdatedValues(test);
    }

    @Test
    public void emitsUpdatedPlaylistWithTracksOnPlaylistPushedEvent() throws Exception {
        AssertableSubscriber<NewPlaylistDetailsPresenter.PlaylistWithTracks> test = dataSourceProvider.data().test();
        tracklistSubject.onNext(trackItems);

        eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistEntityChangedEvent.fromPlaylistPushedToServer(playlist.urn(), pushedPlaylist));

        test.assertValues(initialPlaylistWithoutTracks, initialPlaylistWithTracks, pushedPlaylistWithoutTracks, pushedPlaylistWithTracks)
            .assertNotCompleted();
    }

    @Test
    public void doesNotEmitAgainOnPlaylistMarkedOfflineEvent() throws Exception {
        AssertableSubscriber<NewPlaylistDetailsPresenter.PlaylistWithTracks> test = dataSourceProvider.data().test();
        tracklistSubject.onNext(trackItems);

        eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistMarkedForOfflineStateChangedEvent.fromPlaylistsUnmarkedForDownload(singletonList(updatedPlaylist.urn())));

        assertInitialValues(test);
    }

    @Test
    public void doesNotEmitAgainOnPlaylistEdited() throws Exception {
        AssertableSubscriber<NewPlaylistDetailsPresenter.PlaylistWithTracks> test = dataSourceProvider.data().test();
        tracklistSubject.onNext(trackItems);

        eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistEntityChangedEvent.fromPlaylistEdited(updatedPlaylist));

        assertInitialValues(test);
    }

    @Test
    public void doesNotEmitAgainOnPlaylistRemovedFromOfflineEvent() throws Exception {
        AssertableSubscriber<NewPlaylistDetailsPresenter.PlaylistWithTracks> test = dataSourceProvider.data().test();
        tracklistSubject.onNext(trackItems);

        eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistMarkedForOfflineStateChangedEvent.fromPlaylistsUnmarkedForDownload(singletonList(updatedPlaylist.urn())));

        assertInitialValues(test);
    }

    private void assertInitialValues(AssertableSubscriber<NewPlaylistDetailsPresenter.PlaylistWithTracks> test) {
        test.assertValues(initialPlaylistWithoutTracks, initialPlaylistWithTracks)
            .assertNotCompleted();
    }

    private void assertUpdatedValues(AssertableSubscriber<NewPlaylistDetailsPresenter.PlaylistWithTracks> test) {
        test.assertValues(initialPlaylistWithoutTracks, initialPlaylistWithTracks, updatedPlaylistWithoutTracks, updatedPlaylistWithTracks)
            .assertNotCompleted();
    }
}
