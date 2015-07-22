package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.playlists.AddTrackToPlaylistCommand.AddTrackToPlaylistParams;
import static com.soundcloud.android.playlists.RemoveTrackFromPlaylistCommand.RemoveTrackFromPlaylistParams;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.sync.SyncActions;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observer;
import rx.functions.Action0;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistOperationsTest {

    private PlaylistOperations operations;

    @Mock private Observer<PlaylistWithTracks> playlistInfoObserver;
    @Mock private SyncInitiator syncInitiator;
    @Mock private PlaylistTracksStorage tracksStorage;
    @Mock private PlaylistStorage playlistStorage;
    @Mock private LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrns;
    @Mock private Action0 requestSystemSyncAction;
    @Mock private OfflineContentOperations offlineOperations;
    @Mock private AddTrackToPlaylistCommand addTrackToPlaylistCommand;
    @Mock private RemoveTrackFromPlaylistCommand removeTrackFromPlaylistCommand;
    @Captor private ArgumentCaptor<AddTrackToPlaylistParams> addTrackCommandParamsCaptor;
    @Captor private ArgumentCaptor<RemoveTrackFromPlaylistParams> removeTrackCommandParamsCaptor;

    private final ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
    private final PropertySet track1 = ModelFixtures.create(ApiTrack.class).toPropertySet();
    private final PropertySet track2 = ModelFixtures.create(ApiTrack.class).toPropertySet();
    private final Urn trackUrn = Urn.forTrack(123L);
    private TestEventBus eventBus;

    @Before
    public void setUp() {
        eventBus = new TestEventBus();
        operations = new PlaylistOperations(Schedulers.immediate(), syncInitiator, tracksStorage,
                playlistStorage, loadPlaylistTrackUrns, offlineOperations,
                addTrackToPlaylistCommand, removeTrackFromPlaylistCommand, eventBus);
        when(syncInitiator.requestSystemSyncAction()).thenReturn(requestSystemSyncAction);

    }

    @Test
    public void trackUrnsForPlaybackReturnsTrackUrnsFromCommand() {
        final TestObserver<List<Urn>> observer = new TestObserver<>();
        final List<Urn> urnList = Arrays.asList(Urn.forTrack(123L), Urn.forTrack(456L));
        when(loadPlaylistTrackUrns.toObservable()).thenReturn(Observable.just(urnList));

        operations.trackUrnsForPlayback(Urn.forPlaylist(123L)).subscribe(observer);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0)).toEqual(urnList);
        expect(observer.getOnCompletedEvents()).toNumber(1);
    }

    @Test
    public void loadsPlaylistWithTracksFromStorage() {
        when(tracksStorage.playlistTracks(playlist.getUrn())).thenReturn(Observable.<List<PropertySet>>just(Lists.newArrayList(track1, track2)));
        when(playlistStorage.loadPlaylist(playlist.getUrn())).thenReturn(Observable.just(playlist.toPropertySet()));

        operations.playlist(playlist.getUrn()).subscribe(playlistInfoObserver);

        verify(playlistInfoObserver).onNext(new PlaylistWithTracks(playlist.toPropertySet(), trackItems()));
        verify(playlistInfoObserver).onCompleted();
    }

    @Test
    public void updatedPlaylistSyncsThenLoadsFromStorage() {
        when(syncInitiator.syncPlaylist(playlist.getUrn())).thenReturn(Observable.just(SyncResult.success(SyncActions.SYNC_PLAYLIST, true)));
        when(tracksStorage.playlistTracks(playlist.getUrn())).thenReturn(Observable.<List<PropertySet>>just(Lists.newArrayList(track1, track2)));
        when(playlistStorage.loadPlaylist(playlist.getUrn())).thenReturn(Observable.just(playlist.toPropertySet()));

        operations.updatedPlaylistInfo(playlist.getUrn()).subscribe(playlistInfoObserver);

        InOrder inOrder = Mockito.inOrder(syncInitiator, playlistInfoObserver);
        inOrder.verify(syncInitiator).syncPlaylist(playlist.getUrn());
        inOrder.verify(playlistInfoObserver).onNext(new PlaylistWithTracks(playlist.toPropertySet(), trackItems()));
        inOrder.verify(playlistInfoObserver).onCompleted();
    }

    @Test
    public void loadsPlaylistAndSyncsBeforeEmittingIfPlaylistMetaDataMissing() {
        when(syncInitiator.syncPlaylist(playlist.getUrn())).thenReturn(Observable.just(SyncResult.success(SyncActions.SYNC_PLAYLIST, true)));
        when(tracksStorage.playlistTracks(playlist.getUrn())).thenReturn(Observable.<List<PropertySet>>just(Lists.newArrayList(track1, track2)));
        when(playlistStorage.loadPlaylist(playlist.getUrn())).thenReturn(Observable.just(PropertySet.<PropertySet>create()), Observable.just(playlist.toPropertySet()));

        operations.playlist(playlist.getUrn()).subscribe(playlistInfoObserver);

        InOrder inOrder = Mockito.inOrder(syncInitiator, playlistInfoObserver);
        inOrder.verify(syncInitiator).syncPlaylist(playlist.getUrn());
        inOrder.verify(playlistInfoObserver).onNext(new PlaylistWithTracks(playlist.toPropertySet(), trackItems()));
        inOrder.verify(playlistInfoObserver).onCompleted();
    }

    @Test
    public void loadsPlaylistAndSyncsBeforeEmittingAPlaylistMissingExceptionIfPlaylistMetaDataStillMissing() {
        when(syncInitiator.syncPlaylist(playlist.getUrn())).thenReturn(Observable.just(SyncResult.success(SyncActions.SYNC_PLAYLIST, true)));
        when(tracksStorage.playlistTracks(playlist.getUrn())).thenReturn(Observable.<List<PropertySet>>just(Lists.newArrayList(track1, track2)));
        when(playlistStorage.loadPlaylist(playlist.getUrn())).thenReturn(Observable.just(PropertySet.<PropertySet>create()), Observable.just(PropertySet.create()));

        operations.playlist(playlist.getUrn()).subscribe(playlistInfoObserver);

        InOrder inOrder = Mockito.inOrder(syncInitiator, playlistInfoObserver);
        inOrder.verify(syncInitiator).syncPlaylist(playlist.getUrn());
        inOrder.verify(playlistInfoObserver).onError(any(PlaylistOperations.PlaylistMissingException.class));
    }

    @Test
    public void loadsPlaylistAndEmitsAgainAfterSyncIfNoTracksAvailable() {
        final List<PropertySet> emptyTrackList = Collections.emptyList();
        final List<PropertySet> trackList = Arrays.asList(track1, track2);
        when(syncInitiator.syncPlaylist(playlist.getUrn())).thenReturn(Observable.just(SyncResult.success(SyncActions.SYNC_PLAYLIST, true)));
        when(tracksStorage.playlistTracks(playlist.getUrn())).thenReturn(Observable.just(emptyTrackList), Observable.just(trackList));
        when(playlistStorage.loadPlaylist(playlist.getUrn())).thenReturn(Observable.just(playlist.toPropertySet()));

        operations.playlist(playlist.getUrn()).subscribe(playlistInfoObserver);

        InOrder inOrder = Mockito.inOrder(syncInitiator, playlistInfoObserver);
        inOrder.verify(syncInitiator).syncPlaylist(playlist.getUrn());
        inOrder.verify(playlistInfoObserver).onNext(new PlaylistWithTracks(playlist.toPropertySet(), TrackItem.fromPropertySets().call(emptyTrackList)));
        inOrder.verify(playlistInfoObserver).onNext(new PlaylistWithTracks(playlist.toPropertySet(), TrackItem.fromPropertySets().call(trackList)));
        inOrder.verify(playlistInfoObserver).onCompleted();
    }

    @Test
    public void loadsLocalPlaylistAndRequestsMyPlaylistSyncWhenEmitting() {
        final List<PropertySet> trackList = Arrays.asList(track1, track2);
        final PropertySet playlistProperties = playlist.toPropertySet();
        playlistProperties.put(PlaylistProperty.URN, Urn.forTrack(-123L)); // make it a local playlist

        when(tracksStorage.playlistTracks(playlist.getUrn())).thenReturn(Observable.just(trackList));
        when(playlistStorage.loadPlaylist(playlist.getUrn())).thenReturn(Observable.just(playlistProperties));

        operations.playlist(playlist.getUrn()).subscribe(playlistInfoObserver);

        InOrder inOrder = Mockito.inOrder(syncInitiator, playlistInfoObserver);
        inOrder.verify(syncInitiator).syncLocalPlaylists();
        inOrder.verify(playlistInfoObserver).onNext(new PlaylistWithTracks(playlistProperties, TrackItem.fromPropertySets().call(trackList)));
        inOrder.verify(playlistInfoObserver).onCompleted();
        verify(syncInitiator, never()).syncPlaylist(playlistProperties.get(PlaylistProperty.URN));
    }

    @Test
    public void shouldCreateNewPlaylistUsingCommand() {
        TestObserver<Urn> observer = new TestObserver<>();
        when(tracksStorage.createNewPlaylist("title", true, Urn.forTrack(123))).thenReturn(Observable.just(Urn.forPlaylist(1L)));

        operations.createNewPlaylist("title", true, Urn.forTrack(123)).subscribe(observer);

        observer.assertReceivedOnNext(Arrays.asList(Urn.forPlaylist(1L)));
    }

    @Test
    public void shouldRequestSystemSyncAfterCreatingPlaylist() throws Exception {
        TestObserver<Urn> observer = new TestObserver<>();
        when(tracksStorage.createNewPlaylist("title", true, Urn.forTrack(123))).thenReturn(Observable.just(Urn.forPlaylist(123)));

        operations.createNewPlaylist("title", true, Urn.forTrack(123)).subscribe(observer);

        verify(requestSystemSyncAction).call();
    }

    @Test
    public void shouldPublishEntityChangedEventAfterAddingATrackToPlaylist() {
        when(addTrackToPlaylistCommand.toObservable(any(AddTrackToPlaylistParams.class))).thenReturn(Observable.just(1));

        operations.addTrackToPlaylist(playlist.getUrn(), trackUrn).subscribe();

        verifyAddToPlaylistParams();

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        expect(event.getKind()).toEqual(EntityStateChangedEvent.TRACK_ADDED_TO_PLAYLIST);
        expect(event.getFirstUrn()).toEqual(playlist.getUrn());
        expect(event.getChangeMap().get(playlist.getUrn())).toEqual(playlistChangeSet(playlist.getUrn()));

    }

    @Test
    public void shouldRequestSystemSyncAfterAddingATrackToPlaylist() {
        when(addTrackToPlaylistCommand.toObservable(any(AddTrackToPlaylistParams.class))).thenReturn(Observable.just(1));

        operations.addTrackToPlaylist(playlist.getUrn(), trackUrn).subscribe();

        verifyAddToPlaylistParams();
        verify(requestSystemSyncAction).call();
    }

    @Test
    public void shouldNotPublishEntityChangedEventWhenAddingTrackToPlaylistFailed() {
        when(addTrackToPlaylistCommand.toObservable(any(AddTrackToPlaylistParams.class)))
                .thenReturn(Observable.<Integer>error(new Exception()));

        operations.addTrackToPlaylist(playlist.getUrn(), trackUrn).subscribe(new TestObserver<PropertySet>());

        verifyAddToPlaylistParams();
        eventBus.verifyNoEventsOn(EventQueue.ENTITY_STATE_CHANGED);
    }

    @Test
    public void shouldPublishEntityChangedEventAfterRemovingTrackFromPlaylist() {
        when(removeTrackFromPlaylistCommand.toObservable(any(RemoveTrackFromPlaylistParams.class))).thenReturn(Observable.just(1));

        operations.removeTrackFromPlaylist(playlist.getUrn(), trackUrn).subscribe();

        verifyRemoveFromPlaylistParams();

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        expect(event.getKind()).toEqual(EntityStateChangedEvent.TRACK_REMOVED_FROM_PLAYLIST);
        expect(event.getFirstUrn()).toEqual(playlist.getUrn());
        expect(event.getChangeMap().get(playlist.getUrn())).toEqual(playlistChangeSet(playlist.getUrn()));

    }

    @Test
    public void shouldRequestSystemSyncAfterRemovingTrackFromPlaylist() {
        when(removeTrackFromPlaylistCommand.toObservable(any(RemoveTrackFromPlaylistParams.class))).thenReturn(Observable.just(1));

        operations.removeTrackFromPlaylist(playlist.getUrn(), trackUrn).subscribe();

        verifyRemoveFromPlaylistParams();
        verify(requestSystemSyncAction).call();
    }

    @Test
    public void shouldNotPublishEntityChangedEventWhenRemovingTrackFromPlaylistFailed() {
        when(removeTrackFromPlaylistCommand.toObservable(any(RemoveTrackFromPlaylistParams.class)))
                .thenReturn(Observable.<Integer>error(new Exception()));

        operations.removeTrackFromPlaylist(playlist.getUrn(), trackUrn).subscribe(new TestObserver<PropertySet>());

        verifyRemoveFromPlaylistParams();
        eventBus.verifyNoEventsOn(EventQueue.ENTITY_STATE_CHANGED);
    }

    private PropertySet playlistChangeSet(Urn playlistUrn) {
        return PropertySet.from(
                PlaylistProperty.URN.bind(playlistUrn),
                PlaylistProperty.TRACK_COUNT.bind(1)
        );
    }

    private List<TrackItem> trackItems() {
        return Arrays.asList(TrackItem.from(track1), TrackItem.from(track2));
    }

    private void verifyAddToPlaylistParams() {
        verify(addTrackToPlaylistCommand).toObservable(addTrackCommandParamsCaptor.capture());
        expect(addTrackCommandParamsCaptor.getValue().playlistUrn).toEqual(playlist.getUrn());
        expect(addTrackCommandParamsCaptor.getValue().trackUrn).toEqual(trackUrn);
    }

    private void verifyRemoveFromPlaylistParams() {
        verify(removeTrackFromPlaylistCommand).toObservable(removeTrackCommandParamsCaptor.capture());
        expect(removeTrackCommandParamsCaptor.getValue().playlistUrn).toEqual(playlist.getUrn());
        expect(removeTrackCommandParamsCaptor.getValue().trackUrn).toEqual(trackUrn);
    }
}