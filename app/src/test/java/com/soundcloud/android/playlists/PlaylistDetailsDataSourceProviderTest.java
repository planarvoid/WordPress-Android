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

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.collection.playlists.MyPlaylistsOperations;
import com.soundcloud.android.collection.playlists.PlaylistsOptions;
import com.soundcloud.android.configuration.experiments.OtherPlaylistsByUserConfig;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaylistEntityChangedEvent;
import com.soundcloud.android.events.PlaylistTrackCountChangedEvent;
import com.soundcloud.android.profile.ProfileApiMobile;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestSyncJobResults;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.android.view.ViewError;
import com.soundcloud.rx.eventbus.TestEventBus;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.subjects.MaybeSubject;
import io.reactivex.subjects.SingleSubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.shadows.ShadowLog;
import rx.observers.AssertableSubscriber;
import rx.subjects.PublishSubject;

import java.util.Collections;
import java.util.List;

public class PlaylistDetailsDataSourceProviderTest extends AndroidUnitTest {

    private DataSourceProvider dataSourceProvider;

    @Mock private PlaylistRepository playlistRepository;
    @Mock private TrackRepository trackRepository;
    @Mock private OtherPlaylistsByUserConfig otherPlaylistsByUserConfig;
    @Mock private AccountOperations accountOperations;
    @Mock private MyPlaylistsOperations myPlaylistOperations;
    @Mock private ProfileApiMobile profileApiMobile;
    @Mock private SyncInitiator syncInitiator;

    private TestEventBus eventBus = new TestEventBus();

    private Playlist playlist = ModelFixtures.playlist();
    private Playlist album = ModelFixtures.album();
    private PublishSubject<Void> refreshSubject = PublishSubject.create();

    private Playlist updatedPlaylist = playlist.toBuilder().title("new-title").build();
    private Playlist pushedPlaylist = ModelFixtures.playlist(); // new urn
    private final ApiPlaylist otherApiPlaylist = ModelFixtures.create(ApiPlaylist.class);
    private final ApiPlaylist otherApiAlbum = ModelFixtures.apiAlbum();
    private final Playlist otherPlaylist = Playlist.from(otherApiPlaylist);
    private final Playlist otherAlbum = Playlist.from(otherApiAlbum);
    private final ApiPlaylistPost playlistPost = new ApiPlaylistPost(otherApiPlaylist);
    private final ApiPlaylistPost albumPost = new ApiPlaylistPost(otherApiAlbum);
    private final ModelCollection<ApiPlaylistPost> userPlaylistCollection = new ModelCollection<>(newArrayList(playlistPost), "next-href");
    private final ModelCollection<ApiPlaylistPost> userPlaylistCollectionWithExtraAlbum = new ModelCollection<>(newArrayList(albumPost, playlistPost), "next-href");
    private final ModelCollection<ApiPlaylistPost> userAlbumsCollection = new ModelCollection<>(newArrayList(albumPost), "next-href");

    private Track track1 = ModelFixtures.track();
    private Track track2 = ModelFixtures.track();
    private List<Track> trackItems = singletonList(track1);
    private List<Track> updatedTrackItems = asList(track1, track2);
    //
    private PlaylistWithExtras initialAlbumWithoutTracks = PlaylistWithExtras.create(album, absent(), emptyList(), false);
    private PlaylistWithExtras initialAlbumWithTracks = PlaylistWithExtras.create(album, of(trackItems), emptyList(), false);
    private PlaylistWithExtras initialAlbumWithTrackAndOtherExtras = PlaylistWithExtras.create(album, of(trackItems), singletonList(otherAlbum), false);
    private PlaylistWithExtras initialPlaylistWithoutTracks = PlaylistWithExtras.create(playlist, absent(), emptyList(), false);
    private PlaylistWithExtras initialPlaylistWithTracks = PlaylistWithExtras.create(playlist, of(trackItems), emptyList(), false);
    private PlaylistWithExtras initialPlaylistWithUpdatedTracks = PlaylistWithExtras.create(playlist, of(updatedTrackItems), emptyList(), false);
    private PlaylistWithExtras initialPlaylistWithTrackAndOtherExtras = PlaylistWithExtras.create(playlist, of(trackItems), singletonList(otherPlaylist), false);
    private PlaylistWithExtras updatedPlaylistWithoutTracks = PlaylistWithExtras.create(updatedPlaylist, absent(), emptyList(), false);
    private PlaylistWithExtras updatedPlaylistWithTracks = PlaylistWithExtras.create(updatedPlaylist, of(updatedTrackItems), emptyList(), false);
    private PlaylistWithExtras updatedPlaylistWithTracksAndOtherExtras = PlaylistWithExtras.create(updatedPlaylist, of(updatedTrackItems), singletonList(otherPlaylist), false);
    private PlaylistWithExtras pushedPlaylistWithoutTracks = PlaylistWithExtras.create(pushedPlaylist, absent(), false);
    private PlaylistWithExtras pushedPlaylistWithTracks = PlaylistWithExtras.create(pushedPlaylist, of(updatedTrackItems), emptyList(), false);
    private PlaylistWithExtras pushedPlaylistWithExtras = PlaylistWithExtras.create(pushedPlaylist, of(updatedTrackItems), singletonList(otherPlaylist), false);
    //
    private SingleSubject<List<Track>> tracklistSubject;
    private PublishSubject<ModelCollection<ApiPlaylistPost>> otherUserOtherPlaylists = PublishSubject.create();
    private ApiRequestException apiRequestException = TestApiResponses.networkError().getFailure();
    private MaybeSubject<List<Playlist>> myOtherPlaylists = MaybeSubject.create();
    private SingleSubject<SyncJobResult> syncResultSubject = SingleSubject.create();

