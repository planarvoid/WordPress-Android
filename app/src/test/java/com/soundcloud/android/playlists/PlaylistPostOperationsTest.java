package com.soundcloud.android.playlists;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UrnStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.posts.PostsStorage;
import com.soundcloud.android.rx.RxSignal;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.propeller.TxnResult;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.CompletableSubject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PlaylistPostOperationsTest {

    private final CompletableSubject syncSubject = CompletableSubject.create();
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

        when(syncInitiator.requestSystemSync()).thenReturn(syncSubject);
    }

    @Test
    public void removeShouldRemoveLocalPlaylist() {
        final Urn localPlaylist = Urn.newLocalPlaylist();
        when(postsStorage.removePlaylist(localPlaylist)).thenReturn(Single.just(true));

        TestObserver<RxSignal> testObserver = operations.remove(localPlaylist).test();

        syncSubject.onComplete();

        testObserver.assertValue(RxSignal.SIGNAL).assertComplete();

        verify(postsStorage).removePlaylist(localPlaylist);
    }

    @Test
    public void removeShouldMarkForRemovalSyncedPlaylist() {
        final Urn playlist = Urn.forPlaylist(123);
        when(postsStorage.markPlaylistPendingRemoval(playlist)).thenReturn(Single.just(new TxnResult()));

        TestObserver<RxSignal> testObserver = operations.remove(playlist).test();

        syncSubject.onComplete();

        testObserver.assertValue(RxSignal.SIGNAL).assertComplete();

        verify(postsStorage).markPlaylistPendingRemoval(playlist);
    }

    @Test
    public void removeShouldTriggerMyPlaylistSync() {
        final Urn localPlaylist = Urn.newLocalPlaylist();
        when(postsStorage.removePlaylist(localPlaylist)).thenReturn(Single.just(true));

        TestObserver<RxSignal> testObserver = operations.remove(localPlaylist).test();

        syncSubject.onComplete();

        testObserver.assertValue(RxSignal.SIGNAL).assertComplete();

        verify(syncInitiator).requestSystemSync();
    }

    @Test
    public void shouldPublishEntityChangedEventAfterRemovingPlaylist() {
        final Urn playlist = Urn.forPlaylist(213L);
        when(postsStorage.markPlaylistPendingRemoval(playlist)).thenReturn(Single.just(new TxnResult()));

        TestObserver<RxSignal> testObserver = operations.remove(playlist).test();

        syncSubject.onComplete();

        testObserver.assertValue(RxSignal.SIGNAL).assertComplete();

        final UrnStateChangedEvent event = eventBus.lastEventOn(EventQueue.URN_STATE_CHANGED);
        assertThat(event.kind()).isEqualTo(UrnStateChangedEvent.Kind.ENTITY_DELETED);
        assertThat(event.urns().iterator().next()).isEqualTo(playlist);
    }
}
