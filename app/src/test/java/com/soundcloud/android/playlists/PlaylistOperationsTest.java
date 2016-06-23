package com.soundcloud.android.playlists;

import static com.soundcloud.android.playlists.AddTrackToPlaylistCommand.AddTrackToPlaylistParams;
import static com.soundcloud.android.playlists.RemoveTrackFromPlaylistCommand.RemoveTrackFromPlaylistParams;
import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.playlists.EditPlaylistCommand.EditPlaylistCommandParams;
import com.soundcloud.android.sync.SyncActions;
import com.soundcloud.android.sync.LegacySyncInitiator;
import com.soundcloud.android.sync.SyncJobResult;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
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

public class PlaylistOperationsTest extends AndroidUnitTest {

    private static final boolean IS_PRIVATE = true;
    private static final String NEW_TITLE = "new title";
    private PlaylistOperations operations;

    @Mock private Observer<PlaylistWithTracks> playlistInfoObserver;
    @Mock private LegacySyncInitiator syncInitiator;
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
    private final PropertySet track1 = ModelFixtures.create(ApiTrack.class).toPropertySet();
    private final PropertySet track2 = ModelFixtures.create(ApiTrack.class).toPropertySet();
    private final Urn trackUrn = Urn.forTrack(123L);
    private final List<Urn> newTrackList = Arrays.asList(trackUrn);
    private TestEventBus eventBus;

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
                                            eventBus);
        when(syncInitiator.requestSystemSyncAction()).thenReturn(requestSystemSyncAction);

    }

    @Test
    public void trackUrnsForPlaybackReturnsTrackUrnsFromCommand() {
        final TestObserver<List<Urn>> observer = new TestObserver<>();
        final List<Urn> urnList = Arrays.asList(Urn.forTrack(123L), Urn.forTrack(456L));
        when(loadPlaylistTrackUrns.toObservable()).thenReturn(Observable.just(urnList));

        operations.trackUrnsForPlayback(Urn.forPlaylist(123L)).subscribe(observer);

        assertThat(observer.getOnNextEvents()).hasSize(1);
        assertThat(observer.getOnNextEvents().get(0)).isEqualTo(urnList);
        assertThat(observer.getOnCompletedEvents()).hasSize(1);
    }

    @Test
    public void loadsPlaylistWithTracksFromStorage() {
        when(tracksStorage.playlistTracks(playlist.getUrn())).thenReturn(Observable.<List<PropertySet>>just(newArrayList(
                track1,
                track2)));
        when(playlistStorage.loadPlaylist(playlist.getUrn())).thenReturn(Observable.just(playlist.toPropertySet()));

        operations.playlist(playlist.getUrn()).subscribe(playlistInfoObserver);

        verify(playlistInfoObserver).onNext(new PlaylistWithTracks(playlist.toPropertySet(), trackItems()));
        verify(playlistInfoObserver).onCompleted();
    }

    @Test
    public void updatedPlaylistSyncsThenLoadsFromStorage() {
        when(syncInitiator.syncPlaylist(playlist.getUrn())).thenReturn(Observable.just(SyncJobResult.success(SyncActions.SYNC_PLAYLIST,
                                                                                                             true)));
        when(tracksStorage.playlistTracks(playlist.getUrn())).thenReturn(Observable.<List<PropertySet>>just(newArrayList(
                track1,
                track2)));
        when(playlistStorage.loadPlaylist(playlist.getUrn())).thenReturn(Observable.just(playlist.toPropertySet()));

        operations.updatedPlaylistInfo(playlist.getUrn()).subscribe(playlistInfoObserver);

        InOrder inOrder = Mockito.inOrder(syncInitiator, playlistInfoObserver);
        inOrder.verify(syncInitiator).syncPlaylist(playlist.getUrn());
        inOrder.verify(playlistInfoObserver).onNext(new PlaylistWithTracks(playlist.toPropertySet(), trackItems()));
        inOrder.verify(playlistInfoObserver).onCompleted();
    }

    @Test
    public void loadsPlaylistAndSyncsBeforeEmittingIfPlaylistMetaDataMissing() {
        when(syncInitiator.syncPlaylist(playlist.getUrn())).thenReturn(Observable.just(SyncJobResult.success(SyncActions.SYNC_PLAYLIST,
                                                                                                             true)));
        when(tracksStorage.playlistTracks(playlist.getUrn())).thenReturn(Observable.<List<PropertySet>>just(newArrayList(
                track1,
                track2)));
        when(playlistStorage.loadPlaylist(playlist.getUrn())).thenReturn(Observable.just(PropertySet.<PropertySet>create()),
                                                                         Observable.just(playlist.toPropertySet()));

        operations.playlist(playlist.getUrn()).subscribe(playlistInfoObserver);

        InOrder inOrder = Mockito.inOrder(syncInitiator, playlistInfoObserver);
        inOrder.verify(syncInitiator).syncPlaylist(playlist.getUrn());
        inOrder.verify(playlistInfoObserver).onNext(new PlaylistWithTracks(playlist.toPropertySet(), trackItems()));
        inOrder.verify(playlistInfoObserver).onCompleted();
    }

    @Test
    public void loadsPlaylistAndSyncsBeforeEmittingAPlaylistMissingExceptionIfPlaylistMetaDataStillMissing() {
        when(syncInitiator.syncPlaylist(playlist.getUrn())).thenReturn(Observable.just(SyncJobResult.success(SyncActions.SYNC_PLAYLIST,
                                                                                                             true)));
        when(tracksStorage.playlistTracks(playlist.getUrn())).thenReturn(Observable.<List<PropertySet>>just(newArrayList(
                track1,
                track2)));
        when(playlistStorage.loadPlaylist(playlist.getUrn())).thenReturn(Observable.just(PropertySet.<PropertySet>create()),
                                                                         Observable.just(PropertySet.create()));

        operations.playlist(playlist.getUrn()).subscribe(playlistInfoObserver);

        InOrder inOrder = Mockito.inOrder(syncInitiator, playlistInfoObserver);
        inOrder.verify(syncInitiator).syncPlaylist(playlist.getUrn());
        inOrder.verify(playlistInfoObserver).onError(any(PlaylistOperations.PlaylistMissingException.class));
    }

    @Test
    public void loadsPlaylistAndEmitsAgainAfterSyncIfNoTracksAvailable() {
        final List<PropertySet> emptyTrackList = Collections.emptyList();
        final List<PropertySet> trackList = Arrays.asList(track1, track2);
        when(syncInitiator.syncPlaylist(playlist.getUrn())).thenReturn(Observable.just(SyncJobResult.success(SyncActions.SYNC_PLAYLIST,
                                                                                                             true)));
        when(tracksStorage.playlistTracks(playlist.getUrn())).thenReturn(Observable.just(emptyTrackList),
                                                                         Observable.just(trackList));
        when(playlistStorage.loadPlaylist(playlist.getUrn())).thenReturn(Observable.just(playlist.toPropertySet()));

        operations.playlist(playlist.getUrn()).subscribe(playlistInfoObserver);

        InOrder inOrder = Mockito.inOrder(syncInitiator, playlistInfoObserver);
        inOrder.verify(syncInitiator).syncPlaylist(playlist.getUrn());
        inOrder.verify(playlistInfoObserver)
               .onNext(new PlaylistWithTracks(playlist.toPropertySet(),
                                              TrackItem.fromPropertySets().call(emptyTrackList)));
        inOrder.verify(playlistInfoObserver)
               .onNext(new PlaylistWithTracks(playlist.toPropertySet(), TrackItem.fromPropertySets().call(trackList)));
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
        inOrder.verify(playlistInfoObserver)
               .onNext(new PlaylistWithTracks(playlistProperties, TrackItem.fromPropertySets().call(trackList)));
        inOrder.verify(playlistInfoObserver).onCompleted();
        verify(syncInitiator, never()).syncPlaylist(playlistProperties.get(PlaylistProperty.URN));
    }

    @Test
    public void shouldCreateNewPlaylistUsingCommand() {
        TestObserver<Urn> observer = new TestObserver<>();
        when(tracksStorage.createNewPlaylist("title",
                                             true,
                                             Urn.forTrack(123))).thenReturn(Observable.just(Urn.forPlaylist(1L)));

        operations.createNewPlaylist("title", true, Urn.forTrack(123)).subscribe(observer);

        observer.assertReceivedOnNext(Arrays.asList(Urn.forPlaylist(1L)));
    }

    @Test
    public void shouldPublishEntityChangedEventAfterCreatingPlaylist() {
        TestObserver<Urn> observer = new TestObserver<>();
        final Urn localPlaylist = Urn.newLocalPlaylist();
        when(tracksStorage.createNewPlaylist("title",
                                             true,
                                             Urn.forTrack(123))).thenReturn(Observable.just(localPlaylist));

        operations.createNewPlaylist("title", true, Urn.forTrack(123)).subscribe(observer);

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        assertThat(event.getKind()).isEqualTo(EntityStateChangedEvent.ENTITY_CREATED);
        assertThat(event.getFirstUrn()).isEqualTo(localPlaylist);
    }

    @Test
    public void shouldRequestSystemSyncAfterCreatingPlaylist() throws Exception {
        TestObserver<Urn> observer = new TestObserver<>();
        when(tracksStorage.createNewPlaylist("title",
                                             true,
                                             Urn.forTrack(123))).thenReturn(Observable.just(Urn.forPlaylist(123)));

        operations.createNewPlaylist("title", true, Urn.forTrack(123)).subscribe(observer);

        verify(requestSystemSyncAction).call();
    }

    @Test
    public void shouldPublishEntityChangedEventAfterAddingATrackToPlaylist() {
        when(addTrackToPlaylistCommand.toObservable(any(AddTrackToPlaylistParams.class))).thenReturn(Observable.just(1));

        operations.addTrackToPlaylist(playlist.getUrn(), trackUrn).subscribe();

        verifyAddToPlaylistParams();

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        assertThat(event.getKind()).isEqualTo(EntityStateChangedEvent.TRACK_ADDED_TO_PLAYLIST);
        assertThat(event.getFirstUrn()).isEqualTo(playlist.getUrn());
        assertThat(event.getChangeMap().get(playlist.getUrn())).isEqualTo(playlistChangeSet(playlist.getUrn()));
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
        when(removeTrackFromPlaylistCommand.toObservable(any(RemoveTrackFromPlaylistParams.class))).thenReturn(
                Observable.just(1));

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
                Observable.just(1));

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

    @Test
    public void shouldPublishEntityChangedEventAfterEditingPlaylist() {
        when(editPlaylistCommand.toObservable(any(EditPlaylistCommandParams.class))).thenReturn(Observable.just(1));

        operations.editPlaylist(playlist.getUrn(), NEW_TITLE, IS_PRIVATE, Arrays.asList(trackUrn)).subscribe();

        verifyEditPlaylistCommandParams();

        final EntityStateChangedEvent event = eventBus.lastEventOn(EventQueue.ENTITY_STATE_CHANGED);
        assertThat(event.getKind()).isEqualTo(EntityStateChangedEvent.PLAYLIST_EDITED);
        assertThat(event.getFirstUrn()).isEqualTo(playlist.getUrn());
        assertThat(event.getChangeMap().get(playlist.getUrn())).isEqualTo(playlistEditedChangeSet(playlist.getUrn()));
    }

    @Test
    public void shouldRequestSystemSyncAfterEditingPlaylist() {
        when(editPlaylistCommand.toObservable(any(EditPlaylistCommandParams.class))).thenReturn(Observable.just(1));

        operations.editPlaylist(playlist.getUrn(), NEW_TITLE, IS_PRIVATE, Arrays.asList(trackUrn)).subscribe();

        verifyEditPlaylistCommandParams();
        verify(requestSystemSyncAction).call();
    }

    @Test
    public void shouldNotPublishEntityChangedEventAfterEditingPlaylistFailed() {
        when(editPlaylistCommand.toObservable(any(EditPlaylistCommandParams.class)))
                .thenReturn(Observable.<Integer>error(new Exception()));

        operations.editPlaylist(playlist.getUrn(), NEW_TITLE, IS_PRIVATE, Arrays.asList(trackUrn))
                  .subscribe(new TestObserver<PropertySet>());

        verifyEditPlaylistCommandParams();
        eventBus.verifyNoEventsOn(EventQueue.ENTITY_STATE_CHANGED);
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
