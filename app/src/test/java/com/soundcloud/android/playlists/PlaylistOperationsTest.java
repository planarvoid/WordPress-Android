package com.soundcloud.android.playlists;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.PlaylistStorage;
import com.soundcloud.android.storage.SoundAssociationStorage;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.SyncStateManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

import android.accounts.Account;

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
    private ScModelManager modelManager;
    @Mock
    private AccountOperations accountOperations;
    @Mock
    private Account account;
    @Mock
    private Observer observer;

    @Before
    public void setup() {
        playlistOperations = new PlaylistOperations(playlistStorage, soundAssociationStorage,
                syncStateManager, accountOperations, modelManager);
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
        when(accountOperations.getSoundCloudAccount()).thenReturn(account);

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
    public void shouldReturnPlaylistFromCacheIfFound() {
        when(modelManager.getPlaylist(1L)).thenReturn(playlist);

        playlistOperations.loadPlaylist(1).subscribe(observer);

        verify(observer).onNext(playlist);
        verifyZeroInteractions(playlistStorage);
    }

    @Test
    public void shouldReturnPlaylistFromStorageIfNotCached() {
        when(playlistStorage.loadPlaylistWithTracksAsync(anyLong())).thenReturn(Observable.from(playlist));

        playlistOperations.loadPlaylist(1).subscribe(observer);

        verify(playlistStorage).loadPlaylistWithTracksAsync(1L);
        verify(observer).onNext(playlist);
    }
}
