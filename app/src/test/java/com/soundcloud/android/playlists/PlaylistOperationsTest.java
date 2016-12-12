package com.soundcloud.android.playlists;

import static com.soundcloud.android.playlists.AddTrackToPlaylistCommand.AddTrackToPlaylistParams;
import static com.soundcloud.android.playlists.RemoveTrackFromPlaylistCommand.RemoveTrackFromPlaylistParams;
import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.empty;
import static rx.Observable.just;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.playlists.EditPlaylistCommand.EditPlaylistCommandParams;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncInitiatorBridge;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestSyncJobResults;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PlaylistOperationsTest extends AndroidUnitTest {

    private static final boolean IS_PRIVATE = true;
    private static final String NEW_TITLE = "new title";
    private PlaylistOperations operations;

    @Mock private SyncInitiator syncInitiator;
    @Mock private SyncInitiatorBridge syncInitiatorBridge;
    @Mock private PlaylistTracksStorage tracksStorage;
    @Mock private PlaylistStorage playlistStorage;
    @Mock private LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrns;
    @Mock private Action0 requestSystemSyncAction;
    @Mock private OfflineContentOperations offlineOperations;
    @Mock private AddTrackToPlaylistCommand addTrackToPlaylistCommand;
    @Mock private RemoveTrackFromPlaylistCommand removeTrackFromPlaylistCommand;
    @Mock private EditPlaylistCommand editPlaylistCommand;
    @Captor private ArgumentCaptor<AddTrackToPlaylistParams> addTrackCommandParamsCaptor;
    @Captor private ArgumentCaptor<RemoveTrackFromPlaylistParams> removeTrackCommandParamsCaptor;
    @Captor private ArgumentCaptor<EditPlaylistCommandParams> editPlaylistCommandParamsCaptor;

    private final ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
    private final PlaylistItem playlistItem = ModelFixtures.create(PlaylistItem.class);
    private final PropertySet track1 = ModelFixtures.create(ApiTrack.class).toPropertySet();
    private final PropertySet track2 = ModelFixtures.create(ApiTrack.class).toPropertySet();
    private final Urn trackUrn = Urn.forTrack(123L);
    private final List<Urn> newTrackList = Arrays.asList(trackUrn);
    private TestEventBus eventBus;
    private TestSubscriber<PlaylistWithTracks> testSubscriber = new TestSubscriber<>();
    private PublishSubject<SyncJobResult> playlistSyncSubject = PublishSubject.create();

    @Before
    public void setUp() {
        eventBus = new TestEventBus();
        operations = new PlaylistOperations(Schedulers.immediate(),
                                            syncInitiator,
                                            tracksStorage,
                                            playlistStorage,
                                            providerOf(loadPlaylistTrackUrns),
                                            addTrackToPlaylistCommand,
                                            removeTrackFromPlaylistCommand,
                                            editPlaylistCommand,
                                            syncInitiatorBridge,
                                            eventBus);
        when(syncInitiator.requestSystemSyncAction()).thenReturn(requestSystemSyncAction);
        when(syncInitiator.syncPlaylist(any(Urn.class))).thenReturn(playlistSyncSubject);
    }

    @Test
    public void trackUrnsForPlaybackReturnsTrackUrnsFromCommand() {
        final TestSubscriber<List<Urn>> observer = new TestSubscriber<>();
        final List<Urn> urnList = Arrays.asList(Urn.forTrack(123L), Urn.forTrack(456L));
        when(loadPlaylistTrackUrns.toObservable()).thenReturn(just(urnList));

        operations.trackUrnsForPlayback(Urn.forPlaylist(123L)).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0)).isEqualTo(urnList);
        assertThat(observer.getOnCompletedEvents()).hasSize(1);
    }

    @Test
    public void loadsPlaylistWithTracksFromStorage() {
        when(tracksStorage.playlistTracks(playlist.getUrn())).thenReturn(just(newArrayList(
                track1,
                track2)));
        when(playlistStorage.loadPlaylist(playlist.getUrn())).thenReturn(just(playlist.toPropertySet()));

        operations.playlist(playlist.getUrn()).subscribe(testSubscriber);

        testSubscriber.assertReceivedOnNext(singletonList(playlistWithTracks()));
        testSubscriber.assertCompleted();
    }

    @Test
    public void updatedPlaylistSyncsThenLoadsFromStorage() {
        when(tracksStorage.playlistTracks(playlist.getUrn())).thenReturn(Observable.just(newArrayList(
                track1,
                track2)));
        when(playlistStorage.loadPlaylist(playlist.getUrn())).thenReturn(just(playlist.toPropertySet()));

        operations.updatedPlaylistInfo(playlist.getUrn()).subscribe(testSubscriber);

        testSubscriber.assertNoValues();
        playlistSyncSubject.onNext(TestSyncJobResults.successWithChange());
        playlistSyncSubject.onCompleted();
        testSubscriber.assertReceivedOnNext(Collections.singletonList(playlistWithTracks()));
        testSubscriber.assertCompleted();
    }

    @NonNull
    PlaylistWithTracks playlistWithTracks() {
        return new PlaylistWithTracks(PlaylistItem.from(playlist), trackItems());
    }

    @Test
    public void loadsPlaylistAndSyncsBeforeEmittingIfPlaylistMetaDataMissing() {
        when(tracksStorage.playlistTracks(playlist.getUrn())).thenReturn(just(newArrayList(
                track1,
                track2)));
        when(playlistStorage.loadPlaylist(playlist.getUrn())).thenReturn(Observable.empty(), just(playlist.toPropertySet()));

        operations.playlist(playlist.getUrn()).subscribe(testSubscriber);

        testSubscriber.assertNoValues();
        playlistSyncSubject.onNext(TestSyncJobResults.successWithChange());
        playlistSyncSubject.onCompleted();
        testSubscriber.assertReceivedOnNext(Collections.singletonList(playlistWithTracks()));
        testSubscriber.assertCompleted();
    }

    @Test
    public void loadsPlaylistAndSyncsBeforeEmittingAPlaylistMissingExceptionIfPlaylistMetaDataStillMissing() {
        when(syncInitiator.syncPlaylist(playlist.getUrn())).thenReturn(just(SyncJobResult.success(
                Syncable.PLAYLIST.name(),
                true)));
        when(tracksStorage.playlistTracks(playlist.getUrn())).thenReturn(Observable.just(newArrayList(
                track1,
                track2)));
        when(playlistStorage.loadPlaylist(playlist.getUrn())).thenReturn(empty(),
                                                                         empty());

        operations.playlist(playlist.getUrn()).subscribe(testSubscriber);

        testSubscriber.assertNoValues();
        playlistSyncSubject.onNext(TestSyncJobResults.successWithChange());
        testSubscriber.assertError(PlaylistOperations.PlaylistMissingException.class);
    }

    @Test
    public void loadsPlaylistAndEmitsAgainAfterSyncIfNoTracksAvailable() {
        final List<PropertySet> emptyTrackList = Collections.emptyList();
        final List<PropertySet> trackList = Arrays.asList(track1, track2);
        PublishSubject<SyncJobResult> syncSubject = PublishSubject.create();
        when(syncInitiator.syncPlaylist(playlist.getUrn())).thenReturn(syncSubject);
        when(tracksStorage.playlistTracks(playlist.getUrn())).thenReturn(just(emptyTrackList),
                                                                         just(trackList));
        when(playlistStorage.loadPlaylist(playlist.getUrn())).thenReturn(just(playlist.toPropertySet()));

        operations.playlist(playlist.getUrn()).subscribe(testSubscriber);

        testSubscriber.assertReceivedOnNext(singletonList(new PlaylistWithTracks(PlaylistItem.from(playlist),
                                                                                 TrackItem.fromPropertySets()
                                                                                                .call(emptyTrackList))));

        syncSubject.onNext(TestSyncJobResults.successWithChange());

        testSubscriber.assertReceivedOnNext(Arrays.asList(new PlaylistWithTracks(PlaylistItem.from(playlist),
                                                                                 TrackItem.fromPropertySets()
                                                                                                .call(emptyTrackList)),
                                                          new PlaylistWithTracks(PlaylistItem.from(playlist),
                                                                                       TrackItem.fromPropertySets()
                                                                                                .call(trackList))));
    }

    @Test
    public void loadsLocalPlaylistAndRequestsMyPlaylistSyncWhenEmitting() {
        final List<PropertySet> trackList = Arrays.asList(track1, track2);
        final PropertySet playlistProperties = playlist.toPropertySet();
        playlistProperties.put(PlaylistProperty.URN, Urn.forTrack(-123L)); // make it a local playlist

        PublishSubject<Void> myPlaylistSyncSubject = PublishSubject.create();
        when(syncInitiatorBridge.refreshMyPlaylists()).thenReturn(myPlaylistSyncSubject);
        when(tracksStorage.playlistTracks(playlist.getUrn())).thenReturn(just(trackList));
        when(playlistStorage.loadPlaylist(playlist.getUrn())).thenReturn(just(playlistProperties));

        operations.playlist(playlist.getUrn()).subscribe(testSubscriber);

        PlaylistWithTracks playlistWithTracks = new PlaylistWithTracks(PlaylistItem.from(playlistProperties),
                                                      TrackItem.fromPropertySets().call(trackList));
        testSubscriber.assertReceivedOnNext(Collections.singletonList(playlistWithTracks));
        testSubscriber.assertCompleted();

        assertThat(myPlaylistSyncSubject.hasObservers()).isTrue();
        assertThat(playlistSyncSubject.hasObservers()).isFalse();
    }

    @Test
    public void shouldCreateNewPlaylistUsingCommand() {
        TestSubscriber<Urn> observer = new TestSubscriber<>();
        when(tracksStorage.createNewPlaylist("title",
                                             true,
                                             Urn.forTrack(123))).thenReturn(just(Urn.forPlaylist(1L)));

        operations.createNewPlaylist("title", true, Urn.forTrack(123)).subscribe(observer);

        observer.assertReceivedOnNext(Arrays.asList(Urn.forPlaylist(1L)));
    }

    @Test
    public void shouldPublishEntityChangedEventAfterCreatingPlaylist() {
        TestSubscriber<Urn> observer = new TestSubscriber<>();
        final Urn localPlaylist = Urn.newLocalPlaylist();
        when(tracksStorage.createNewPlaylist("title",
                                             true,
                                             Urn.forTrack(123))).thenReturn(just(localPlaylist));

        operations.createNewPlaylist("title", true, Urn.forTrack(123)).subscribe(observer);

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        assertThat(event.getKind()).isEqualTo(EntityStateChangedEvent.ENTITY_CREATED);
        assertThat(event.getFirstUrn()).isEqualTo(localPlaylist);
    }

    @Test
    public void shouldRequestSystemSyncAfterCreatingPlaylist() throws Exception {
        TestSubscriber<Urn> observer = new TestSubscriber<>();
        when(tracksStorage.createNewPlaylist("title",
                                             true,
                                             Urn.forTrack(123))).thenReturn(just(Urn.forPlaylist(123)));

        operations.createNewPlaylist("title", true, Urn.forTrack(123)).subscribe(observer);

        verify(requestSystemSyncAction).call();
    }

    @Test
    public void shouldPublishEntityChangedEventAfterAddingATrackToPlaylist() {
        when(addTrackToPlaylistCommand.toObservable(any(AddTrackToPlaylistParams.class))).thenReturn(
                just(1));

        operations.addTrackToPlaylist(playlist.getUrn(), trackUrn).subscribe();

        verifyAddToPlaylistParams();

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        assertThat(event.getKind()).isEqualTo(EntityStateChangedEvent.TRACK_ADDED_TO_PLAYLIST);
        assertThat(event.getFirstUrn()).isEqualTo(playlist.getUrn());
        assertThat(event.getChangeMap().get(playlist.getUrn())).isEqualTo(playlistChangeSet(playlist.getUrn()));
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
                .thenReturn(Observable.<Integer>error(new Exception()));

        operations.addTrackToPlaylist(playlist.getUrn(), trackUrn).subscribe(new TestSubscriber<PropertySet>());

        verifyAddToPlaylistParams();
        eventBus.verifyNoEventsOn(EventQueue.ENTITY_STATE_CHANGED);
    }

    @Test
    public void shouldPublishEntityChangedEventAfterRemovingTrackFromPlaylist() {
        when(removeTrackFromPlaylistCommand.toObservable(any(RemoveTrackFromPlaylistParams.class))).thenReturn(
                just(1));

        operations.removeTrackFromPlaylist(playlist.getUrn(), trackUrn).subscribe();

        verifyRemoveFromPlaylistParams();

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        assertThat(event.getKind()).isEqualTo(EntityStateChangedEvent.TRACK_REMOVED_FROM_PLAYLIST);
        assertThat(event.getFirstUrn()).isEqualTo(playlist.getUrn());
        assertThat(event.getChangeMap().get(playlist.getUrn())).isEqualTo(playlistChangeSet(playlist.getUrn()));

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
                .thenReturn(Observable.<Integer>error(new Exception()));

        operations.removeTrackFromPlaylist(playlist.getUrn(), trackUrn).subscribe(new TestSubscriber<PropertySet>());

        verifyRemoveFromPlaylistParams();
        eventBus.verifyNoEventsOn(EventQueue.ENTITY_STATE_CHANGED);
    }

    @Test
    public void shouldPublishEntityChangedEventAfterEditingPlaylist() {
        when(editPlaylistCommand.toObservable(any(EditPlaylistCommandParams.class))).thenReturn(just(1));

        operations.editPlaylist(playlist.getUrn(), NEW_TITLE, IS_PRIVATE, Arrays.asList(trackUrn)).subscribe();

        verifyEditPlaylistCommandParams();

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        assertThat(event.getKind()).isEqualTo(EntityStateChangedEvent.PLAYLIST_EDITED);
        assertThat(event.getFirstUrn()).isEqualTo(playlist.getUrn());
        assertThat(event.getChangeMap().get(playlist.getUrn())).isEqualTo(playlistEditedChangeSet(playlist.getUrn()));
    }

    @Test
    public void shouldRequestSystemSyncAfterEditingPlaylist() {
        when(editPlaylistCommand.toObservable(any(EditPlaylistCommandParams.class))).thenReturn(just(1));

        operations.editPlaylist(playlist.getUrn(), NEW_TITLE, IS_PRIVATE, Arrays.asList(trackUrn)).subscribe();

        verifyEditPlaylistCommandParams();
        verify(requestSystemSyncAction).call();
    }

    @Test
    public void shouldNotPublishEntityChangedEventAfterEditingPlaylistFailed() {
        when(editPlaylistCommand.toObservable(any(EditPlaylistCommandParams.class)))
                .thenReturn(Observable.<Integer>error(new Exception()));

        operations.editPlaylist(playlist.getUrn(), NEW_TITLE, IS_PRIVATE, Arrays.asList(trackUrn))
                  .subscribe(new TestSubscriber<PropertySet>());

        verifyEditPlaylistCommandParams();
        eventBus.verifyNoEventsOn(EventQueue.ENTITY_STATE_CHANGED);
    }

    @Test
    public void playlistsList() throws Exception {
        Set<Urn> urns = new HashSet<>();
        urns.add(playlistItem.getUrn());

        when(playlistStorage.loadPlaylists(urns)).thenReturn(just(singletonList(playlistItem)));

        TestSubscriber<List<PlaylistItem>> testSubsriber = new TestSubscriber<>();
        operations.playlists(urns).subscribe(testSubsriber);

        testSubsriber.assertValue(singletonList(playlistItem));
        testSubsriber.assertCompleted();
        testSubsriber.assertNoErrors();
    }

    @Test
    public void playlistsMap() throws Exception {
        Set<Urn> urns = new HashSet<>();
        urns.add(playlistItem.getUrn());

        when(playlistStorage.loadPlaylists(urns)).thenReturn(just(singletonList(playlistItem)));

        TestSubscriber<Map<Urn, PlaylistItem>> testSubsriber = new TestSubscriber<>();
        operations.playlistsMap(urns).subscribe(testSubsriber);

        testSubsriber.assertValue(singletonMap(playlistItem.getUrn(), playlistItem));
        testSubsriber.assertCompleted();
        testSubsriber.assertNoErrors();
    }

    private PropertySet playlistChangeSet(Urn playlistUrn) {
        return PropertySet.from(
                PlaylistProperty.URN.bind(playlistUrn),
                PlaylistProperty.TRACK_COUNT.bind(1)
        );
    }

    private PropertySet playlistEditedChangeSet(Urn playlistUrn) {
        return PropertySet.from(
                PlaylistProperty.URN.bind(playlistUrn),
                PlaylistProperty.IS_PRIVATE.bind(IS_PRIVATE),
                PlaylistProperty.TITLE.bind(NEW_TITLE),
                PlaylistProperty.TRACK_COUNT.bind(1)
        );
    }

    private List<TrackItem> trackItems() {
        return Arrays.asList(TrackItem.from(track1), TrackItem.from(track2));
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
}
