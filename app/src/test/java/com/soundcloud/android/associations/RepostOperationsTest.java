package com.soundcloud.android.associations;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isPublicApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.io.IOException;

public class RepostOperationsTest extends AndroidUnitTest {

    private static final int REPOST_COUNT = 3;
    private static final int UNPOST_COUNT = 2;
    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(123L);

    private RepostOperations operations;
    private TestObserver<RepostsStatusEvent.RepostStatus> testObserver = new TestObserver<>();
    private TestEventBus eventBus = new TestEventBus();

    @Mock private RepostStorage repostStorage;
    @Mock private Command<Urn, Integer> addRepost;
    @Mock private Command<Urn, Integer> removeRepost;
    @Mock private ApiClientRx apiClientRx;
    @Mock private WriteResult writeResult;

    @Before
    public void setUp() throws Exception {
        operations = new RepostOperations(repostStorage, apiClientRx, Schedulers.immediate(), eventBus);
        when(repostStorage.addRepost()).thenReturn(addRepost);
        when(addRepost.toObservable(TRACK_URN)).thenReturn(Observable.just(REPOST_COUNT));
        when(addRepost.toObservable(PLAYLIST_URN)).thenReturn(Observable.just(REPOST_COUNT));

        when(repostStorage.removeRepost()).thenReturn(removeRepost);
        when(removeRepost.toObservable(TRACK_URN)).thenReturn(Observable.just(UNPOST_COUNT));
        when(removeRepost.toObservable(PLAYLIST_URN)).thenReturn(Observable.just(UNPOST_COUNT));

    }

