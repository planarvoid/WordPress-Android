package com.soundcloud.android.playlists;


import static com.soundcloud.android.events.PlaylistTrackCountChangedEvent.fromTrackAddedToPlaylist;
import static com.soundcloud.android.playlists.DataSourceProvider.STALE_TIME_MILLIS;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.optional.Optional.absent;
import static com.soundcloud.java.optional.Optional.of;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.collection.playlists.MyPlaylistsOperations;
import com.soundcloud.android.collection.playlists.PlaylistsOptions;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaylistEntityChangedEvent;
import com.soundcloud.android.events.PlaylistTrackCountChangedEvent;
import com.soundcloud.android.profile.ProfileApiMobile;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestSyncJobResults;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.view.ViewError;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.shadows.ShadowLog;
import rx.observers.AssertableSubscriber;
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
    @Mock private SyncInitiator syncInitiator;

    private TestEventBus eventBus = new TestEventBus();

    private Playlist playlist = ModelFixtures.playlist();
    private PublishSubject<Object> refreshSubject = PublishSubject.create();

    private Playlist updatedPlaylist = playlist.toBuilder().title("new-title").build();
    private Playlist pushedPlaylist = ModelFixtures.playlist(); // new urn
    private final ApiPlaylist otherApiPlaylist = ModelFixtures.create(ApiPlaylist.class);
    private final Playlist otherPlaylist = Playlist.from(otherApiPlaylist);
    private final ApiPlaylistPost playlistPost = new ApiPlaylistPost(otherApiPlaylist);
    private final ModelCollection<ApiPlaylistPost> userPlaylistCollection = new ModelCollection<>(newArrayList(playlistPost), "next-href");

    private Track track1 = ModelFixtures.track();
    private Track track2 = ModelFixtures.track();
    private List<Track> trackItems = singletonList(track1);
    private List<Track> updatedTrackItems = asList(track1, track2);
    //
    private PlaylistWithExtras initialPlaylistWithoutTracks = PlaylistWithExtras.create(playlist, absent(), emptyList());
    private PlaylistWithExtras initialPlaylistWithTracks = PlaylistWithExtras.create(playlist, of(trackItems), emptyList());
    private PlaylistWithExtras initialPlaylistWithUpdatedTracks = PlaylistWithExtras.create(playlist, of(updatedTrackItems), emptyList());
    private PlaylistWithExtras initialPlaylistWithTrackAndOtherExtras = PlaylistWithExtras.create(playlist, of(trackItems), singletonList(otherPlaylist));
    private PlaylistWithExtras updatedPlaylistWithoutTracks = PlaylistWithExtras.create(updatedPlaylist,absent(), emptyList());
    private PlaylistWithExtras updatedPlaylistWithTracks = PlaylistWithExtras.create(updatedPlaylist, of(updatedTrackItems), emptyList());
    private PlaylistWithExtras updatedPlaylistWithExtras = PlaylistWithExtras.create(updatedPlaylist, of(updatedTrackItems), singletonList(otherPlaylist));
    private PlaylistWithExtras pushedPlaylistWithoutTracks = PlaylistWithExtras.create(pushedPlaylist, absent());
    private PlaylistWithExtras pushedPlaylistWithTracks = PlaylistWithExtras.create(pushedPlaylist, of(updatedTrackItems), emptyList());
    private PlaylistWithExtras pushedPlaylistWithExtras = PlaylistWithExtras.create(pushedPlaylist, of(updatedTrackItems), singletonList(otherPlaylist));
    //
    private PublishSubject<List<Track>> tracklistSubject = PublishSubject.create();
    private PublishSubject<ModelCollection<ApiPlaylistPost>> otherUserOtherPlaylists = PublishSubject.create();
    private ApiRequestException apiRequestException = TestApiResponses.networkError().getFailure();
    private PublishSubject<List<Playlist>> myOtherPlaylists = PublishSubject.create();
    private PublishSubject<SyncJobResult> syncResultSubject = PublishSubject.create();

    //
    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
        when(playlistRepository.withUrn(playlist.urn())).thenReturn(just(playlist));
        when(playlistRepository.withUrn(pushedPlaylist.urn())).thenReturn(just(pushedPlaylist));

        when(trackRepository.forPlaylist(playlist.urn(), STALE_TIME_MILLIS)).thenReturn(tracklistSubject);
        when(trackRepository.forPlaylist(pushedPlaylist.urn(), STALE_TIME_MILLIS)).thenReturn(tracklistSubject);

        when(profileApiMobile.userPlaylists(playlist.creatorUrn())).thenReturn(otherUserOtherPlaylists);
        when(profileApiMobile.userPlaylists(pushedPlaylist.creatorUrn())).thenReturn(otherUserOtherPlaylists);

        when(myPlaylistOperations.myPlaylists(PlaylistsOptions.builder().showLikes(false).showPosts(true).build())).thenReturn(myOtherPlaylists);
        when(syncInitiator.syncPlaylist(playlist.urn())).thenReturn(syncResultSubject);

        dataSourceProvider = new DataSourceProvider(playlist.urn(),
                                                    refreshSubject,
                                                    playlistRepository,
                                                    trackRepository,
                                                    eventBus,
                                                    featureFlags,
                                                    accountOperations,
                                                    myPlaylistOperations,
                                                    profileApiMobile,
                                                    syncInitiator);
    }

    @Test
    public void emitsInitialPlaylistMetadataWithoutTracks() throws Exception {
        AssertableSubscriber<PlaylistWithExtrasState> test = dataSourceProvider.data().test();
        dataSourceProvider.connect();


        test.assertValues(
                PlaylistWithExtrasState.initialState(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithoutTracks)).build()
        );
    }

    @Test
    public void emitsInitialPlaylistMetadataThenTracks() throws Exception {
        AssertableSubscriber<PlaylistWithExtrasState> test = dataSourceProvider.data().test();
        dataSourceProvider.connect();

        tracklistSubject.onNext(trackItems);

        test.assertValues(
                PlaylistWithExtrasState.initialState(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithoutTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithTracks)).build()
        );
    }

    @Test
    public void emitsInitialPlaylistMetadataThenErrorForTracks() throws Exception {
        AssertableSubscriber<PlaylistWithExtrasState> test = dataSourceProvider.data().test();
        dataSourceProvider.connect();

        tracklistSubject.onError(apiRequestException);
        otherUserOtherPlaylists.onNext(userPlaylistCollection); // this should be ignored as we do not show this without tracks

        test.assertValues(
                PlaylistWithExtrasState.initialState(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithoutTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithoutTracks))
                                       .viewError(of(ViewError.from(apiRequestException))).build()
        );
    }

    @Test
    public void emitsInitialPlaylistMetadataThenTracksAndOtherPlaylists() throws Exception {
        AssertableSubscriber<PlaylistWithExtrasState> test = dataSourceProvider.data().test();
        dataSourceProvider.connect();

        tracklistSubject.onNext(trackItems);
        otherUserOtherPlaylists.onNext(userPlaylistCollection);

        test.assertValues(
                PlaylistWithExtrasState.initialState(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithoutTracks)).build(),
                // there are separate emissions here because other playlists emits an empty list first if its another user
                // since it is a remote fetch
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithTrackAndOtherExtras)).build()
        );
    }

    @Test
    public void emitsInitialPlaylistMetadataThenTracksAndOtherPlaylistsForLoggedInUser() throws Exception {
        when(accountOperations.isLoggedInUser(playlist.creatorUrn())).thenReturn(true);
        AssertableSubscriber<PlaylistWithExtrasState> test = dataSourceProvider.data().test();

        dataSourceProvider.connect();
        List<Playlist> myPlaylists = asList(playlist, otherPlaylist); // current playlist should be filtered out

        tracklistSubject.onNext(trackItems);
        myOtherPlaylists.onNext(myPlaylists);

        test.assertValues(
                PlaylistWithExtrasState.initialState(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithoutTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithTrackAndOtherExtras)).build()
        );
    }

    @Test
    public void emitsInitialPlaylistMetadataWithEverythingIfWeHaveOtherPlaylistsBeforeTracksCome() throws Exception {
        AssertableSubscriber<PlaylistWithExtrasState> test = dataSourceProvider.data().test();
        dataSourceProvider.connect();

        otherUserOtherPlaylists.onNext(userPlaylistCollection);
        tracklistSubject.onNext(trackItems);

        test.assertValues(
                PlaylistWithExtrasState.initialState(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithoutTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithTrackAndOtherExtras)).build()
        );
    }

    @Test
    public void emitsInitialPlaylistMetadataThenTracksAndIgnoresErrorOnOtherPlaylists() throws Exception {
        AssertableSubscriber<PlaylistWithExtrasState> test = dataSourceProvider.data().test();
        dataSourceProvider.connect();

        tracklistSubject.onNext(trackItems);
        otherUserOtherPlaylists.onError(apiRequestException); // this should be ignored as we do not show this without tracks

        test.assertValues(
                PlaylistWithExtrasState.initialState(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithoutTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithTracks)).build()
        );
    }


    @Test
    public void emitsEverythingAgainOnRefresh() throws Exception {
        when(playlistRepository.withUrn(playlist.urn())).thenReturn(just(playlist), just(updatedPlaylist));

        AssertableSubscriber<PlaylistWithExtrasState> test = dataSourceProvider.data().test();
        dataSourceProvider.connect();

        tracklistSubject.onNext(trackItems);
        otherUserOtherPlaylists.onNext(userPlaylistCollection);

        refreshSubject.onNext(null);

        syncResultSubject.onNext(TestSyncJobResults.successWithChange());
        syncResultSubject.onCompleted();

        tracklistSubject.onNext(updatedTrackItems);
        otherUserOtherPlaylists.onNext(userPlaylistCollection);

        test.assertValues(
                PlaylistWithExtrasState.initialState(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithoutTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithTrackAndOtherExtras)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithTrackAndOtherExtras)).isRefreshing(true).build(),
                PlaylistWithExtrasState.initialState(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(updatedPlaylistWithoutTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(updatedPlaylistWithTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(updatedPlaylistWithExtras)).build()
        );
    }

    @Test
    public void emitsEverythingAgainOnUrnUpdate() throws Exception {
        when(playlistRepository.withUrn(playlist.urn())).thenReturn(just(playlist), just(updatedPlaylist));

        AssertableSubscriber<PlaylistWithExtrasState> test = dataSourceProvider.data().test();
        dataSourceProvider.connect();


        tracklistSubject.onNext(trackItems);
        otherUserOtherPlaylists.onNext(userPlaylistCollection);

        eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistEntityChangedEvent.fromPlaylistPushedToServer(playlist.urn(), pushedPlaylist));

        tracklistSubject.onNext(updatedTrackItems);
        otherUserOtherPlaylists.onNext(userPlaylistCollection);

        test.assertValues(
                PlaylistWithExtrasState.initialState(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithoutTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithTrackAndOtherExtras)).build(),
                PlaylistWithExtrasState.initialState(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(pushedPlaylistWithoutTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(pushedPlaylistWithTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(pushedPlaylistWithExtras)).build()
        );
    }

    @Test
    public void reEmitsTracklistWhenItemAdded() throws Exception {
        when(trackRepository.forPlaylist(playlist.urn(), STALE_TIME_MILLIS)).thenReturn(just(trackItems), just(updatedTrackItems));
        AssertableSubscriber<PlaylistWithExtrasState> test = dataSourceProvider.data().test();

        dataSourceProvider.connect();

        PlaylistTrackCountChangedEvent trackAdded = fromTrackAddedToPlaylist(playlist.urn(), 2);
        eventBus.publish(EventQueue.PLAYLIST_CHANGED, trackAdded);

        test.assertValues(
                PlaylistWithExtrasState.initialState(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithoutTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithUpdatedTracks)).build()
        );
    }
}
