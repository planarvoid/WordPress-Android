package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.rx.TestObservables.MockObservable;
import static com.soundcloud.android.sync.SyncInitiator.ResultReceiverAdapter;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.NotFoundException;
import com.soundcloud.android.storage.PlaylistStorage;
import com.soundcloud.android.storage.SoundAssociationStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncStateManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

import android.accounts.Account;
import android.os.Bundle;
import android.os.ResultReceiver;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistOperationsTest {

    private PlaylistOperations playlistOperations;
    private Playlist playlist;

    @Mock
    private PlaylistStorage playlistStorage;
    @Mock
    private SoundAssociationStorage soundAssociationStorage;
    @Mock
    private SyncStateManager syncStateManager;
    @Mock
    private LocalCollection syncState;
    @Mock
    private SyncInitiator syncInitiator;
    @Mock
    private ScModelManager modelManager;
    @Mock
    private Account account;
    @Mock
    private Observer<Playlist> observer;

    @Before
    public void setup() {
        playlistOperations = new PlaylistOperations(playlistStorage, soundAssociationStorage,
                syncInitiator, syncStateManager);
        playlist = new Playlist(123L);

        Observable<Playlist> storageObservable = Observable.from(playlist);
        when(playlistStorage.loadPlaylistAsync(123L)).thenReturn(storageObservable);
    }

    @Test
    public void shouldCreateNewPlaylist() {
        User currentUser = new User();
        long firstTrackId = 1L;

        when(playlistStorage.createNewUserPlaylistAsync(
                currentUser, "new playlist", false, firstTrackId)).thenReturn(Observable.just(playlist));
        SoundAssociation playlistCreation = new SoundAssociation(playlist);
        when(soundAssociationStorage.addCreationAsync(refEq(playlist))).thenReturn(Observable.just(playlistCreation));

        playlistOperations.createNewPlaylist(currentUser, "new playlist", false, firstTrackId).subscribe(observer);

        verify(syncStateManager).forceToStale(Content.ME_PLAYLISTS);
        verify(observer).onNext(playlist);
    }

    @Test
    public void shouldAddATrackToExistingPlaylist() {
        when(playlistStorage.addTrackToPlaylist(playlist, 1L)).thenReturn(playlist);

        playlistOperations.addTrackToPlaylist(123L, 1L).subscribe(observer);

        verify(playlistStorage).addTrackToPlaylist(playlist, 1L);
        verify(observer).onNext(playlist);
    }

    @Test
    public void loadPlaylistShouldSyncPlaylistWhenNotPresentInLocalStorage() throws Exception {
        final Playlist playlist = new Playlist(1L);
        when(playlistStorage.loadPlaylistWithTracksAsync(playlist.getId())).thenReturn(
                MockObservable.<Playlist>error(new NotFoundException(playlist.getId())), MockObservable.just(playlist));

        playlistOperations.loadPlaylist(playlist.getId()).subscribe(observer);

        ArgumentCaptor<ResultReceiver> resultReceiver = ArgumentCaptor.forClass(ResultReceiver.class);
        InOrder callbacks = inOrder(observer, syncInitiator);
        callbacks.verify(syncInitiator).syncResource(eq(playlist.toUri()), resultReceiver.capture());
        forwardSyncResult(ApiSyncService.STATUS_SYNC_FINISHED, resultReceiver);
        callbacks.verify(observer).onNext(playlist);
        callbacks.verify(observer).onCompleted();
        callbacks.verifyNoMoreInteractions();
    }

    @Test
    public void loadPlaylistShouldForwardErrorsFromLocalStorage() throws Exception {
        final Playlist playlist = new Playlist(1L);
        Exception exception = new Exception();
        when(playlistStorage.loadPlaylistWithTracksAsync(playlist.getId())).thenReturn(
                MockObservable.<Playlist>error(exception));

        playlistOperations.loadPlaylist(playlist.getId()).subscribe(observer);

        verify(observer).onError(exception);
        verifyNoMoreInteractions(observer);
        verifyZeroInteractions(syncInitiator);
    }

    @Test
    public void loadPlaylistShouldNotSyncAndEmitPlaylistImmediatelyIfPlaylistUpToDate() {
        final Playlist storedPlaylist = new Playlist(1L);
        expect(Playlist.isLocal(storedPlaylist.getId())).toBeFalse();

        when(syncState.isSyncDue()).thenReturn(false);
        when(syncStateManager.fromContent(storedPlaylist.toUri())).thenReturn(syncState);
        when(playlistStorage.loadPlaylistWithTracksAsync(storedPlaylist.getId())).thenReturn(
                MockObservable.just(storedPlaylist));

        playlistOperations.loadPlaylist(storedPlaylist.getId()).subscribe(observer);

        InOrder callbacks = inOrder(observer);
        callbacks.verify(observer).onNext(storedPlaylist);
        callbacks.verify(observer).onCompleted();
        callbacks.verifyNoMoreInteractions();

        verifyZeroInteractions(syncInitiator);
    }

    @Test
    public void loadPlaylistShouldEmitPlaylistThenTriggerSyncIfPlaylistExistsButNeedsSyncing() {
        final Playlist storedPlaylist = new Playlist(1L);
        final Playlist syncedPlaylist = new Playlist(1L);

        when(syncState.isSyncDue()).thenReturn(true);
        when(syncStateManager.fromContent(storedPlaylist.toUri())).thenReturn(syncState);
        when(playlistStorage.loadPlaylistWithTracksAsync(storedPlaylist.getId())).thenReturn(
                MockObservable.just(storedPlaylist), MockObservable.just(syncedPlaylist));

        playlistOperations.loadPlaylist(storedPlaylist.getId()).subscribe(observer);

        ArgumentCaptor<ResultReceiver> resultReceiver = ArgumentCaptor.forClass(ResultReceiver.class);
        InOrder callbacks = inOrder(observer, syncInitiator);
        callbacks.verify(observer).onNext(storedPlaylist);
        callbacks.verify(syncInitiator).syncResource(eq(storedPlaylist.toUri()), resultReceiver.capture());
        forwardSyncResult(ApiSyncService.STATUS_SYNC_FINISHED, resultReceiver);
        callbacks.verify(observer).onNext(syncedPlaylist);
        callbacks.verify(observer).onCompleted();
        callbacks.verifyNoMoreInteractions();
    }

    @Test
    public void loadPlaylistTriggersSyncAndEmitsPlaylistIfPlaylistExistsButIsALocalPlaylist() {
        final Playlist storedPlaylist = new Playlist(-123L);
        expect(Playlist.isLocal(storedPlaylist.getId())).toBeTrue();

        when(syncState.isSyncDue()).thenReturn(false);
        when(syncStateManager.fromContent(storedPlaylist.toUri())).thenReturn(syncState);
        when(playlistStorage.loadPlaylistWithTracksAsync(storedPlaylist.getId())).thenReturn(MockObservable.just(storedPlaylist));

        playlistOperations.loadPlaylist(storedPlaylist.getId()).subscribe(observer);

        InOrder callbacks = inOrder(observer, syncInitiator);
        callbacks.verify(syncInitiator).syncLocalPlaylists();
        callbacks.verify(observer).onNext(storedPlaylist);
        callbacks.verify(observer).onCompleted();
        callbacks.verifyNoMoreInteractions();
    }

    @Test
    public void loadPlaylistShouldEmitErrorOnFailedPlaylistSync() throws Exception {
        final Playlist playlist = new Playlist(1L);
        when(syncState.isSyncDue()).thenReturn(true);
        when(syncStateManager.fromContent(playlist.toUri())).thenReturn(syncState);
        when(playlistStorage.loadPlaylistWithTracksAsync(playlist.getId())).thenReturn(
                MockObservable.just(playlist));

        playlistOperations.loadPlaylist(playlist.getId()).subscribe(observer);

        ArgumentCaptor<ResultReceiver> resultReceiver = ArgumentCaptor.forClass(ResultReceiver.class);
        InOrder callbacks = inOrder(observer, syncInitiator);
        callbacks.verify(observer).onNext(playlist);
        callbacks.verify(syncInitiator).syncResource(eq(playlist.toUri()), resultReceiver.capture());
        forwardSyncResult(ApiSyncService.STATUS_SYNC_ERROR, resultReceiver);
        callbacks.verify(observer).onError(any(SyncInitiator.SyncFailedException.class));
        callbacks.verifyNoMoreInteractions();
    }

    @Test
    public void refreshPlaylistSyncsAndEmitsPlaylistFromLocalStorage() throws Exception {
        final Playlist playlist = new Playlist(1L);
        when(playlistStorage.loadPlaylistWithTracksAsync(playlist.getId())).thenReturn(Observable.just(playlist));

        playlistOperations.refreshPlaylist(playlist.getId()).subscribe(observer);

        ArgumentCaptor<ResultReceiver> resultReceiver = ArgumentCaptor.forClass(ResultReceiver.class);
        InOrder callbacks = inOrder(observer, syncInitiator);
        callbacks.verify(syncInitiator).syncPlaylist(eq(playlist.toUri()), resultReceiver.capture());
        forwardSyncResult(ApiSyncService.STATUS_SYNC_FINISHED, resultReceiver);
        callbacks.verify(observer).onNext(playlist);
        callbacks.verify(observer).onCompleted();
        callbacks.verifyNoMoreInteractions();

    }

    private void forwardSyncResult(int syncOutcome, ArgumentCaptor<ResultReceiver> resultReceiver) {
        // emulate Android ResultReceiver behavior
        expect(resultReceiver.getValue()).toBeInstanceOf(ResultReceiverAdapter.class);

        ResultReceiverAdapter receiverAdapter = (ResultReceiverAdapter) resultReceiver.getValue();
        // forward sync result to subscriber
        receiverAdapter.onReceiveResult(syncOutcome, new Bundle());
    }

}
