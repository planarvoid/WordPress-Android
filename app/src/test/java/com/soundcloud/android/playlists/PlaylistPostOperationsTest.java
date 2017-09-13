package com.soundcloud.android.playlists;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PlaylistPostOperationsTest {

    private PlaylistPostOperations operations;

    @Mock private PostsStorage postsStorage;
    @Mock private SyncInitiator syncInitiator;
    private TestEventBusV2 eventBus;

    private Scheduler scheduler = Schedulers.trampoline();

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBusV2();
        operations = new PlaylistPostOperations(
                postsStorage,
                scheduler,
                syncInitiator,
                eventBus);

        when(syncInitiator.requestSystemSync()).thenReturn(Completable.complete());
    }

    @Test
    public void removeShouldRemoveLocalPlaylist() {
        final Urn localPlaylist = Urn.newLocalPlaylist();
        when(postsStorage.remove(localPlaylist)).thenReturn(Observable.just(new TxnResult()));

        operations.remove(localPlaylist).subscribe();

        verify(postsStorage).remove(localPlaylist);
    }

    @Test
    public void removeShouldMarkForRemovalSyncedPlaylist() {
        final Urn playlist = Urn.forPlaylist(123);
        when(postsStorage.markPendingRemoval(playlist)).thenReturn(Observable.just(new TxnResult()));

        operations.remove(playlist).subscribe();

        verify(postsStorage).markPendingRemoval(playlist);
    }

    @Test
    public void removeShouldTriggerMyPlaylistSync() {
        final Urn localPlaylist = Urn.newLocalPlaylist();
        when(postsStorage.remove(localPlaylist)).thenReturn(Observable.just(new TxnResult()));

        operations.remove(localPlaylist).subscribe();

        verify(syncInitiator).requestSystemSync();
    }

    @Test
    public void shouldPublishEntityChangedEventAfterRemovingPlaylist() {
        final Urn playlist = Urn.forPlaylist(213L);
        when(postsStorage.markPendingRemoval(playlist)).thenReturn(Observable.just(new TxnResult()));

        operations.remove(playlist).subscribe();

        final UrnStateChangedEvent event = eventBus.lastEventOn(EventQueue.URN_STATE_CHANGED);
        assertThat(event.kind()).isEqualTo(UrnStateChangedEvent.Kind.ENTITY_DELETED);
        assertThat(event.urns().iterator().next()).isEqualTo(playlist);
    }
}
