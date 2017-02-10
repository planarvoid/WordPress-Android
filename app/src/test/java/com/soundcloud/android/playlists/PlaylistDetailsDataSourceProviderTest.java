package com.soundcloud.android.playlists;


import static com.soundcloud.java.collections.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.collection.playlists.MyPlaylistsOperations;
import com.soundcloud.android.collection.playlists.PlaylistsOptions;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaylistEntityChangedEvent;
import com.soundcloud.android.events.PlaylistMarkedForOfflineStateChangedEvent;
import com.soundcloud.android.profile.ProfileApiMobile;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.observers.AssertableSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.util.List;

public class PlaylistDetailsDataSourceProviderTest extends AndroidUnitTest {

    private DataSourceProvider dataSourceProvider;

    @Mock private PlaylistRepository playlistRepository;
    @Mock private TrackRepository trackRepository;
    @Mock private FeatureFlags featureFlags;
    @Mock private AccountOperations accountOperations;
    @Mock private MyPlaylistsOperations myPlaylistOperations;
    @Mock private ProfileApiMobile profileApiMobile;

    private TestEventBus eventBus = new TestEventBus();

    private Playlist playlist = ModelFixtures.playlist();
    private Playlist updatedPlaylist = playlist.toBuilder().title("new-title").build();
    private Playlist pushedPlaylist = ModelFixtures.playlist(); // new urn
    private final ApiPlaylist otherApiPlaylist = ModelFixtures.create(ApiPlaylist.class);
    private final Playlist otherPlaylist = Playlist.from(otherApiPlaylist);
    private final ApiPlaylistPost playlistPost = new ApiPlaylistPost(otherApiPlaylist);
    private final ModelCollection<ApiPlaylistPost> userPlaylistCollection = new ModelCollection<>(newArrayList(playlistPost),"next-href");

    private Track track1 = ModelFixtures.track();
    private Track track2 = ModelFixtures.track();
    private List<Track> trackItems = singletonList(track1);
    private List<Track> updatedTrackItems = asList(track1, track2);

    private PlaylistWithExtras initialPlaylistWithoutTracks = PlaylistWithExtras.create(playlist, Optional.absent());
    private PlaylistWithExtras initialPlaylistWithTrackExtras = PlaylistWithExtras.create(playlist, trackItems);
    private PlaylistWithExtras initialPlaylistWithTrackAndOtherExtras = PlaylistWithExtras.create(playlist, trackItems, singletonList(otherPlaylist));
    private PlaylistWithExtras updatedPlaylistWithoutTracks = PlaylistWithExtras.create(updatedPlaylist, Optional.absent());
    private PlaylistWithExtras updatedPlaylistWithExtras = PlaylistWithExtras.create(updatedPlaylist, updatedTrackItems, singletonList(otherPlaylist));
    private PlaylistWithExtras pushedPlaylistWithoutTracks = PlaylistWithExtras.create(pushedPlaylist, Optional.absent());
    private PlaylistWithExtras pushedPlaylistWithExtras = PlaylistWithExtras.create(pushedPlaylist, updatedTrackItems);

    private PublishSubject<List<Track>> tracklistSubject = PublishSubject.create();

    @Before
    public void setUp() throws Exception {
        when(playlistRepository.withUrn(playlist.urn())).thenReturn(just(playlist));
        when(trackRepository.forPlaylist(playlist.urn())).thenReturn(tracklistSubject);

        when(profileApiMobile.userPlaylists(playlist.creatorUrn())).thenReturn(just(new ModelCollection<>()));
        when(profileApiMobile.userPlaylists(pushedPlaylist.creatorUrn())).thenReturn(just(new ModelCollection<>()));
        when(myPlaylistOperations.myPlaylists(PlaylistsOptions.builder().showLikes(false).showPosts(true).build())).thenReturn(just(emptyList()));

        when(playlistRepository.withUrn(pushedPlaylist.urn())).thenReturn(just(pushedPlaylist));
        when(trackRepository.forPlaylist(pushedPlaylist.urn())).thenReturn(just(updatedTrackItems));

        dataSourceProvider = new DataSourceProvider(playlist.urn(),
                                                    Schedulers.immediate(),
                                                    playlistRepository,
                                                    trackRepository,
                                                    eventBus,
                                                    featureFlags,
                                                    accountOperations,
                                                    myPlaylistOperations,
                                                    profileApiMobile);
    }

    @Test
    public void emitsInitialPlaylistWithoutTracks() throws Exception {
        dataSourceProvider.connect();

        AssertableSubscriber<PlaylistWithExtras> test = dataSourceProvider.data().test();

        test.assertValues(initialPlaylistWithoutTracks)
            .assertNotCompleted();
    }

    @Test
    public void emitsInitialPlaylistWithTrackExtras() throws Exception {
        dataSourceProvider.connect();

        AssertableSubscriber<PlaylistWithExtras> test = dataSourceProvider.data().test();

        tracklistSubject.onNext(trackItems);

        assertInitialValues(test);
    }

