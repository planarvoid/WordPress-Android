package com.soundcloud.android.playlists;

import static com.soundcloud.android.playlists.AddTrackToPlaylistCommand.AddTrackToPlaylistParams;
import static com.soundcloud.android.playlists.RemoveTrackFromPlaylistCommand.RemoveTrackFromPlaylistParams;
import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static com.soundcloud.java.optional.Optional.of;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.empty;
import static rx.Observable.just;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiPlaylistPost;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.collection.playlists.MyPlaylistsOperations;
import com.soundcloud.android.collection.playlists.PlaylistsOptions;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.experiments.OtherPlaylistsByUserConfig;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaylistChangedEvent;
import com.soundcloud.android.events.PlaylistEntityChangedEvent;
import com.soundcloud.android.events.PlaylistTrackCountChangedEvent;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.playlists.EditPlaylistCommand.EditPlaylistCommandParams;
import com.soundcloud.android.profile.ProfileApiMobile;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestSyncJobResults;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.functions.Action0;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.util.ArrayList;
import java.util.List;

public class PlaylistOperationsTest extends AndroidUnitTest {

    private static final boolean IS_PRIVATE = true;
    private static final String NEW_TITLE = "new title";
    private PlaylistOperations operations;

    @Mock private SyncInitiator syncInitiator;
    @Mock private SyncInitiatorBridge syncInitiatorBridge;
    @Mock private PlaylistTracksStorage playlistTracksStorage;
    @Mock private PlaylistRepository playlistRepository;
    @Mock private TrackRepository trackRepository;
    @Mock private LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrns;
    @Mock private Action0 requestSystemSyncAction;
    @Mock private OfflineContentOperations offlineOperations;
    @Mock private AddTrackToPlaylistCommand addTrackToPlaylistCommand;
    @Mock private RemoveTrackFromPlaylistCommand removeTrackFromPlaylistCommand;
    @Mock private EditPlaylistCommand editPlaylistCommand;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private ProfileApiMobile profileApiMobile;
    @Mock private PlaylistUpsellOperations upsellOperations;
    @Mock private OtherPlaylistsByUserConfig otherPlaylistsByUserConfig;
    @Mock private MyPlaylistsOperations myPlaylistsOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private FeatureOperations featureOperations;

    @Captor private ArgumentCaptor<AddTrackToPlaylistParams> addTrackCommandParamsCaptor;
    @Captor private ArgumentCaptor<RemoveTrackFromPlaylistParams> removeTrackCommandParamsCaptor;
    @Captor private ArgumentCaptor<EditPlaylistCommandParams> editPlaylistCommandParamsCaptor;

    private final Playlist playlist = ModelFixtures.playlist();
    private final Track track1 = ModelFixtures.trackBuilder().build();
    private final Track track2 = ModelFixtures.trackBuilder().build();
    private final TrackItem trackItem1 = ModelFixtures.trackItem(track1);
    private final TrackItem trackItem2 = ModelFixtures.trackItem(track2);

    private final Urn trackUrn = Urn.forTrack(123L);
    private final List<Urn> newTrackList = asList(trackUrn);
    private TestEventBus eventBus;
    private TestSubscriber<Playlist> playlistSubscriber = new TestSubscriber<>();
    private TestSubscriber<PlaylistDetailsViewModel> viewModelSubscriber = new TestSubscriber<>();
    private PublishSubject<SyncJobResult> playlistSyncSubject = PublishSubject.create();

    private final ApiPlaylistPost playlistPost = new ApiPlaylistPost(ModelFixtures.create(ApiPlaylist.class));
    private final ModelCollection<ApiPlaylistPost> userPlaylistCollection = new ModelCollection<>(
            newArrayList(playlistPost),
            "next-href");

    private PlaylistDetailsViewModelCreator viewModelCreator;


