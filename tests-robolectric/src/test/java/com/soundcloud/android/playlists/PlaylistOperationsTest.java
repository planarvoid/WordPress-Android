package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
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
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.propeller.InsertResult;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.propeller.WriteResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observer;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistOperationsTest {

    private PlaylistOperations operations;

    @Mock private Observer<PlaylistInfo> playlistInfoObserver;
    @Mock private SyncInitiator syncInitiator;
    @Mock private PlaylistTracksStorage tracksStorage;
    @Mock private LoadPlaylistCommand loadPlaylistCommand;
    @Mock private LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrns;
    @Mock private LoadPlaylistTracksCommand loadPlaylistTracksCommand;
    @Mock private CreateNewPlaylistCommand createNewPlaylistCommand;

    private final ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
    private final PropertySet track1 = ModelFixtures.create(ApiTrack.class).toPropertySet();
    private final PropertySet track2 = ModelFixtures.create(ApiTrack.class).toPropertySet();
    private final Urn trackUrn = Urn.forTrack(123L);
    private TestEventBus eventBus;

    @Before
    public void setUp() {
        eventBus = new TestEventBus();
        operations = new PlaylistOperations(Schedulers.immediate(), syncInitiator, tracksStorage,
                loadPlaylistCommand, loadPlaylistTrackUrns, loadPlaylistTracksCommand, createNewPlaylistCommand, eventBus);
        when(tracksStorage.addTrackToPlaylist(any(Urn.class), any(Urn.class))).thenReturn(Observable.<PropertySet>empty());
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
        when(loadPlaylistTracksCommand.toObservable()).thenReturn(Observable.<List<PropertySet>>just(Lists.newArrayList(track1, track2)));
        when(loadPlaylistCommand.toObservable()).thenReturn(Observable.just(playlist.toPropertySet()));

        operations.playlistInfo(playlist.getUrn()).subscribe(playlistInfoObserver);

        verify(playlistInfoObserver).onNext(new PlaylistInfo(playlist.toPropertySet(), Arrays.asList(track1, track2)));
        verify(playlistInfoObserver).onCompleted();
    }

    @Test
    public void updatedPlaylistSyncsThenLoadsFromStorage() {
        when(syncInitiator.syncPlaylist(playlist.getUrn())).thenReturn(Observable.just(true));
        when(loadPlaylistTracksCommand.toObservable()).thenReturn(Observable.<List<PropertySet>>just(Lists.newArrayList(track1, track2)));
        when(loadPlaylistCommand.toObservable()).thenReturn(Observable.just(playlist.toPropertySet()));

        operations.updatedPlaylistInfo(playlist.getUrn()).subscribe(playlistInfoObserver);

        InOrder inOrder = Mockito.inOrder(syncInitiator, playlistInfoObserver);
        inOrder.verify(syncInitiator).syncPlaylist(playlist.getUrn());
        inOrder.verify(playlistInfoObserver).onNext(new PlaylistInfo(playlist.toPropertySet(), Arrays.asList(track1, track2)));
        inOrder.verify(playlistInfoObserver).onCompleted();
    }

    @Test
    public void loadsPlaylistAndSyncsBeforeEmittingIfPlaylistMetaDataMissing() {
        when(syncInitiator.syncPlaylist(playlist.getUrn())).thenReturn(Observable.just(true));
        when(loadPlaylistTracksCommand.toObservable()).thenReturn(Observable.<List<PropertySet>>just(Lists.newArrayList(track1, track2)));
        when(loadPlaylistCommand.toObservable()).thenReturn(Observable.just(PropertySet.<PropertySet>create()), Observable.just(playlist.toPropertySet()));

        operations.playlistInfo(playlist.getUrn()).subscribe(playlistInfoObserver);

        InOrder inOrder = Mockito.inOrder(syncInitiator, playlistInfoObserver);
        inOrder.verify(syncInitiator).syncPlaylist(playlist.getUrn());
        inOrder.verify(playlistInfoObserver).onNext(new PlaylistInfo(playlist.toPropertySet(), Arrays.asList(track1, track2)));
        inOrder.verify(playlistInfoObserver).onCompleted();
    }

    @Test
    public void loadsPlaylistAndEmitsAgainAfterSyncIfNoTracksAvailable() {
        final List<PropertySet> emptyTrackList = Collections.emptyList();
        final List<PropertySet> trackList = Arrays.asList(track1, track2);
        when(syncInitiator.syncPlaylist(playlist.getUrn())).thenReturn(Observable.just(true));
        when(loadPlaylistTracksCommand.toObservable()).thenReturn(Observable.just(emptyTrackList), Observable.just(trackList));
        when(loadPlaylistCommand.toObservable()).thenReturn(Observable.just(playlist.toPropertySet()));

        operations.playlistInfo(playlist.getUrn()).subscribe(playlistInfoObserver);

        InOrder inOrder = Mockito.inOrder(syncInitiator, playlistInfoObserver);
        inOrder.verify(syncInitiator).syncPlaylist(playlist.getUrn());
        inOrder.verify(playlistInfoObserver).onNext(new PlaylistInfo(playlist.toPropertySet(), emptyTrackList));
        inOrder.verify(playlistInfoObserver).onNext(new PlaylistInfo(playlist.toPropertySet(), trackList));
        inOrder.verify(playlistInfoObserver).onCompleted();
    }

    @Test
    public void loadsPlaylistAndEmitsOnceIfPlaylistHasEmptyTrackCount() {
        final List<PropertySet> emptyTrackList = Collections.emptyList();
        final PropertySet playlistProperties = playlist.toPropertySet();
        playlistProperties.put(PlaylistProperty.TRACK_COUNT, 0);

        when(loadPlaylistTracksCommand.toObservable()).thenReturn(Observable.just(emptyTrackList));
        when(loadPlaylistCommand.toObservable()).thenReturn(Observable.just(playlistProperties));

        operations.playlistInfo(playlist.getUrn()).subscribe(playlistInfoObserver);

        verify(syncInitiator, never()).syncPlaylist(playlist.getUrn());
        verify(playlistInfoObserver).onNext(new PlaylistInfo(playlistProperties, emptyTrackList));
        verify(playlistInfoObserver).onCompleted();
    }

    @Test
    public void loadsLocalPlaylistAndRequestsMyPlaylistSyncWhenEmitting() {
        final List<PropertySet> trackList = Arrays.asList(track1, track2);
        final PropertySet playlistProperties = playlist.toPropertySet();
        playlistProperties.put(PlaylistProperty.URN, Urn.forTrack(-123L)); // make it a local playlist

        when(loadPlaylistTracksCommand.toObservable()).thenReturn(Observable.just(trackList));
        when(loadPlaylistCommand.toObservable()).thenReturn(Observable.just(playlistProperties));

        operations.playlistInfo(playlist.getUrn()).subscribe(playlistInfoObserver);

        InOrder inOrder = Mockito.inOrder(syncInitiator, playlistInfoObserver);
        inOrder.verify(syncInitiator).syncLocalPlaylists();
        inOrder.verify(playlistInfoObserver).onNext(new PlaylistInfo(playlistProperties, trackList));
        inOrder.verify(playlistInfoObserver).onCompleted();
        verify(syncInitiator, never()).syncPlaylist(playlistProperties.get(PlaylistProperty.URN));
    }

    @Test
    public void shouldCreateNewPlaylistUsingCommand() {
        TestObserver<Urn> observer = new TestObserver<>();
        final TxnResult txnResult = new TxnResult();
        txnResult.add(new InsertResult(1L));
        when(createNewPlaylistCommand.toObservable()).thenReturn(Observable.just((WriteResult) txnResult));

        operations.createNewPlaylist("title", true, Urn.forTrack(123)).subscribe(observer);

        observer.assertReceivedOnNext(Arrays.asList(Urn.forPlaylist(1L)));
    }

    @Test
    public void shouldPublishEntityChangedEventAfterAddingATrackToPlaylist() {
        final PropertySet changeSet = trackAddedChangeSet(playlist.getUrn());
        when(operations.addTrackToPlaylist(playlist.getUrn(), trackUrn)).thenReturn(Observable.just(changeSet));

        operations.addTrackToPlaylist(playlist.getUrn(), trackUrn).subscribe();

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        expect(event.getKind()).toEqual(EntityStateChangedEvent.TRACK_ADDED_TO_PLAYLIST);
        expect(event.getNextUrn()).toEqual(playlist.getUrn());
        expect(event.getChangeMap().get(playlist.getUrn())).toEqual(changeSet);
    }

    @Test
    public void shouldNotPublishEntityChangedEventWhenAddingTrackToPlaylistFailed() {
        TestObserver<PropertySet> testObserver = new TestObserver<>();
        when(operations.addTrackToPlaylist(playlist.getUrn(), trackUrn))
                .thenReturn(Observable.<PropertySet>error(new Exception("Something bad happened")));

        operations.addTrackToPlaylist(playlist.getUrn(), trackUrn).subscribe(testObserver);

        eventBus.verifyNoEventsOn(EventQueue.ENTITY_STATE_CHANGED);
    }

    private PropertySet trackAddedChangeSet(Urn playlistUrn) {
        return PropertySet.from(
                PlaylistProperty.URN.bind(playlistUrn),
                PlaylistProperty.TRACK_COUNT.bind(1)
        );
    }
}