    @Test
    public void emitsInitialPlaylistWithTrackAndOtherPlaylistExtrasForLoggedInUser() throws Exception {
        List<Playlist> myPlaylists = asList(playlist, otherPlaylist); // current playlist should be filtered out
        when(myPlaylistOperations.myPlaylists(PlaylistsOptions.builder().showLikes(false).showPosts(true).build())).thenReturn(just(myPlaylists));
        when(accountOperations.isLoggedInUser(playlist.creatorUrn())).thenReturn(true);

        dataSourceProvider.connect();

        AssertableSubscriber<PlaylistWithExtras> test = dataSourceProvider.data().test();

        tracklistSubject.onNext(trackItems);

        assertInitialValuesWithOtherPlaylist(test);
    }

    @Test
    public void emitsInitialPlaylistWithTrackAndOtherPlaylistExtrasForOtherUser() throws Exception {
        when(profileApiMobile.userPlaylists(playlist.creatorUrn())).thenReturn(just(userPlaylistCollection));

        dataSourceProvider.connect();

        AssertableSubscriber<PlaylistWithExtras> test = dataSourceProvider.data().test();

        tracklistSubject.onNext(trackItems);

        assertInitialValuesWithEmptyBeforeOtherPlaylist(test);
    }

    @Test
    public void emitsUpdatedPlaylistWithExtrasOnPlaylistUpdateEvent() throws Exception {
        when(playlistRepository.withUrn(playlist.urn())).thenReturn(just(playlist), just(updatedPlaylist));
        when(profileApiMobile.userPlaylists(playlist.creatorUrn())).thenReturn(just(userPlaylistCollection));

        dataSourceProvider.connect();

        AssertableSubscriber<PlaylistWithExtras> test = dataSourceProvider.data().test();
        tracklistSubject.onNext(trackItems);

        eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistEntityChangedEvent.forUpdate(singleton(updatedPlaylist)));
        tracklistSubject.onNext(updatedTrackItems);

        assertUpdatedValues(test);
    }

    @Test
    public void emitsUpdatedPlaylistWithExtrasOnPlaylistPushedEvent() throws Exception {
        dataSourceProvider.connect();

        AssertableSubscriber<PlaylistWithExtras> test = dataSourceProvider.data().test();
        tracklistSubject.onNext(trackItems);

        eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistEntityChangedEvent.fromPlaylistPushedToServer(playlist.urn(), pushedPlaylist));

        test.assertValues(initialPlaylistWithoutTracks, initialPlaylistWithTrackExtras, initialPlaylistWithTrackExtras, initialPlaylistWithTrackExtras, pushedPlaylistWithoutTracks, pushedPlaylistWithExtras)
            .assertNotCompleted();
    }

    @Test
    public void doesNotEmitAgainOnPlaylistMarkedOfflineEvent() throws Exception {
        dataSourceProvider.connect();

        AssertableSubscriber<PlaylistWithExtras> test = dataSourceProvider.data().test();
        tracklistSubject.onNext(trackItems);

        eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistMarkedForOfflineStateChangedEvent.fromPlaylistsUnmarkedForDownload(singletonList(updatedPlaylist.urn())));

        assertInitialValues(test);
    }

    @Test
    public void doesNotEmitAgainOnPlaylistEdited() throws Exception {
        dataSourceProvider.connect();

        AssertableSubscriber<PlaylistWithExtras> test = dataSourceProvider.data().test();
        tracklistSubject.onNext(trackItems);

        eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistEntityChangedEvent.fromPlaylistEdited(updatedPlaylist));

        assertInitialValues(test);
    }

    @Test
    public void doesNotEmitAgainOnPlaylistRemovedFromOfflineEvent() throws Exception {
        dataSourceProvider.connect();

        AssertableSubscriber<PlaylistWithExtras> test = dataSourceProvider.data().test();
        tracklistSubject.onNext(trackItems);

        eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistMarkedForOfflineStateChangedEvent.fromPlaylistsUnmarkedForDownload(singletonList(updatedPlaylist.urn())));

        assertInitialValues(test);
    }

    private void assertInitialValues(AssertableSubscriber<PlaylistWithExtras> test) {
        test.assertValues(initialPlaylistWithoutTracks, initialPlaylistWithTrackExtras)
            .assertNotCompleted();
    }

    private void assertInitialValuesWithOtherPlaylist(AssertableSubscriber<PlaylistWithExtras> test) {
        test.assertValues(initialPlaylistWithoutTracks, initialPlaylistWithTrackAndOtherExtras)
            .assertNotCompleted();
    }

    private void assertInitialValuesWithEmptyBeforeOtherPlaylist(AssertableSubscriber<PlaylistWithExtras> test) {
        test.assertValues(initialPlaylistWithoutTracks, initialPlaylistWithTrackAndOtherExtras)
            .assertNotCompleted();
    }

    private void assertUpdatedValues(AssertableSubscriber<PlaylistWithExtras> test) {
        test.assertValues(initialPlaylistWithoutTracks, initialPlaylistWithTrackAndOtherExtras, initialPlaylistWithTrackExtras, initialPlaylistWithTrackAndOtherExtras, updatedPlaylistWithoutTracks, updatedPlaylistWithExtras)
            .assertNotCompleted();
    }
}
