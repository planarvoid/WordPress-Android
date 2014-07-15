package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.legacy.model.LocalCollection;
import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.api.legacy.model.SoundAssociation;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.NotFoundException;
import com.soundcloud.android.storage.PlaylistStorage;
import com.soundcloud.android.storage.SoundAssociationStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.SyncFailedException;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncStateManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.observers.TestSubscriber;

import android.accounts.Account;
import android.content.SyncResult;
import android.os.Bundle;

@RunWith(SoundCloudTestRunner.class)
public class LegacyPlaylistOperationsTest {

    private LegacyPlaylistOperations playlistOperations;
    private PublicApiPlaylist playlist;
    private Subscriber<SyncResult> syncSubscriber = new TestSubscriber<SyncResult>();

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
    private Observer<PublicApiPlaylist> observer;

    @Before
    public void setup() {
        playlistOperations = new LegacyPlaylistOperations(playlistStorage, soundAssociationStorage,
                syncInitiator, syncStateManager);
        playlist = new PublicApiPlaylist(123L);

        Observable<PublicApiPlaylist> storageObservable = Observable.from(playlist);
        when(playlistStorage.loadPlaylistAsync(123L)).thenReturn(storageObservable);
    }

    @Test
    public void shouldCreateNewPlaylist() {
        PublicApiUser currentUser = new PublicApiUser();
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
        final PublicApiPlaylist playlist = new PublicApiPlaylist(1L);
        when(playlistStorage.loadPlaylistWithTracksAsync(playlist.getId())).thenReturn(
                Observable.<PublicApiPlaylist>error(new NotFoundException(playlist.getId())), Observable.just(playlist));
        when(syncInitiator.syncPlaylist(playlist.getUrn())).thenReturn(Observable.just(true));

        playlistOperations.loadPlaylist(playlist.getUrn()).subscribe(observer);

        verify(syncInitiator).syncPlaylist(playlist.getUrn());
    }

    @Test
    public void loadPlaylistShouldForwardErrorsFromLocalStorage() throws Exception {
        final PublicApiPlaylist playlist = new PublicApiPlaylist(1L);
        Exception exception = new Exception();
        when(playlistStorage.loadPlaylistWithTracksAsync(playlist.getId())).thenReturn(
                Observable.<PublicApiPlaylist>error(exception));

        playlistOperations.loadPlaylist(playlist.getUrn()).subscribe(observer);

        verify(observer).onError(exception);
        verifyNoMoreInteractions(observer);
        verifyZeroInteractions(syncInitiator);
    }

    @Test
    public void loadPlaylistShouldNotSyncAndEmitPlaylistImmediatelyIfPlaylistUpToDate() {
        final PublicApiPlaylist playlist = new PublicApiPlaylist(1L);
        expect(PublicApiPlaylist.isLocal(playlist.getId())).toBeFalse();

        when(syncState.isSyncDue()).thenReturn(false);
        when(syncStateManager.fromContent(playlist.toUri())).thenReturn(syncState);
        when(playlistStorage.loadPlaylistWithTracksAsync(playlist.getId())).thenReturn(
                Observable.just(playlist));

        playlistOperations.loadPlaylist(playlist.getUrn()).subscribe(observer);

        InOrder callbacks = inOrder(observer);
        callbacks.verify(observer).onNext(playlist);
        callbacks.verify(observer).onCompleted();
        callbacks.verifyNoMoreInteractions();

        verifyZeroInteractions(syncInitiator);
    }

    @Test
    public void loadPlaylistShouldEmitPlaylistThenTriggerSyncIfPlaylistExistsButNeedsSyncing() {
        final PublicApiPlaylist playlist = new PublicApiPlaylist(1L);

        when(syncState.isSyncDue()).thenReturn(true);
        when(syncStateManager.fromContent(playlist.toUri())).thenReturn(syncState);
        when(playlistStorage.loadPlaylistWithTracksAsync(playlist.getId())).thenReturn(
                Observable.just(playlist), Observable.just(playlist));
        when(syncInitiator.syncPlaylist(playlist.getUrn())).thenReturn(Observable.just(true));

        playlistOperations.loadPlaylist(playlist.getUrn()).subscribe(observer);

        InOrder callbacks = inOrder(observer, syncInitiator);
        callbacks.verify(syncInitiator).syncPlaylist(playlist.getUrn());
        // we emit the playlist eagerly after loading from local storage, then again after
        // any successful sync, so twice in total
        callbacks.verify(observer, times(2)).onNext(playlist);
        callbacks.verify(observer).onCompleted();
        callbacks.verifyNoMoreInteractions();
    }

    @Test
    public void loadPlaylistTriggersSyncAndEmitsPlaylistIfPlaylistExistsButIsALocalPlaylist() {
        final PublicApiPlaylist playlist = new PublicApiPlaylist(-123L);
        expect(PublicApiPlaylist.isLocal(playlist.getId())).toBeTrue();

        when(syncState.isSyncDue()).thenReturn(false);
        when(syncStateManager.fromContent(playlist.toUri())).thenReturn(syncState);
        when(playlistStorage.loadPlaylistWithTracksAsync(playlist.getId())).thenReturn(Observable.just(playlist));

        playlistOperations.loadPlaylist(playlist.getUrn()).subscribe(observer);

        InOrder callbacks = inOrder(observer, syncInitiator);
        callbacks.verify(syncInitiator).syncLocalPlaylists();
        callbacks.verify(observer).onNext(playlist);
        callbacks.verify(observer).onCompleted();
        callbacks.verifyNoMoreInteractions();
    }

    @Test
    public void loadPlaylistShouldEmitErrorOnFailedPlaylistSync() throws Exception {
        final PublicApiPlaylist playlist = new PublicApiPlaylist(1L);
        when(syncState.isSyncDue()).thenReturn(true);
        when(syncStateManager.fromContent(playlist.toUri())).thenReturn(syncState);
        when(playlistStorage.loadPlaylistWithTracksAsync(playlist.getId())).thenReturn(
                Observable.just(playlist));
        when(syncInitiator.syncPlaylist(playlist.getUrn())).thenReturn(
                Observable.<Boolean>error(new SyncFailedException(new Bundle())));

        playlistOperations.loadPlaylist(playlist.getUrn()).subscribe(observer);

        InOrder callbacks = inOrder(observer, syncInitiator);
        callbacks.verify(syncInitiator).syncPlaylist(playlist.getUrn());
        // we expect one call to onNext with whatever we found in local storage
        callbacks.verify(observer).onNext(playlist);
        callbacks.verify(observer).onError(any(SyncFailedException.class));
        callbacks.verifyNoMoreInteractions();
    }

    @Test
    public void refreshPlaylistSyncsAndEmitsPlaylistFromLocalStorage() throws Exception {
        final PublicApiPlaylist playlist = new PublicApiPlaylist(1L);
        when(playlistStorage.loadPlaylistWithTracksAsync(playlist.getId())).thenReturn(Observable.just(playlist));
        when(syncInitiator.syncPlaylist(playlist.getUrn())).thenReturn(Observable.just(true));

        playlistOperations.refreshPlaylist(playlist.getUrn()).subscribe(observer);

        InOrder callbacks = inOrder(observer, syncInitiator);
        callbacks.verify(syncInitiator).syncPlaylist(playlist.getUrn());
        callbacks.verify(observer).onNext(playlist);
        callbacks.verify(observer).onCompleted();
        callbacks.verifyNoMoreInteractions();
    }
}
