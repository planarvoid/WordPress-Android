package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.Event;
import com.soundcloud.android.events.SocialEvent;
import com.soundcloud.android.model.Playlist;
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
import org.mockito.ArgumentCaptor;
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
    private AccountOperations accountOperations;
    @Mock
    private Account account;
    @Mock
    private Observer observer;

    @Before
    public void setup() {
        playlistOperations = new PlaylistOperations(playlistStorage, soundAssociationStorage,
                syncStateManager, accountOperations);
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

        playlistOperations.createNewPlaylist(currentUser, "new playlist", false, firstTrackId, "screen_tag").subscribe(observer);

        verify(syncStateManager).forceToStale(Content.ME_PLAYLISTS);
        verify(observer).onNext(playlist);
    }

    @Test
    public void shouldPublishSocialEventWhenCreatingNewPlaylist() {
        User currentUser = new User();
        long firstTrackId = 1L;

        Observer<SocialEvent> eventObserver = mock(Observer.class);
        Event.SOCIAL.subscribe(eventObserver);
        when(playlistStorage.createNewUserPlaylistAsync(
                currentUser, "new playlist", false, firstTrackId)).thenReturn(Observable.just(playlist));

        playlistOperations.createNewPlaylist(currentUser, "new playlist", false, firstTrackId, "screen_tag").subscribe(observer);

        ArgumentCaptor<SocialEvent> socialEvent = ArgumentCaptor.forClass(SocialEvent.class);
        verify(eventObserver).onNext(socialEvent.capture());
        expect(socialEvent.getValue().getType()).toBe(SocialEvent.TYPE_ADD_TO_PLAYLIST);
        expect(socialEvent.getValue().getAttributes().get("context")).toEqual("screen_tag");
    }

    @Test
    public void shouldAddATrackToExistingPlaylist() {
        when(playlistStorage.addTrackToPlaylist(playlist, 1L)).thenReturn(playlist);

        playlistOperations.addTrackToPlaylist(123L, 1L, "screen_tag").subscribe(observer);

        verify(playlistStorage).addTrackToPlaylist(playlist, 1L);
        verify(observer).onNext(playlist);
    }

    @Test
    public void shouldPublishSocialEventWhenAddingTrackToPlaylist() {
        Observer<SocialEvent> eventObserver = mock(Observer.class);
        Event.SOCIAL.subscribe(eventObserver);
        when(playlistStorage.addTrackToPlaylist(playlist, 1L)).thenReturn(playlist);

        playlistOperations.addTrackToPlaylist(123L, 1L, "screen_tag").subscribe(observer);

        ArgumentCaptor<SocialEvent> socialEvent = ArgumentCaptor.forClass(SocialEvent.class);
        verify(eventObserver).onNext(socialEvent.capture());
        expect(socialEvent.getValue().getType()).toBe(SocialEvent.TYPE_ADD_TO_PLAYLIST);
        expect(socialEvent.getValue().getAttributes().get("context")).toEqual("screen_tag");
    }
}