    @Before
    public void setUp() {
        viewModelCreator = new PlaylistDetailsViewModelCreator(resources(), featureOperations, accountOperations, upsellOperations);
        when(upsellOperations.getUpsell(any(Playlist.class), anyList())).thenReturn(Optional.absent());
        when(featureOperations.isOfflineContentEnabled()).thenReturn(false);
        when(featureOperations.upsellOfflineContent()).thenReturn(false);

        eventBus = new TestEventBus();
        when(playlistRepository.withUrn(playlist.urn())).thenReturn(just(playlist));
        operations = new PlaylistOperations(Schedulers.immediate(),
                                            syncInitiator,
                                            playlistRepository,
                                            providerOf(loadPlaylistTrackUrns),
                                            playlistTracksStorage, trackRepository,
                                            addTrackToPlaylistCommand,
                                            removeTrackFromPlaylistCommand,
                                            editPlaylistCommand,
                                            syncInitiatorBridge,
                                            offlineContentOperations,
                                            eventBus,
                                            profileApiMobile,
                                            myPlaylistsOperations,
                                            accountOperations,
                                            otherPlaylistsByUserConfig,
                                            viewModelCreator,
                                            ModelFixtures.entityItemCreator());
        when(syncInitiator.requestSystemSyncAction()).thenReturn(requestSystemSyncAction);
        when(syncInitiator.syncPlaylist(playlist.urn())).thenReturn(playlistSyncSubject);
        when(otherPlaylistsByUserConfig.isEnabled()).thenReturn(true);
    }

