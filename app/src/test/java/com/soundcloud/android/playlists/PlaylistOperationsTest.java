package com.soundcloud.android.playlists;

import static com.soundcloud.android.playlists.AddTrackToPlaylistCommand.AddTrackToPlaylistParams;
import static com.soundcloud.android.playlists.RemoveTrackFromPlaylistCommand.RemoveTrackFromPlaylistParams;
import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
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
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlaylistChangedEvent;
import com.soundcloud.android.events.PlaylistEntityChangedEvent;
import com.soundcloud.android.events.PlaylistTrackCountChangedEvent;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.playlists.EditPlaylistCommand.EditPlaylistCommandParams;
import com.soundcloud.android.profile.ProfileApiMobile;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestSyncJobResults;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
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

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
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
    @Mock private FeatureFlags featureFlags;
    @Mock private MyPlaylistsOperations myPlaylistsOperations;
    @Mock private AccountOperations accountOperations;

    @Captor private ArgumentCaptor<AddTrackToPlaylistParams> addTrackCommandParamsCaptor;
    @Captor private ArgumentCaptor<RemoveTrackFromPlaylistParams> removeTrackCommandParamsCaptor;
    @Captor private ArgumentCaptor<EditPlaylistCommandParams> editPlaylistCommandParamsCaptor;

    private final PlaylistItem playlist = ModelFixtures.playlistItem();
    private final TrackItem track1 = ModelFixtures.trackItem();
    private final TrackItem track2 = ModelFixtures.trackItem();
    private final Urn trackUrn = Urn.forTrack(123L);
    private final List<Urn> newTrackList = asList(trackUrn);
    private TestEventBus eventBus;
    private TestSubscriber<PlaylistWithTracks> playlistSubscriber = new TestSubscriber<>();
    private TestSubscriber<PlaylistDetailsViewModel> viewModelSubscriber = new TestSubscriber<>();
    private PublishSubject<SyncJobResult> playlistSyncSubject = PublishSubject.create();

    private final ApiPlaylistPost playlistPost = new ApiPlaylistPost(ModelFixtures.create(ApiPlaylist.class));
    private final ModelCollection<ApiPlaylistPost> userPlaylistCollection = new ModelCollection<>(
            newArrayList(playlistPost),
            "next-href");

    @Before
    public void setUp() {
        eventBus = new TestEventBus();
        when(playlistRepository.withUrn(playlist.getUrn())).thenReturn(just(playlist));
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
                                            upsellOperations,
                                            featureFlags);
        when(syncInitiator.requestSystemSyncAction()).thenReturn(requestSystemSyncAction);
        when(syncInitiator.syncPlaylist(any(Urn.class))).thenReturn(playlistSyncSubject);
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
        when(trackRepository.forPlaylist(playlist.getUrn())).thenReturn(just(newArrayList(track1, track2)));
        when(playlistRepository.withUrn(playlist.getUrn())).thenReturn(just(playlist));

        operations.playlist(playlist.getUrn()).subscribe(playlistSubscriber);

        playlistSubscriber.assertReceivedOnNext(singletonList(playlistWithTracks()));
        playlistSubscriber.assertCompleted();
    }

    @Test
    public void playlistWithTracksAndRecosLoadsFromStorageAndOthersApi() {
        PlaylistWithTracks playlistWithTracks = playlistWithTracks();
        List<PlaylistDetailItem> playlistDetailItems = new ArrayList<>(asList(new PlaylistDetailTrackItem(track1),
                                                                              new PlaylistDetailTrackItem(track2)));

        when(trackRepository.forPlaylist(playlist.getUrn())).thenReturn(Observable.just(newArrayList(track1,track2)));
        when(playlistRepository.withUrn(playlist.getUrn())).thenReturn(just(playlist));
        when(profileApiMobile.userPlaylists(playlist.getCreatorUrn())).thenReturn(just(userPlaylistCollection));
        when(upsellOperations.toListItems(playlistWithTracks)).thenReturn(playlistDetailItems);

        operations.playlistWithTracksAndRecommendations(playlist.getUrn(), false).subscribe(viewModelSubscriber);

        List<PlaylistDetailItem> itemsWithOthers = asList(new PlaylistDetailTrackItem(track1), new PlaylistDetailTrackItem(track2),
                                                          createOtherPlaylistItem(playlistWithTracks));

        viewModelSubscriber.assertReceivedOnNext(asList(createViewModel(playlistWithTracks, playlistDetailItems),
                                                        createViewModel(playlistWithTracks, itemsWithOthers)));
        viewModelSubscriber.assertCompleted();
    }

    @Test
    public void playlistWithTracksAndRecosLoadsFromStorageAndOthersByLoggedInUserFromStorage() {
        PlaylistWithTracks playlistWithTracks = playlistWithTracks();
        PlaylistItem playlistPostItem = PlaylistItem.from(playlistPost.getApiPlaylist());

        when(accountOperations.isLoggedInUser(playlist.getCreatorUrn())).thenReturn(true);
        List<PlaylistDetailItem> playlistDetailItems = new ArrayList<>(asList(new PlaylistDetailTrackItem(track1),
                                                                              new PlaylistDetailTrackItem(track2)));

        when(trackRepository.forPlaylist(playlist.getUrn())).thenReturn(Observable.just(newArrayList(track1,track2)));
        when(playlistRepository.withUrn(playlist.getUrn())).thenReturn(just(playlist));
        when(upsellOperations.toListItems(playlistWithTracks)).thenReturn(playlistDetailItems);

        when(myPlaylistsOperations.myPlaylists(PlaylistsOptions.builder().showLikes(false).showPosts(true).build()))
                .thenReturn(just(singletonList(playlistPostItem)));

        operations.playlistWithTracksAndRecommendations(playlist.getUrn(), false).subscribe(viewModelSubscriber);

        List<PlaylistDetailItem> itemsWithOthers = asList(new PlaylistDetailTrackItem(track1), new PlaylistDetailTrackItem(track2),
                                                          createOtherPlaylistItem(playlistWithTracks));

        viewModelSubscriber.assertReceivedOnNext(asList(createViewModel(playlistWithTracks, itemsWithOthers)));
        viewModelSubscriber.assertCompleted();
    }

    @Test
    public void playlistWithTracksAndRecosLoadsFromStorageAndEmitsExceptionIfStillMissingMetadata() {
        PlaylistWithTracks playlistWithTracks = playlistWithTracks();
        List<PlaylistDetailItem> playlistDetailItems = new ArrayList<>(asList(new PlaylistDetailTrackItem(track1),
                                                                              new PlaylistDetailTrackItem(track2)));

        when(playlistRepository.withUrn(playlist.getUrn())).thenReturn(empty(), empty());
        when(trackRepository.forPlaylist(playlist.getUrn())).thenReturn(Observable.just(newArrayList(track1,track2)));
        when(profileApiMobile.userPlaylists(playlist.getCreatorUrn())).thenReturn(just(userPlaylistCollection));
        when(upsellOperations.toListItems(playlistWithTracks)).thenReturn(playlistDetailItems);

        operations.playlistWithTracksAndRecommendations(playlist.getUrn(), false).subscribe(viewModelSubscriber);

        playlistSubscriber.assertNoValues();
        playlistSyncSubject.onNext(TestSyncJobResults.successWithChange());
        viewModelSubscriber.assertError(PlaylistOperations.PlaylistMissingException.class);
    }

    @Test
    public void playlistWithTracksAndRecosLoadsFromStorageAndBackfillsTracksFromApiIfTracksMissing() {
        PlaylistWithTracks playlistWithTracks = playlistWithTracks();
        List<PlaylistDetailItem> playlistDetailItems = new ArrayList<>(asList(new PlaylistDetailTrackItem(track1),
                                                                              new PlaylistDetailTrackItem(track2)));

        List<TrackItem> emptyTrackList = emptyList();
        when(trackRepository.forPlaylist(playlist.getUrn())).thenReturn(Observable.just(emptyTrackList), Observable.just(newArrayList(track1, track2)));
        when(playlistRepository.withUrn(playlist.getUrn())).thenReturn(just(playlist));
        when(profileApiMobile.userPlaylists(playlist.getCreatorUrn())).thenReturn(just(userPlaylistCollection));
        when(upsellOperations.toListItems(playlistWithTracks)).thenReturn(playlistDetailItems);

        operations.playlistWithTracksAndRecommendations(playlist.getUrn(), false).subscribe(viewModelSubscriber);

        viewModelSubscriber.assertReceivedOnNext(singletonList(createViewModel(new PlaylistWithTracks(playlist, emptyTrackList), emptyList())));

        playlistSyncSubject.onNext(TestSyncJobResults.successWithChange());
        playlistSyncSubject.onCompleted();

        List<PlaylistDetailItem> itemsWithOthers = asList(new PlaylistDetailTrackItem(track1), new PlaylistDetailTrackItem(track2),
                                                          createOtherPlaylistItem(playlistWithTracks));

        viewModelSubscriber.assertReceivedOnNext(asList(createViewModel(new PlaylistWithTracks(playlist,emptyTrackList), emptyList()),
                                                        createViewModel(playlistWithTracks, playlistDetailItems),
                                                        createViewModel(playlistWithTracks, itemsWithOthers)));
        viewModelSubscriber.assertCompleted();
    }

    @Test
    public void updatedPlaylistSyncsThenLoadsFromStorage() {
        PlaylistWithTracks playlistWithTracks = playlistWithTracks();
        List<PlaylistDetailItem> playlistDetailItems = new ArrayList<>(asList(new PlaylistDetailTrackItem(track1),
                                                                            new PlaylistDetailTrackItem(track2)));

        when(trackRepository.forPlaylist(playlist.getUrn())).thenReturn(Observable.just(newArrayList(track1,track2)));
        when(playlistRepository.withUrn(playlist.getUrn())).thenReturn(just(playlist));
        when(profileApiMobile.userPlaylists(playlist.getCreatorUrn())).thenReturn(just(userPlaylistCollection));
        when(upsellOperations.toListItems(playlistWithTracks)).thenReturn(playlistDetailItems);

        operations.updatedPlaylistWithTracksAndRecommendations(playlist.getUrn(), false).subscribe(viewModelSubscriber);

        viewModelSubscriber.assertNoValues();
        playlistSyncSubject.onNext(TestSyncJobResults.successWithChange());
        playlistSyncSubject.onCompleted();

        List<PlaylistDetailItem> itemsWithOthers = asList(new PlaylistDetailTrackItem(track1), new PlaylistDetailTrackItem(track2),
                                                          createOtherPlaylistItem(playlistWithTracks));

        viewModelSubscriber.assertReceivedOnNext(asList(createViewModel(playlistWithTracks, playlistDetailItems),
                                                        createViewModel(playlistWithTracks, itemsWithOthers)));
        viewModelSubscriber.assertCompleted();
    }

    @NonNull
    PlaylistDetailOtherPlaylistsItem createOtherPlaylistItem(PlaylistWithTracks playlistWithTracks) {
        return new PlaylistDetailOtherPlaylistsItem(
                playlistWithTracks.getCreatorName(), singletonList(PlaylistItem.from(playlistPost.getApiPlaylist())));
    }

    @NonNull
    PlaylistWithTracks playlistWithTracks() {
        return new PlaylistWithTracks(playlist, trackItems());
    }

    @Test
    public void loadsPlaylistAndSyncsBeforeEmittingIfPlaylistMetaDataMissing() {
        when(trackRepository.forPlaylist(playlist.getUrn())).thenReturn(just(newArrayList(
                track1,
                track2)));
        when(playlistRepository.withUrn(playlist.getUrn())).thenReturn(Observable.empty(), just(playlist));

        operations.playlist(playlist.getUrn()).subscribe(playlistSubscriber);

        playlistSubscriber.assertNoValues();
        playlistSyncSubject.onNext(TestSyncJobResults.successWithChange());
        playlistSyncSubject.onCompleted();
        playlistSubscriber.assertReceivedOnNext(singletonList(playlistWithTracks()));
        playlistSubscriber.assertCompleted();
    }

    @Test
    public void loadsPlaylistAndSyncsBeforeEmittingAPlaylistMissingExceptionIfPlaylistMetaDataStillMissing() {
        when(syncInitiator.syncPlaylist(playlist.getUrn())).thenReturn(just(SyncJobResult.success(
                Syncable.PLAYLIST.name(),
                true)));
        when(trackRepository.forPlaylist(playlist.getUrn())).thenReturn(Observable.just(newArrayList(
                track1,
                track2)));
        when(playlistRepository.withUrn(playlist.getUrn())).thenReturn(empty(), empty());

        operations.playlist(playlist.getUrn()).subscribe(playlistSubscriber);

        playlistSubscriber.assertNoValues();
        playlistSyncSubject.onNext(TestSyncJobResults.successWithChange());
        playlistSubscriber.assertError(PlaylistOperations.PlaylistMissingException.class);
    }

    @Test
    public void loadsPlaylistAndEmitsAgainAfterSyncIfNoTracksAvailable() {
        final List<TrackItem> emptyTrackList = Collections.emptyList();
        final List<TrackItem> trackList = asList(track1, track2);
        PublishSubject<SyncJobResult> syncSubject = PublishSubject.create();
        when(syncInitiator.syncPlaylist(playlist.getUrn())).thenReturn(syncSubject);
        when(trackRepository.forPlaylist(playlist.getUrn())).thenReturn(just(emptyTrackList), just(trackList));
        when(playlistRepository.withUrn(playlist.getUrn())).thenReturn(just(playlist));

        operations.playlist(playlist.getUrn()).subscribe(playlistSubscriber);

        playlistSubscriber.assertReceivedOnNext(singletonList(new PlaylistWithTracks(playlist,
                                                                                     emptyTrackList)));

        syncSubject.onNext(TestSyncJobResults.successWithChange());

        playlistSubscriber.assertReceivedOnNext(asList(new PlaylistWithTracks(playlist,
                                                                              emptyTrackList),
                                                       new PlaylistWithTracks(playlist,
                                                                          trackList)));
    }

    @Test
    public void loadsLocalPlaylistAndRequestsMyPlaylistSyncWhenEmitting() {
        final List<TrackItem> trackList = asList(track1, track2);
        final PlaylistItem localPlaylistItem = ModelFixtures.playlistItem(Urn.forTrack(-123L));

        PublishSubject<Void> myPlaylistSyncSubject = PublishSubject.create();
        when(syncInitiatorBridge.refreshMyPlaylists()).thenReturn(myPlaylistSyncSubject);
        when(trackRepository.forPlaylist(playlist.getUrn())).thenReturn(just(trackList));
        when(playlistRepository.withUrn(playlist.getUrn())).thenReturn(just(localPlaylistItem));

        operations.playlist(playlist.getUrn()).subscribe(playlistSubscriber);

        PlaylistWithTracks playlistWithTracks = new PlaylistWithTracks(localPlaylistItem, trackList);
        playlistSubscriber.assertReceivedOnNext(singletonList(playlistWithTracks));
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

        operations.addTrackToPlaylist(playlist.getUrn(), trackUrn).subscribe();

        verifyAddToPlaylistParams();

        final PlaylistChangedEvent event = eventBus.lastEventOn(EventQueue.PLAYLIST_CHANGED);
        assertThat(event.kind()).isEqualTo(PlaylistTrackCountChangedEvent.Kind.TRACK_ADDED);
        assertThat(event.changeMap().keySet().iterator().next()).isEqualTo(playlist.getUrn());
    }

    @Test
    public void shouldRequestSystemSyncAfterAddingATrackToPlaylist() {
        when(addTrackToPlaylistCommand.toObservable(any(AddTrackToPlaylistParams.class))).thenReturn(
                just(1));

        operations.addTrackToPlaylist(playlist.getUrn(), trackUrn).subscribe();

        verifyAddToPlaylistParams();
        verify(requestSystemSyncAction).call();
    }

    @Test
    public void shouldNotPublishEntityChangedEventWhenAddingTrackToPlaylistFailed() {
        when(addTrackToPlaylistCommand.toObservable(any(AddTrackToPlaylistParams.class)))
                .thenReturn(Observable.error(new Exception()));

        operations.addTrackToPlaylist(playlist.getUrn(), trackUrn).subscribe(new TestSubscriber<>());

        verifyAddToPlaylistParams();
        eventBus.verifyNoEventsOn(EventQueue.PLAYLIST_CHANGED);
    }

    @Test
    public void shouldPublishEntityChangedEventAfterRemovingTrackFromPlaylist() {
        when(removeTrackFromPlaylistCommand.toObservable(any(RemoveTrackFromPlaylistParams.class))).thenReturn(
                just(1));

        operations.removeTrackFromPlaylist(playlist.getUrn(), trackUrn).subscribe();

        verifyRemoveFromPlaylistParams();

        final PlaylistChangedEvent event = eventBus.lastEventOn(EventQueue.PLAYLIST_CHANGED);
        assertThat(event.kind()).isEqualTo(PlaylistTrackCountChangedEvent.Kind.TRACK_REMOVED);
        assertThat(event.changeMap().keySet().iterator().next()).isEqualTo(playlist.getUrn());
    }

    @Test
    public void shouldRequestSystemSyncAfterRemovingTrackFromPlaylist() {
        when(removeTrackFromPlaylistCommand.toObservable(any(RemoveTrackFromPlaylistParams.class))).thenReturn(
                just(1));

        operations.removeTrackFromPlaylist(playlist.getUrn(), trackUrn).subscribe();

        verifyRemoveFromPlaylistParams();
        verify(requestSystemSyncAction).call();
    }

    @Test
    public void shouldNotPublishEntityChangedEventWhenRemovingTrackFromPlaylistFailed() {
        when(removeTrackFromPlaylistCommand.toObservable(any(RemoveTrackFromPlaylistParams.class)))
                .thenReturn(Observable.error(new Exception()));

        operations.removeTrackFromPlaylist(playlist.getUrn(), trackUrn).subscribe(new TestSubscriber<>());

        verifyRemoveFromPlaylistParams();
        eventBus.verifyNoEventsOn(EventQueue.PLAYLIST_CHANGED);
    }

    @Test
    public void shouldPublishEntityChangedEventAfterEditingPlaylist() {
        when(editPlaylistCommand.toObservable(any(EditPlaylistCommandParams.class))).thenReturn(just(1));

        operations.editPlaylist(playlist.getUrn(), NEW_TITLE, IS_PRIVATE, asList(trackUrn)).subscribe();

        verifyEditPlaylistCommandParams();

        final PlaylistEntityChangedEvent event = (PlaylistEntityChangedEvent) eventBus.lastEventOn(EventQueue.PLAYLIST_CHANGED);
        assertThat(event.kind()).isEqualTo(PlaylistChangedEvent.Kind.PLAYLIST_EDITED);
        assertThat(event.changeMap().containsKey(playlist.getUrn())).isTrue();
        assertThat(event.changeMap().get(playlist.getUrn())).isEqualTo(playlist);
    }

    @Test
    public void shouldRequestSystemSyncAfterEditingPlaylist() {
        when(editPlaylistCommand.toObservable(any(EditPlaylistCommandParams.class))).thenReturn(just(1));

        operations.editPlaylist(playlist.getUrn(), NEW_TITLE, IS_PRIVATE, asList(trackUrn)).subscribe();

        verifyEditPlaylistCommandParams();
        verify(requestSystemSyncAction).call();
    }

    @Test
    public void shouldNotPublishEntityChangedEventAfterEditingPlaylistFailed() {
        when(editPlaylistCommand.toObservable(any(EditPlaylistCommandParams.class)))
                .thenReturn(Observable.error(new Exception()));

        operations.editPlaylist(playlist.getUrn(), NEW_TITLE, IS_PRIVATE, asList(trackUrn))
                  .subscribe(new TestSubscriber<>());

        verifyEditPlaylistCommandParams();
        eventBus.verifyNoEventsOn(EventQueue.PLAYLIST_CHANGED);
    }

    private List<TrackItem> trackItems() {
        return asList(track1, track2);
    }

    private void verifyAddToPlaylistParams() {
        verify(addTrackToPlaylistCommand).toObservable(addTrackCommandParamsCaptor.capture());
        assertThat(addTrackCommandParamsCaptor.getValue().playlistUrn).isEqualTo(playlist.getUrn());
        assertThat(addTrackCommandParamsCaptor.getValue().trackUrn).isEqualTo(trackUrn);
    }

    private void verifyRemoveFromPlaylistParams() {
        verify(removeTrackFromPlaylistCommand).toObservable(removeTrackCommandParamsCaptor.capture());
        assertThat(removeTrackCommandParamsCaptor.getValue().playlistUrn).isEqualTo(playlist.getUrn());
        assertThat(removeTrackCommandParamsCaptor.getValue().trackUrn).isEqualTo(trackUrn);
    }

    private void verifyEditPlaylistCommandParams() {
        verify(editPlaylistCommand).toObservable(editPlaylistCommandParamsCaptor.capture());
        assertThat(editPlaylistCommandParamsCaptor.getValue().playlistUrn).isEqualTo(playlist.getUrn());
        assertThat(editPlaylistCommandParamsCaptor.getValue().trackList).isEqualTo(newTrackList);
        assertThat(editPlaylistCommandParamsCaptor.getValue().isPrivate).isEqualTo(IS_PRIVATE);
        assertThat(editPlaylistCommandParamsCaptor.getValue().playlistTitle).isEqualTo(NEW_TITLE);
    }

    @NonNull
    PlaylistDetailsViewModel createViewModel(PlaylistWithTracks playlistWithTracks,
                                             List<PlaylistDetailItem> playlistDetailItems) {
        return new PlaylistDetailsViewModel(playlistWithTracks, playlistDetailItems);
    }
}
