package com.soundcloud.android.operations;

import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.dao.PlaylistStorage;
import com.soundcloud.android.dao.SoundAssociationStorage;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.service.sync.SyncStateManager;
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

    @Mock
    private PlaylistStorage playlistStorage;
    @Mock
    private SoundAssociationStorage soundAssociationStorage;
    @Mock
    private SyncStateManager syncStateManager;
    @Mock
    private AccountOperations accountOperations;
    @Mock
    private Account account;
    @Mock
    private Observer observer;

    @Before
    public void setup() {
        playlistOperations = new PlaylistOperations(playlistStorage, soundAssociationStorage,
                syncStateManager, accountOperations);
    }

    @Test
    public void shouldCreateNewPlaylist() {
        User currentUser = new User();
        long firstTrackId = 1L;

        Playlist createdPlaylist = new Playlist(1L);
        when(playlistStorage.createNewUserPlaylistAsync(
                currentUser, "new playlist", false, firstTrackId)).thenReturn(Observable.just(createdPlaylist));
        SoundAssociation playlistCreation = new SoundAssociation(createdPlaylist);
        when(soundAssociationStorage.addCreationAsync(refEq(createdPlaylist))).thenReturn(Observable.just(playlistCreation));
        when(accountOperations.getSoundCloudAccount()).thenReturn(account);

        playlistOperations.createNewPlaylist(currentUser, "new playlist", false, firstTrackId).subscribe(observer);

        verify(syncStateManager).forceToStale(Content.ME_PLAYLISTS);
        verify(observer).onNext(createdPlaylist);
    }

}