    @Test
    public void shouldStoreTrackRepostAndPushToApi() throws Exception {
        when(apiClientRx.response(argThat(isPublicApiRequestTo("PUT", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.just(TestApiResponses.status(200)));

        operations.toggleRepost(TRACK_URN, true).subscribe(testObserver);

        assertThat(testObserver.getOnNextEvents()).containsExactly(RepostsStatusEvent.RepostStatus.createReposted(TRACK_URN, REPOST_COUNT));
    }

    @Test
    public void shouldStorePlaylistRepostAndPushToApi() throws Exception {
        when(apiClientRx.response(argThat(isPublicApiRequestTo("PUT", "/e1/me/playlist_reposts/123"))))
                .thenReturn(Observable.just(TestApiResponses.status(200)));

        operations.toggleRepost(PLAYLIST_URN, true).subscribe(testObserver);

        assertThat(testObserver.getOnNextEvents()).containsExactly(RepostsStatusEvent.RepostStatus.createReposted(PLAYLIST_URN, REPOST_COUNT));
    }

    @Test
    public void shouldPublishEntityChangedEventAfterSuccessfulPushToAdi() throws Exception {
        when(apiClientRx.response(argThat(isPublicApiRequestTo("PUT", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.just(TestApiResponses.status(200)));

        operations.toggleRepost(TRACK_URN, true).subscribe(testObserver);

        final RepostsStatusEvent event = eventBus.lastEventOn(EventQueue.REPOST_CHANGED);
        final RepostsStatusEvent.RepostStatus next = event.reposts().values().iterator().next();
        assertThat(next.urn()).isEqualTo(TRACK_URN);
        assertThat(next.isReposted()).isEqualTo(true);
        assertThat(next.repostCount().get()).isEqualTo(REPOST_COUNT);
    }

    @Test
    public void shouldNotPushRepostToApiIfStorageCallFailed() throws Exception {
        when(addRepost.toObservable(TRACK_URN)).thenReturn(Observable.<Integer>error(mock(PropellerWriteException.class)));

        PublishSubject<ApiResponse> subject = PublishSubject.create();
        when(apiClientRx.response(any(ApiRequest.class))).thenReturn(subject);

        operations.toggleRepost(TRACK_URN, true).subscribe(testObserver);
        subject.onNext(TestApiResponses.ok()); // this must not propagate

        assertThat(testObserver.getOnNextEvents()).isEmpty();
        assertThat(testObserver.getOnErrorEvents()).isNotEmpty();
    }

    @Test
    public void shouldNotPublishRepostedEventIfInsertFailed() throws Exception {
        when(addRepost.toObservable(TRACK_URN)).thenReturn(Observable.<Integer>error(mock(PropellerWriteException.class)));

        operations.toggleRepost(TRACK_URN, true).subscribe(testObserver);

        final RepostsStatusEvent event = eventBus.lastEventOn(EventQueue.REPOST_CHANGED);
        final RepostsStatusEvent.RepostStatus next = event.reposts().values().iterator().next();
        assertThat(next.urn()).isEqualTo(TRACK_URN);
        assertThat(next.isReposted()).isEqualTo(false);
    }

    @Test
    public void shouldRollbackStoredRepostAfterFailedApiCall() throws Exception {
        when(apiClientRx.response(argThat(isPublicApiRequestTo("PUT", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.<ApiResponse>error(new IOException()));

        operations.toggleRepost(TRACK_URN, true).subscribe(testObserver);

        verify(removeRepost).toObservable(TRACK_URN);
        assertThat(testObserver.getOnNextEvents()).containsExactly(RepostsStatusEvent.RepostStatus.createUnposted(TRACK_URN, UNPOST_COUNT));
    }

    @Test
    public void shouldPublishUnpostedEventAfterRepostFailedApiCall() throws Exception {
        when(apiClientRx.response(argThat(isPublicApiRequestTo("PUT", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.<ApiResponse>error(new IOException()));

        operations.toggleRepost(TRACK_URN, true).subscribe(testObserver);

        final RepostsStatusEvent event = eventBus.lastEventOn(EventQueue.REPOST_CHANGED);
        final RepostsStatusEvent.RepostStatus next = event.reposts().values().iterator().next();
        assertThat(next.urn()).isEqualTo(TRACK_URN);
        assertThat(next.isReposted()).isEqualTo(false);
    }

    @Test
    public void shouldStoreTrackUnpostAndPushToApi() throws Exception {
        when(apiClientRx.response(argThat(isPublicApiRequestTo("DELETE", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.just(TestApiResponses.status(200)));

        operations.toggleRepost(TRACK_URN, false).subscribe(testObserver);

        assertThat(testObserver.getOnNextEvents()).containsExactly(RepostsStatusEvent.RepostStatus.createUnposted(TRACK_URN, UNPOST_COUNT));
    }

    @Test
    public void shouldStorePlaylistUnpostAndPushToApi() throws Exception {
        when(apiClientRx.response(argThat(isPublicApiRequestTo("DELETE", "/e1/me/playlist_reposts/123"))))
                .thenReturn(Observable.just(TestApiResponses.status(200)));

        operations.toggleRepost(PLAYLIST_URN, false).subscribe(testObserver);

        assertThat(testObserver.getOnNextEvents()).containsExactly(RepostsStatusEvent.RepostStatus.createUnposted(PLAYLIST_URN, UNPOST_COUNT));
    }

    @Test
    public void shouldPublishEntityChangedEventForUnboltAfterSuccessfulPushToApi() throws Exception {
        when(apiClientRx.response(argThat(isPublicApiRequestTo("DELETE", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.just(TestApiResponses.status(200)));

        operations.toggleRepost(TRACK_URN, false).subscribe(testObserver);

        final RepostsStatusEvent event = eventBus.lastEventOn(EventQueue.REPOST_CHANGED);
        final RepostsStatusEvent.RepostStatus next = event.reposts().values().iterator().next();
        assertThat(next.urn()).isEqualTo(TRACK_URN);
        assertThat(next.isReposted()).isEqualTo(false);
    }

    @Test
    public void shouldPublishRepostedEventIfRepostRemovalFailed() throws Exception {
        when(removeRepost.toObservable(TRACK_URN)).thenReturn(Observable.<Integer>error(mock(PropellerWriteException.class)));

        operations.toggleRepost(TRACK_URN, false).subscribe(testObserver);

        final RepostsStatusEvent event = eventBus.lastEventOn(EventQueue.REPOST_CHANGED);
        final RepostsStatusEvent.RepostStatus next = event.reposts().values().iterator().next();
        assertThat(next.urn()).isEqualTo(TRACK_URN);
        assertThat(next.isReposted()).isEqualTo(true);
    }

    @Test
    public void shouldRollbackRepostRemovalAfterFailedRemoteRemoval() throws Exception {
        when(apiClientRx.response(argThat(isPublicApiRequestTo("DELETE", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.<ApiResponse>error(new IOException()));

        operations.toggleRepost(TRACK_URN, false).subscribe(testObserver);

        verify(addRepost).toObservable(TRACK_URN);
        assertThat(testObserver.getOnNextEvents()).containsExactly(RepostsStatusEvent.RepostStatus.createReposted(TRACK_URN, REPOST_COUNT));
    }

    @Test
    public void shouldPublishRepostedEventAfterFailedRemoteRemoval() throws Exception {
        when(apiClientRx.response(argThat(isPublicApiRequestTo("DELETE", "/e1/me/track_reposts/123"))))
                .thenReturn(Observable.<ApiResponse>error(new IOException()));

        operations.toggleRepost(TRACK_URN, false).subscribe(testObserver);

        final RepostsStatusEvent event = eventBus.lastEventOn(EventQueue.REPOST_CHANGED);
        final RepostsStatusEvent.RepostStatus next = event.reposts().values().iterator().next();
        assertThat(next.urn()).isEqualTo(TRACK_URN);
        assertThat(next.isReposted()).isEqualTo(true);
        assertThat(next.repostCount().get()).isEqualTo(REPOST_COUNT);
    }


}
