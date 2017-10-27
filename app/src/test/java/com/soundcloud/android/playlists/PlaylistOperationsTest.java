package com.soundcloud.android.playlists;

import static com.soundcloud.android.playlists.AddTrackToPlaylistCommand.AddTrackToPlaylistParams;
import static com.soundcloud.android.playlists.RemoveTrackFromPlaylistCommand.RemoveTrackFromPlaylistParams;
import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static io.reactivex.schedulers.Schedulers.trampoline;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.just;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.collection.playlists.MyPlaylistsOperations;
import com.soundcloud.android.configuration.FeatureOperations;
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
import com.soundcloud.android.testsupport.PlaylistFixtures;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.rx.eventbus.TestEventBus;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class PlaylistOperationsTest {

    private static final boolean IS_PRIVATE = true;
    private static final String NEW_TITLE = "new title";
    private PlaylistOperations operations;

    @Mock private SyncInitiator syncInitiator;
    @Mock private PlaylistTracksStorage playlistTracksStorage;
    @Mock private PlaylistRepository playlistRepository;
    @Mock private TrackRepository trackRepository;
    @Mock private LoadPlaylistTrackUrnsCommand loadPlaylistTrackUrns;
    @Mock private AddTrackToPlaylistCommand addTrackToPlaylistCommand;
    @Mock private RemoveTrackFromPlaylistCommand removeTrackFromPlaylistCommand;
    @Mock private EditPlaylistCommand editPlaylistCommand;
    @Mock private OfflineContentOperations offlineContentOperations;
    @Mock private ProfileApiMobile profileApiMobile;
    @Mock private PlaylistUpsellOperations upsellOperations;
    @Mock private MyPlaylistsOperations myPlaylistsOperations;
    @Mock private AccountOperations accountOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private RemovePlaylistFromDatabaseCommand removePlaylistCommand;

    @Captor private ArgumentCaptor<AddTrackToPlaylistParams> addTrackCommandParamsCaptor;
    @Captor private ArgumentCaptor<RemoveTrackFromPlaylistParams> removeTrackCommandParamsCaptor;
    @Captor private ArgumentCaptor<EditPlaylistCommandParams> editPlaylistCommandParamsCaptor;

    private final Playlist playlist = PlaylistFixtures.playlist();

    private final Urn trackUrn = Urn.forTrack(123L);
    private final List<Urn> newTrackList = asList(trackUrn);
    private TestEventBus eventBus;


    @Before
    public void setUp() {
        eventBus = new TestEventBus();
        when(playlistRepository.withUrn(playlist.urn())).thenReturn(Maybe.just(playlist));
        when(syncInitiator.requestSystemSync()).thenReturn(Completable.complete());
        operations = new PlaylistOperations(trampoline(),
                                            syncInitiator,
                                            playlistRepository,
                                            providerOf(loadPlaylistTrackUrns),
                                            playlistTracksStorage, trackRepository,
                                            addTrackToPlaylistCommand,
                                            removeTrackFromPlaylistCommand,
                                            editPlaylistCommand,
                                            offlineContentOperations,
                                            removePlaylistCommand,
                                            eventBus);
    }

    @Test
    public void trackUrnsForPlaybackReturnsTrackUrnsFromCommand() {
        final TestObserver<List<Urn>> observer = new TestObserver<>();
        final List<Urn> urnList = asList(Urn.forTrack(123L), Urn.forTrack(456L));
        final Urn playlistUrn = Urn.forPlaylist(123L);
        when(loadPlaylistTrackUrns.toSingle(playlistUrn)).thenReturn(Single.just(urnList));

        operations.trackUrnsForPlayback(playlistUrn).subscribe(observer);

        observer.assertValue(urnList).assertComplete();
    }

    @Test
    public void shouldCreateNewPlaylistUsingCommand() {
        TestObserver<Urn> observer = new TestObserver<>();
        when(playlistTracksStorage.createNewPlaylist("title", true, Urn.forTrack(123))).thenReturn(just(Urn.forPlaylist(1L)));

        operations.createNewPlaylist("title", true, false, Urn.forTrack(123)).subscribe(observer);

        observer.assertValue(Urn.forPlaylist(1L)).assertComplete();
    }

    @Test
    public void shouldMarkPlaylistForOfflineAfterCreatingPlaylist() throws Exception {
        TestObserver<Urn> observer = new TestObserver<>();
        Urn playlistUrn = Urn.forPlaylist(123);
        when(playlistTracksStorage.createNewPlaylist("title", true, Urn.forTrack(123))).thenReturn(just(playlistUrn));
        when(offlineContentOperations.makePlaylistAvailableOffline(playlistUrn)).thenReturn(Completable.complete());

        operations.createNewPlaylist("title", true, true, Urn.forTrack(123)).subscribe(observer);

        observer.assertValue(playlistUrn).assertComplete();
    }

    @Test
    public void shouldPublishEntityChangedEventAfterCreatingPlaylist() {
        TestObserver<Urn> observer = new TestObserver<>();
        final Urn localPlaylist = Urn.newLocalPlaylist();
        when(playlistTracksStorage.createNewPlaylist("title", true, Urn.forTrack(123))).thenReturn(just(localPlaylist));

        operations.createNewPlaylist("title", true, false, Urn.forTrack(123)).subscribe(observer);

        final UrnStateChangedEvent event = eventBus.lastEventOn(EventQueue.URN_STATE_CHANGED);
        assertThat(event.kind()).isEqualTo(UrnStateChangedEvent.Kind.ENTITY_CREATED);
        assertThat(event.urns().iterator().next()).isEqualTo(localPlaylist);
    }

    @Test
    public void shouldRequestSystemSyncAfterCreatingPlaylist() throws Exception {
        TestObserver<Urn> observer = new TestObserver<>();
        when(playlistTracksStorage.createNewPlaylist("title", true, Urn.forTrack(123))).thenReturn(just(Urn.forPlaylist(123)));

        operations.createNewPlaylist("title", true, false, Urn.forTrack(123)).subscribe(observer);

        verify(syncInitiator).requestSystemSync();
    }

    @Test
    public void shouldPublishEntityChangedEventAfterAddingATrackToPlaylist() {
        when(addTrackToPlaylistCommand.toSingle(any(AddTrackToPlaylistParams.class))).thenReturn(
                Single.just(1));

        operations.addTrackToPlaylist(playlist.urn(), trackUrn).subscribe();

        verifyAddToPlaylistParams();

        final PlaylistChangedEvent event = eventBus.lastEventOn(EventQueue.PLAYLIST_CHANGED);
        assertThat(event.kind()).isEqualTo(PlaylistTrackCountChangedEvent.Kind.TRACK_ADDED);
        assertThat(event.changeMap().keySet().iterator().next()).isEqualTo(playlist.urn());
    }

    @Test
    public void shouldRequestSystemSyncAfterAddingATrackToPlaylist() {
        when(addTrackToPlaylistCommand.toSingle(any(AddTrackToPlaylistParams.class))).thenReturn(
                Single.just(1));

        operations.addTrackToPlaylist(playlist.urn(), trackUrn).subscribe();

        verifyAddToPlaylistParams();
        verify(syncInitiator).requestSystemSync();
    }

    @Test
    public void shouldNotPublishEntityChangedEventWhenAddingTrackToPlaylistFailed() {
        when(addTrackToPlaylistCommand.toSingle(any(AddTrackToPlaylistParams.class)))
                .thenReturn(Single.error(new Exception()));

        operations.addTrackToPlaylist(playlist.urn(), trackUrn).subscribe(new TestObserver<>());

        verifyAddToPlaylistParams();
        eventBus.verifyNoEventsOn(EventQueue.PLAYLIST_CHANGED);
    }

    @Test
    public void shouldPublishEntityChangedEventAfterRemovingTrackFromPlaylist() {
        when(removeTrackFromPlaylistCommand.toSingle(any(RemoveTrackFromPlaylistParams.class))).thenReturn(Single.just(1));

        operations.removeTrackFromPlaylist(playlist.urn(), trackUrn).subscribe();

        verifyRemoveFromPlaylistParams();

        final PlaylistChangedEvent event = eventBus.lastEventOn(EventQueue.PLAYLIST_CHANGED);
        assertThat(event.kind()).isEqualTo(PlaylistTrackCountChangedEvent.Kind.TRACK_REMOVED);
        assertThat(event.changeMap().keySet().iterator().next()).isEqualTo(playlist.urn());
    }

    @Test
    public void shouldRequestSystemSyncAfterRemovingTrackFromPlaylist() {
        when(removeTrackFromPlaylistCommand.toSingle(any(RemoveTrackFromPlaylistParams.class))).thenReturn(
                Single.just(1));

        operations.removeTrackFromPlaylist(playlist.urn(), trackUrn).subscribe();

        verifyRemoveFromPlaylistParams();
        verify(syncInitiator).requestSystemSync();
    }

    @Test
    public void shouldNotPublishEntityChangedEventWhenRemovingTrackFromPlaylistFailed() {
        when(removeTrackFromPlaylistCommand.toSingle(any(RemoveTrackFromPlaylistParams.class)))
                .thenReturn(Single.error(new Exception()));

        operations.removeTrackFromPlaylist(playlist.urn(), trackUrn).subscribe(new TestObserver<>());

        verifyRemoveFromPlaylistParams();
        eventBus.verifyNoEventsOn(EventQueue.PLAYLIST_CHANGED);
    }

    @Test
    public void shouldPublishEntityChangedEventAfterEditingPlaylist() {
        when(editPlaylistCommand.toSingle(any(EditPlaylistCommandParams.class))).thenReturn(Single.just(1));

        operations.editPlaylist(playlist.urn(), NEW_TITLE, IS_PRIVATE, asList(trackUrn)).subscribe();

        verifyEditPlaylistCommandParams();

        final PlaylistEntityChangedEvent event = (PlaylistEntityChangedEvent) eventBus.lastEventOn(EventQueue.PLAYLIST_CHANGED);
        assertThat(event.kind()).isEqualTo(PlaylistChangedEvent.Kind.PLAYLIST_EDITED);
        assertThat(event.changeMap().containsKey(playlist.urn())).isTrue();
        assertThat(event.changeMap().get(playlist.urn())).isEqualTo(playlist);
    }

    @Test
    public void shouldRequestSystemSyncAfterEditingPlaylist() {
        when(editPlaylistCommand.toSingle(any(EditPlaylistCommandParams.class))).thenReturn(Single.just(1));

        operations.editPlaylist(playlist.urn(), NEW_TITLE, IS_PRIVATE, asList(trackUrn)).subscribe();

        verifyEditPlaylistCommandParams();
        verify(syncInitiator).syncPlaylistAndForget(playlist.urn());
    }

    @Test
    public void shouldNotPublishEntityChangedEventAfterEditingPlaylistFailed() {
        when(editPlaylistCommand.toSingle(any(EditPlaylistCommandParams.class)))
                .thenReturn(Single.error(new Exception()));

        operations.editPlaylist(playlist.urn(), NEW_TITLE, IS_PRIVATE, asList(trackUrn))
                  .subscribe(new TestObserver<>());

        verifyEditPlaylistCommandParams();
        eventBus.verifyNoEventsOn(EventQueue.PLAYLIST_CHANGED);
    }

    private void verifyAddToPlaylistParams() {
        verify(addTrackToPlaylistCommand).toSingle(addTrackCommandParamsCaptor.capture());
        assertThat(addTrackCommandParamsCaptor.getValue().playlistUrn).isEqualTo(playlist.urn());
        assertThat(addTrackCommandParamsCaptor.getValue().trackUrn).isEqualTo(trackUrn);
    }

    private void verifyRemoveFromPlaylistParams() {
        verify(removeTrackFromPlaylistCommand).toSingle(removeTrackCommandParamsCaptor.capture());
        assertThat(removeTrackCommandParamsCaptor.getValue().playlistUrn).isEqualTo(playlist.urn());
        assertThat(removeTrackCommandParamsCaptor.getValue().trackUrn).isEqualTo(trackUrn);
    }

    private void verifyEditPlaylistCommandParams() {
        verify(editPlaylistCommand).toSingle(editPlaylistCommandParamsCaptor.capture());
        assertThat(editPlaylistCommandParamsCaptor.getValue().playlistUrn).isEqualTo(playlist.urn());
        assertThat(editPlaylistCommandParamsCaptor.getValue().trackList).isEqualTo(newTrackList);
        assertThat(editPlaylistCommandParamsCaptor.getValue().isPrivate.get()).isEqualTo(IS_PRIVATE);
        assertThat(editPlaylistCommandParamsCaptor.getValue().playlistTitle.get()).isEqualTo(NEW_TITLE);
    }
}