    @Test
    public void trackUrnsForPlaybackReturnsTrackUrnsFromCommand() {
        final TestSubscriber<List<Urn>> observer = new TestSubscriber<>();
        final List<Urn> urnList = asList(Urn.forTrack(123L), Urn.forTrack(456L));
        when(loadPlaylistTrackUrns.toObservable()).thenReturn(just(urnList));

        operations.trackUrnsForPlayback(Urn.forPlaylist(123L)).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0)).isEqualTo(urnList);
        assertThat(observer.getOnCompletedEvents()).hasSize(1);
    }

    @Test
    public void loadsPlaylistWithTracksFromStorage() {
        when(trackRepository.forPlaylist(playlist.urn())).thenReturn(just(newArrayList(track1, track2)));
        when(playlistRepository.withUrn(playlist.urn())).thenReturn(just(playlist));

        operations.playlist(playlist.urn()).subscribe(playlistSubscriber);

        playlistSubscriber.assertReceivedOnNext(singletonList(playlist));
        playlistSubscriber.assertCompleted();
    }

    @Test
    public void playlistWithTracksAndRecosLoadsFromStorageAndOthersApi() {
        when(trackRepository.forPlaylist(this.playlist.urn())).thenReturn(Observable.just(newArrayList(track1, track2)));
        when(playlistRepository.withUrn(this.playlist.urn())).thenReturn(just(this.playlist));
        when(profileApiMobile.userPlaylists(this.playlist.creatorUrn())).thenReturn(just(userPlaylistCollection));
        when(upsellOperations.getUpsell(playlist, trackItems())).thenReturn(Optional.absent());

        operations.playlistWithTracksAndRecommendations(this.playlist.urn()).subscribe(viewModelSubscriber);

        final PlaylistDetailsViewModel initialModel = PlaylistDetailFixtures.create(resources(), playlist, asList(trackItem1, trackItem2));
        final PlaylistDetailsViewModel updatedModel = initialModel.toBuilder().otherPlaylists(createOtherPlaylistItem()).build();

        viewModelSubscriber.assertValues(initialModel, updatedModel);
        viewModelSubscriber.assertCompleted();
    }

    @Test
    public void playlistWithTracksAndRecosLoadsFromStorageAndOthersByLoggedInUserFromStorage() {
        Playlist playlistPostItem = Playlist.from(playlistPost.getApiPlaylist());

        when(accountOperations.isLoggedInUser(this.playlist.creatorUrn())).thenReturn(true);
        final List<Track> tracks = newArrayList(track1, track2);
        final List<TrackItem> trackItems = newArrayList(trackItem1, trackItem2);
        when(trackRepository.forPlaylist(this.playlist.urn())).thenReturn(Observable.just(tracks));
        when(playlistRepository.withUrn(this.playlist.urn())).thenReturn(just(this.playlist));
        when(upsellOperations.getUpsell(playlist, trackItems())).thenReturn(Optional.absent());

        when(myPlaylistsOperations.myPlaylists(PlaylistsOptions.builder().showLikes(false).showPosts(true).build()))
                .thenReturn(just(singletonList(playlistPostItem)));

        operations.playlistWithTracksAndRecommendations(this.playlist.urn()).subscribe(viewModelSubscriber);

        PlaylistDetailsViewModel expected = PlaylistDetailFixtures
                .createWithOtherMyPlaylists(resources(), playlist, asList(trackItem1, trackItem2), of(createOtherPlaylistItem()));

        viewModelSubscriber.assertValue(expected);
        viewModelSubscriber.assertCompleted();
    }

    @Test
    public void playlistWithTracksAndRecosLoadsFromStorageAndEmitsExceptionIfStillMissingMetadata() {
        when(playlistRepository.withUrn(this.playlist.urn())).thenReturn(empty(), empty());
        when(trackRepository.forPlaylist(this.playlist.urn())).thenReturn(Observable.just(newArrayList(track1, track2)));
        when(profileApiMobile.userPlaylists(this.playlist.creatorUrn())).thenReturn(just(userPlaylistCollection));
        when(upsellOperations.getUpsell(playlist, trackItems())).thenReturn(Optional.absent());

        operations.playlistWithTracksAndRecommendations(this.playlist.urn()).subscribe(viewModelSubscriber);

        playlistSubscriber.assertNoValues();
        playlistSyncSubject.onNext(TestSyncJobResults.successWithChange());
        viewModelSubscriber.assertError(PlaylistOperations.PlaylistMissingException.class);
    }

    @Test
    public void updatedPlaylistSyncsThenLoadsFromStorage() {
        final ArrayList<Track> tracks = newArrayList(track1, track2);
        final ArrayList<TrackItem> trackItems = newArrayList(trackItem1, trackItem2);
        when(trackRepository.forPlaylist(this.playlist.urn())).thenReturn(Observable.just(tracks));
        when(playlistRepository.withUrn(this.playlist.urn())).thenReturn(just(playlist));
        when(profileApiMobile.userPlaylists(this.playlist.creatorUrn())).thenReturn(just(userPlaylistCollection));
        when(upsellOperations.getUpsell(playlist, trackItems())).thenReturn(Optional.absent());

        operations.updatedPlaylistWithTracksAndRecommendations(this.playlist.urn()).subscribe(viewModelSubscriber);

        viewModelSubscriber.assertNoValues();
        playlistSyncSubject.onNext(TestSyncJobResults.successWithChange());
        playlistSyncSubject.onCompleted();

        final PlaylistDetailsViewModel initialModel = PlaylistDetailFixtures.create(resources(), playlist, asList(trackItem1, trackItem2));
        final PlaylistDetailsViewModel updatedModel = initialModel.toBuilder().otherPlaylists(createOtherPlaylistItem()).build();

        viewModelSubscriber.assertValues(initialModel, updatedModel);
        viewModelSubscriber.assertCompleted();
    }

    @Test
    public void loadsPlaylistAndSyncsBeforeEmittingIfPlaylistMetaDataMissing() {
        when(trackRepository.forPlaylist(playlist.urn())).thenReturn(just(newArrayList(
                track1,
                track2)));
        when(playlistRepository.withUrn(playlist.urn())).thenReturn(Observable.empty(), just(playlist));

        operations.playlist(playlist.urn()).subscribe(playlistSubscriber);

        playlistSubscriber.assertNoValues();
        playlistSyncSubject.onNext(TestSyncJobResults.successWithChange());
        playlistSyncSubject.onCompleted();
        playlistSubscriber.assertReceivedOnNext(singletonList(playlist));
        playlistSubscriber.assertCompleted();
    }

    @Test
    public void loadsPlaylistAndSyncsBeforeEmittingAPlaylistMissingExceptionIfPlaylistMetaDataStillMissing() {
        when(syncInitiator.syncPlaylist(playlist.urn())).thenReturn(just(SyncJobResult.success(
                Syncable.PLAYLIST.name(),
                true)));
        when(trackRepository.forPlaylist(playlist.urn())).thenReturn(Observable.just(newArrayList(
                track1,
                track2)));
        when(playlistRepository.withUrn(playlist.urn())).thenReturn(empty(), empty());

        operations.playlist(playlist.urn()).subscribe(playlistSubscriber);

        playlistSubscriber.assertNoValues();
        playlistSyncSubject.onNext(TestSyncJobResults.successWithChange());
        playlistSubscriber.assertError(PlaylistOperations.PlaylistMissingException.class);
    }

    @Test
    public void loadsLocalPlaylistAndRequestsMyPlaylistSyncWhenEmitting() {
        final List<Track> trackList = asList(track1, track2);
        final Playlist localPlaylist = ModelFixtures.playlistBuilder().urn(Urn.forTrack(-123L)).build();

        PublishSubject<Void> myPlaylistSyncSubject = PublishSubject.create();
        when(syncInitiatorBridge.refreshMyPlaylists()).thenReturn(myPlaylistSyncSubject);
        when(trackRepository.forPlaylist(localPlaylist.urn())).thenReturn(just(trackList));
        when(playlistRepository.withUrn(localPlaylist.urn())).thenReturn(just(localPlaylist));
        when(syncInitiator.syncPlaylist(localPlaylist.urn())).thenReturn(playlistSyncSubject);

        operations.playlist(localPlaylist.urn()).subscribe(playlistSubscriber);

        playlistSubscriber.assertReceivedOnNext(singletonList(localPlaylist));
        playlistSubscriber.assertCompleted();

        assertThat(myPlaylistSyncSubject.hasObservers()).isTrue();
        assertThat(playlistSyncSubject.hasObservers()).isFalse();
    }

    @Test
    public void shouldCreateNewPlaylistUsingCommand() {
        TestSubscriber<Urn> observer = new TestSubscriber<>();
        when(playlistTracksStorage.createNewPlaylist("title", true, Urn.forTrack(123))).thenReturn(just(Urn.forPlaylist(1L)));

        operations.createNewPlaylist("title", true, false, Urn.forTrack(123)).subscribe(observer);

        observer.assertReceivedOnNext(singletonList(Urn.forPlaylist(1L)));
    }

    @Test
    public void shouldMarkPlaylistForOfflineAfterCreatingPlaylist() throws Exception {
        TestSubscriber<Urn> observer = new TestSubscriber<>();
        Urn playlistUrn = Urn.forPlaylist(123);
        when(playlistTracksStorage.createNewPlaylist("title", true, Urn.forTrack(123))).thenReturn(just(playlistUrn));
        when(offlineContentOperations.makePlaylistAvailableOffline(playlistUrn)).thenReturn(Observable.just(null));

        operations.createNewPlaylist("title", true, true, Urn.forTrack(123)).subscribe(observer);

        observer.assertReceivedOnNext(singletonList(playlistUrn));
    }

    @Test
    public void shouldPublishEntityChangedEventAfterCreatingPlaylist() {
        TestSubscriber<Urn> observer = new TestSubscriber<>();
        final Urn localPlaylist = Urn.newLocalPlaylist();
        when(playlistTracksStorage.createNewPlaylist("title", true, Urn.forTrack(123))).thenReturn(just(localPlaylist));

        operations.createNewPlaylist("title", true, false, Urn.forTrack(123)).subscribe(observer);

        final UrnStateChangedEvent event = eventBus.lastEventOn(EventQueue.URN_STATE_CHANGED);
        assertThat(event.kind()).isEqualTo(UrnStateChangedEvent.Kind.ENTITY_CREATED);
        assertThat(event.urns().iterator().next()).isEqualTo(localPlaylist);
    }

    @Test
    public void shouldRequestSystemSyncAfterCreatingPlaylist() throws Exception {
        TestSubscriber<Urn> observer = new TestSubscriber<>();
        when(playlistTracksStorage.createNewPlaylist("title", true, Urn.forTrack(123))).thenReturn(just(Urn.forPlaylist(123)));

        operations.createNewPlaylist("title", true, false, Urn.forTrack(123)).subscribe(observer);

        verify(requestSystemSyncAction).call();
    }

    @Test
    public void shouldPublishEntityChangedEventAfterAddingATrackToPlaylist() {
        when(addTrackToPlaylistCommand.toObservable(any(AddTrackToPlaylistParams.class))).thenReturn(
                just(1));

        operations.addTrackToPlaylist(playlist.urn(), trackUrn).subscribe();

        verifyAddToPlaylistParams();

        final PlaylistChangedEvent event = eventBus.lastEventOn(EventQueue.PLAYLIST_CHANGED);
        assertThat(event.kind()).isEqualTo(PlaylistTrackCountChangedEvent.Kind.TRACK_ADDED);
        assertThat(event.changeMap().keySet().iterator().next()).isEqualTo(playlist.urn());
    }

    @Test
    public void shouldRequestSystemSyncAfterAddingATrackToPlaylist() {
        when(addTrackToPlaylistCommand.toObservable(any(AddTrackToPlaylistParams.class))).thenReturn(
                just(1));

        operations.addTrackToPlaylist(playlist.urn(), trackUrn).subscribe();

        verifyAddToPlaylistParams();
        verify(requestSystemSyncAction).call();
    }

    @Test
    public void shouldNotPublishEntityChangedEventWhenAddingTrackToPlaylistFailed() {
        when(addTrackToPlaylistCommand.toObservable(any(AddTrackToPlaylistParams.class)))
                .thenReturn(Observable.error(new Exception()));

        operations.addTrackToPlaylist(playlist.urn(), trackUrn).subscribe(new TestSubscriber<>());

        verifyAddToPlaylistParams();
        eventBus.verifyNoEventsOn(EventQueue.PLAYLIST_CHANGED);
    }

    @Test
    public void shouldPublishEntityChangedEventAfterRemovingTrackFromPlaylist() {
        when(removeTrackFromPlaylistCommand.toObservable(any(RemoveTrackFromPlaylistParams.class))).thenReturn(
                just(1));

        operations.removeTrackFromPlaylist(playlist.urn(), trackUrn).subscribe();

        verifyRemoveFromPlaylistParams();

        final PlaylistChangedEvent event = eventBus.lastEventOn(EventQueue.PLAYLIST_CHANGED);
        assertThat(event.kind()).isEqualTo(PlaylistTrackCountChangedEvent.Kind.TRACK_REMOVED);
        assertThat(event.changeMap().keySet().iterator().next()).isEqualTo(playlist.urn());
    }

    @Test
    public void shouldRequestSystemSyncAfterRemovingTrackFromPlaylist() {
        when(removeTrackFromPlaylistCommand.toObservable(any(RemoveTrackFromPlaylistParams.class))).thenReturn(
                just(1));

        operations.removeTrackFromPlaylist(playlist.urn(), trackUrn).subscribe();

        verifyRemoveFromPlaylistParams();
        verify(requestSystemSyncAction).call();
    }

    @Test
    public void shouldNotPublishEntityChangedEventWhenRemovingTrackFromPlaylistFailed() {
        when(removeTrackFromPlaylistCommand.toObservable(any(RemoveTrackFromPlaylistParams.class)))
                .thenReturn(Observable.error(new Exception()));

        operations.removeTrackFromPlaylist(playlist.urn(), trackUrn).subscribe(new TestSubscriber<>());

        verifyRemoveFromPlaylistParams();
        eventBus.verifyNoEventsOn(EventQueue.PLAYLIST_CHANGED);
    }

    @Test
    public void shouldPublishEntityChangedEventAfterEditingPlaylist() {
        when(editPlaylistCommand.toObservable(any(EditPlaylistCommandParams.class))).thenReturn(just(1));

        operations.editPlaylist(playlist.urn(), NEW_TITLE, IS_PRIVATE, asList(trackUrn)).subscribe();

        verifyEditPlaylistCommandParams();

        final PlaylistEntityChangedEvent event = (PlaylistEntityChangedEvent) eventBus.lastEventOn(EventQueue.PLAYLIST_CHANGED);
        assertThat(event.kind()).isEqualTo(PlaylistChangedEvent.Kind.PLAYLIST_EDITED);
        assertThat(event.changeMap().containsKey(playlist.urn())).isTrue();
        assertThat(event.changeMap().get(playlist.urn())).isEqualTo(playlist);
    }

    @Test
    public void shouldRequestSystemSyncAfterEditingPlaylist() {
        when(editPlaylistCommand.toObservable(any(EditPlaylistCommandParams.class))).thenReturn(just(1));

        operations.editPlaylist(playlist.urn(), NEW_TITLE, IS_PRIVATE, asList(trackUrn)).subscribe();

        verifyEditPlaylistCommandParams();
        assertThat(playlistSyncSubject.hasObservers()).isTrue();
    }

    @Test
    public void shouldNotPublishEntityChangedEventAfterEditingPlaylistFailed() {
        when(editPlaylistCommand.toObservable(any(EditPlaylistCommandParams.class)))
                .thenReturn(Observable.error(new Exception()));

        operations.editPlaylist(playlist.urn(), NEW_TITLE, IS_PRIVATE, asList(trackUrn))
                  .subscribe(new TestSubscriber<>());

        verifyEditPlaylistCommandParams();
        eventBus.verifyNoEventsOn(EventQueue.PLAYLIST_CHANGED);
    }

    private List<TrackItem> trackItems() {
        return asList(trackItem1, trackItem2);
    }

    private void verifyAddToPlaylistParams() {
        verify(addTrackToPlaylistCommand).toObservable(addTrackCommandParamsCaptor.capture());
        assertThat(addTrackCommandParamsCaptor.getValue().playlistUrn).isEqualTo(playlist.urn());
        assertThat(addTrackCommandParamsCaptor.getValue().trackUrn).isEqualTo(trackUrn);
    }

    private void verifyRemoveFromPlaylistParams() {
        verify(removeTrackFromPlaylistCommand).toObservable(removeTrackCommandParamsCaptor.capture());
        assertThat(removeTrackCommandParamsCaptor.getValue().playlistUrn).isEqualTo(playlist.urn());
        assertThat(removeTrackCommandParamsCaptor.getValue().trackUrn).isEqualTo(trackUrn);
    }

    private void verifyEditPlaylistCommandParams() {
        verify(editPlaylistCommand).toObservable(editPlaylistCommandParamsCaptor.capture());
        assertThat(editPlaylistCommandParamsCaptor.getValue().playlistUrn).isEqualTo(playlist.urn());
        assertThat(editPlaylistCommandParamsCaptor.getValue().trackList).isEqualTo(newTrackList);
        assertThat(editPlaylistCommandParamsCaptor.getValue().isPrivate).isEqualTo(IS_PRIVATE);
        assertThat(editPlaylistCommandParamsCaptor.getValue().playlistTitle).isEqualTo(NEW_TITLE);
    }

    private PlaylistDetailOtherPlaylistsItem createOtherPlaylistItem() {
        return new PlaylistDetailOtherPlaylistsItem(
                playlist.creatorName(), singletonList(ModelFixtures.playlistItem(playlistPost.getApiPlaylist())), false);
    }
}