    //
    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
        tracklistSubject = SingleSubject.create();
        when(playlistRepository.withUrn(playlist.urn())).thenReturn(Maybe.just(playlist));
        when(playlistRepository.withUrn(pushedPlaylist.urn())).thenReturn(Maybe.just(pushedPlaylist));

        when(trackRepository.forPlaylist(playlist.urn(), STALE_TIME_MILLIS)).thenReturn(tracklistSubject);
        when(trackRepository.forPlaylist(pushedPlaylist.urn(), STALE_TIME_MILLIS)).thenReturn(tracklistSubject);

        when(profileApiMobile.userPlaylists(playlist.creatorUrn())).thenReturn(otherUserOtherPlaylists);
        when(profileApiMobile.userPlaylists(pushedPlaylist.creatorUrn())).thenReturn(otherUserOtherPlaylists);

        when(myPlaylistOperations.myPlaylists(PlaylistsOptions.builder().showLikes(false).showPosts(true).build())).thenReturn(myOtherPlaylists);
        when(syncInitiator.syncPlaylist(playlist.urn())).thenReturn(syncResultSubject);

        when(otherPlaylistsByUserConfig.isEnabled()).thenReturn(true);

        dataSourceProvider = new DataSourceProvider(
                playlistRepository,
                trackRepository,
                eventBus,
                otherPlaylistsByUserConfig,
                accountOperations,
                myPlaylistOperations,
                profileApiMobile,
                syncInitiator);
    }

    @Test
    public void emitsInitialPlaylistMetadataWithoutTracks() throws Exception {

        dataSourceProvider.dataWith(playlist.urn(), refreshSubject).test()
                          .assertValues(
                                  PlaylistWithExtrasState.initialState(),
                                  PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithoutTracks)).build()
                          );
    }

    @Test
    public void emitsInitialPlaylistMetadataThenTracks() throws Exception {
        AssertableSubscriber<PlaylistWithExtrasState> test = dataSourceProvider.dataWith(playlist.urn(), refreshSubject).test();

        tracklistSubject.onSuccess(trackItems);

        test.assertValues(
                PlaylistWithExtrasState.initialState(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithoutTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithTracks)).build()
        );
    }

    @Test
    public void emitsInitialPlaylistMetadataThenErrorForTracks() throws Exception {
        AssertableSubscriber<PlaylistWithExtrasState> test = dataSourceProvider.dataWith(playlist.urn(), refreshSubject).test();

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
        AssertableSubscriber<PlaylistWithExtrasState> test = dataSourceProvider.dataWith(playlist.urn(), refreshSubject).test();

        tracklistSubject.onSuccess(trackItems);
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
    public void emitsInitialPlaylistMetadataThenTracksAndOtherPlaylistsFiltered() throws Exception {
        AssertableSubscriber<PlaylistWithExtrasState> test = dataSourceProvider.dataWith(playlist.urn(), refreshSubject).test();

        tracklistSubject.onSuccess(trackItems);
        otherUserOtherPlaylists.onNext(userPlaylistCollectionWithExtraAlbum);

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
    public void emitsInitialPlaylistMetadataThenTracksAndOtherAlbums() throws Exception {
        when(playlistRepository.withUrn(album.urn())).thenReturn(Maybe.just(album));
        when(profileApiMobile.userAlbums(album.creatorUrn())).thenReturn(otherUserOtherPlaylists);
        when(trackRepository.forPlaylist(album.urn(), STALE_TIME_MILLIS)).thenReturn(tracklistSubject);


        dataSourceProvider = new DataSourceProvider(
                playlistRepository,
                trackRepository,
                eventBus,
                otherPlaylistsByUserConfig,
                accountOperations,
                myPlaylistOperations,
                profileApiMobile,
                syncInitiator);

        AssertableSubscriber<PlaylistWithExtrasState> test = dataSourceProvider.dataWith(album.urn(), refreshSubject).test();

        tracklistSubject.onSuccess(trackItems);
        otherUserOtherPlaylists.onNext(userAlbumsCollection);

        test.assertValues(
                PlaylistWithExtrasState.initialState(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialAlbumWithoutTracks)).build(),
                // there are separate emissions here because other playlists emits an empty list first if its another user
                // since it is a remote fetch
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialAlbumWithTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialAlbumWithTrackAndOtherExtras)).build()
        );
    }

    @Test
    public void emitsInitialPlaylistMetadataThenTracksAndOtherPlaylistsForLoggedInUser() throws Exception {
        final PlaylistWithExtras initialPlaylist = initialPlaylistWithoutTracks.toBuilder().isLoggedInUserOwner(true).build();
        final PlaylistWithExtras playlistWithOther = initialPlaylistWithTrackAndOtherExtras.toBuilder().isLoggedInUserOwner(true).build();

        when(accountOperations.isLoggedInUser(playlist.creatorUrn())).thenReturn(true);
        AssertableSubscriber<PlaylistWithExtrasState> test = dataSourceProvider.dataWith(playlist.urn(), refreshSubject).test();

        List<Playlist> myPlaylists = asList(playlist, otherPlaylist); // current playlist should be filtered out

        tracklistSubject.onSuccess(trackItems);
        myOtherPlaylists.onSuccess(myPlaylists);

        test.assertValues(
                PlaylistWithExtrasState.initialState(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylist)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(playlistWithOther)).build()
        );
    }

    @Test
    public void emitsInitialPlaylistMetadataThenTracksAndOtherPlaylistsForLoggedInUserWithOnePlaylist() throws Exception {
        final PlaylistWithExtras playlistWithoutTracks = initialPlaylistWithoutTracks.toBuilder().isLoggedInUserOwner(true).build();
        final PlaylistWithExtras.Builder playlistWithTracks = initialPlaylistWithTracks.toBuilder().isLoggedInUserOwner(true);

        when(accountOperations.isLoggedInUser(playlist.creatorUrn())).thenReturn(true);
        AssertableSubscriber<PlaylistWithExtrasState> test = dataSourceProvider.dataWith(playlist.urn(), refreshSubject).test();

        List<Playlist> myPlaylists = Collections.singletonList(playlist); // current playlist should be filtered out

        tracklistSubject.onSuccess(trackItems);
        myOtherPlaylists.onSuccess(myPlaylists);

        test.assertValues(
                PlaylistWithExtrasState.initialState(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(playlistWithoutTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(playlistWithTracks.build())).build()
        );
    }

    @Test
    public void emitsInitialPlaylistMetadataWithEverythingIfWeHaveOtherPlaylistsBeforeTracksCome() throws Exception {
        AssertableSubscriber<PlaylistWithExtrasState> test = dataSourceProvider.dataWith(playlist.urn(), refreshSubject).test();

        otherUserOtherPlaylists.onNext(userPlaylistCollection);
        tracklistSubject.onSuccess(trackItems);

        test.assertValues(
                PlaylistWithExtrasState.initialState(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithoutTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithTrackAndOtherExtras)).build()
        );
    }

    @Test
    public void emitsInitialPlaylistMetadataThenTracksAndIgnoresErrorOnOtherPlaylists() throws Exception {
        AssertableSubscriber<PlaylistWithExtrasState> test = dataSourceProvider.dataWith(playlist.urn(), refreshSubject).test();

        tracklistSubject.onSuccess(trackItems);
        otherUserOtherPlaylists.onError(apiRequestException); // this should be ignored as we do not show this without tracks

        test.assertValues(
                PlaylistWithExtrasState.initialState(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithoutTracks)).build(),
                // Initial "Eager" Emission
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithTracks)).build(),
                // Error Emission
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithTracks)).build()
        );
    }

    @Test
    public void emitsInitialPlaylistMetadataThenTracksWhenOtherPlaylistsByUserIsDisabled() throws Exception {
        when(otherPlaylistsByUserConfig.isEnabled()).thenReturn(false);
        AssertableSubscriber<PlaylistWithExtrasState> test = dataSourceProvider.dataWith(playlist.urn(), refreshSubject).test();

        tracklistSubject.onSuccess(trackItems);

        test.assertValues(
                PlaylistWithExtrasState.initialState(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithoutTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithTracks)).build()
        );
    }

    @Test
    public void emitsEverythingAgainOnRefresh() throws Exception {
        when(trackRepository.forPlaylist(playlist.urn(), STALE_TIME_MILLIS)).thenReturn(Single.just(trackItems), Single.just(updatedTrackItems));
        when(trackRepository.forPlaylist(pushedPlaylist.urn(), STALE_TIME_MILLIS)).thenReturn(Single.just(trackItems), Single.just(updatedTrackItems));
        when(playlistRepository.withUrn(playlist.urn())).thenReturn(Maybe.just(playlist), Maybe.just(updatedPlaylist));

        AssertableSubscriber<PlaylistWithExtrasState> test = dataSourceProvider.dataWith(playlist.urn(), refreshSubject).test();
        // 0, 1

        tracklistSubject.onSuccess(trackItems); // 3
        otherUserOtherPlaylists.onNext(userPlaylistCollection); // 4

        refreshSubject.onNext(null); // 5

        syncResultSubject.onSuccess(TestSyncJobResults.successWithChange());

        tracklistSubject.onSuccess(updatedTrackItems); // 6
        otherUserOtherPlaylists.onNext(userPlaylistCollection);

        System.out.println(test.getOnNextEvents());
        test.assertValues(
                PlaylistWithExtrasState.initialState(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithoutTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithTrackAndOtherExtras)).build(),

                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithTrackAndOtherExtras)).isRefreshing(true).build(),

                PlaylistWithExtrasState.builder().playlistWithExtras(of(updatedPlaylistWithoutTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(updatedPlaylistWithTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(updatedPlaylistWithTracksAndOtherExtras)).build()
        );
    }

    @Test
    public void emitsEverythingAgainOnUrnUpdate() throws Exception {
        when(trackRepository.forPlaylist(playlist.urn(), STALE_TIME_MILLIS)).thenReturn(Single.just(trackItems));
        when(trackRepository.forPlaylist(pushedPlaylist.urn(), STALE_TIME_MILLIS)).thenReturn(Single.just(updatedTrackItems));
        when(playlistRepository.withUrn(playlist.urn())).thenReturn(Maybe.just(playlist), Maybe.just(updatedPlaylist));

        AssertableSubscriber<PlaylistWithExtrasState> test = dataSourceProvider.dataWith(playlist.urn(), refreshSubject).test();

        tracklistSubject.onSuccess(trackItems);
        otherUserOtherPlaylists.onNext(userPlaylistCollection);

        eventBus.publish(EventQueue.PLAYLIST_CHANGED, PlaylistEntityChangedEvent.fromPlaylistPushedToServer(playlist.urn(), pushedPlaylist));

        tracklistSubject.onSuccess(updatedTrackItems);
        otherUserOtherPlaylists.onNext(userPlaylistCollection);

        test.assertValues(
                PlaylistWithExtrasState.initialState(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithoutTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(initialPlaylistWithTrackAndOtherExtras)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(pushedPlaylistWithoutTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(pushedPlaylistWithTracks)).build(),
                PlaylistWithExtrasState.builder().playlistWithExtras(of(pushedPlaylistWithExtras)).build()
        );
    }

    @Test
    public void reEmitsTracklistWhenItemAdded() throws Exception {
        when(trackRepository.forPlaylist(playlist.urn(), STALE_TIME_MILLIS)).thenReturn(Single.just(trackItems), Single.just(updatedTrackItems));
        AssertableSubscriber<PlaylistWithExtrasState> test = dataSourceProvider.dataWith(playlist.urn(), refreshSubject).test();

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
