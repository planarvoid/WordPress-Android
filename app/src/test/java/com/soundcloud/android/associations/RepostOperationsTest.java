package com.soundcloud.android.associations;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isPublicApiRequestTo;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClientRxV2;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.RepostsStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.SingleSubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.IOException;

public class RepostOperationsTest extends AndroidUnitTest {

    private static final int REPOST_COUNT = 3;
    private static final int UNPOST_COUNT = 2;
    private static final Urn TRACK_URN = Urn.forTrack(123L);
    private static final Urn PLAYLIST_URN = Urn.forPlaylist(123L);

    private RepostOperations operations;
    private TestEventBusV2 eventBus = new TestEventBusV2();

    @Mock private RepostStorage repostStorage;
    @Mock private Command<Urn, Integer> addRepost;
    @Mock private Command<Urn, Integer> removeRepost;
    @Mock private ApiClientRxV2 apiClientRx;
    @Mock private WriteResult writeResult;

    @Before
    public void setUp() throws Exception {
        operations = new RepostOperations(repostStorage, apiClientRx, Schedulers.trampoline(), eventBus);
        when(repostStorage.addRepost()).thenReturn(addRepost);
        when(addRepost.toSingle(TRACK_URN)).thenReturn(Single.just(REPOST_COUNT));
        when(addRepost.toSingle(PLAYLIST_URN)).thenReturn(Single.just(REPOST_COUNT));

        when(repostStorage.removeRepost()).thenReturn(removeRepost);
        when(removeRepost.toSingle(TRACK_URN)).thenReturn(Single.just(UNPOST_COUNT));
        when(removeRepost.toSingle(PLAYLIST_URN)).thenReturn(Single.just(UNPOST_COUNT));

    }

    @Test
    public void shouldStoreTrackRepostAndPushToApi() throws Exception {
        when(apiClientRx.response(argThat(isPublicApiRequestTo("PUT", "/e1/me/track_reposts/123"))))
                .thenReturn(Single.just(TestApiResponses.status(200)));

        assertThat(operations.toggleRepost(TRACK_URN, true).test().values()).containsExactly(RepostOperations.RepostResult.REPOST_SUCCEEDED);
    }

    @Test
    public void shouldStorePlaylistRepostAndPushToApi() throws Exception {
        when(apiClientRx.response(argThat(isPublicApiRequestTo("PUT", "/e1/me/playlist_reposts/123"))))
                .thenReturn(Single.just(TestApiResponses.status(200)));


        assertThat(operations.toggleRepost(PLAYLIST_URN, true).test().values()).containsExactly(RepostOperations.RepostResult.REPOST_SUCCEEDED);
    }

    @Test
    public void shouldPublishEntityChangedEventAfterSuccessfulPushToAdi() throws Exception {
        when(apiClientRx.response(argThat(isPublicApiRequestTo("PUT", "/e1/me/track_reposts/123"))))
                .thenReturn(Single.just(TestApiResponses.status(200)));

        operations.toggleRepost(TRACK_URN, true).test();

        final RepostsStatusEvent event = eventBus.lastEventOn(EventQueue.REPOST_CHANGED);
        final RepostsStatusEvent.RepostStatus next = event.reposts().values().iterator().next();
        assertThat(next.urn()).isEqualTo(TRACK_URN);
        assertThat(next.isReposted()).isEqualTo(true);
        assertThat(next.repostCount().get()).isEqualTo(REPOST_COUNT);
    }

    @Test
    public void shouldNotPushRepostToApiIfStorageCallFailed() throws Exception {
        when(addRepost.toSingle(TRACK_URN)).thenReturn(Single.error(mock(PropellerWriteException.class)));

        SingleSubject<ApiResponse> subject = SingleSubject.create();
        when(apiClientRx.response(any(ApiRequest.class))).thenReturn(subject);

        TestObserver<RepostOperations.RepostResult> testObserver = operations.toggleRepost(TRACK_URN, true).test();
        subject.onSuccess(TestApiResponses.ok()); // this must not propagate

        assertThat(testObserver.values()).isEmpty();
        assertThat(testObserver.errors()).isNotEmpty();
    }

    @Test
    public void shouldNotPublishRepostedEventIfInsertFailed() throws Exception {
        when(addRepost.toSingle(TRACK_URN)).thenReturn(Single.error(mock(PropellerWriteException.class)));

        operations.toggleRepost(TRACK_URN, true).test();

        final RepostsStatusEvent event = eventBus.lastEventOn(EventQueue.REPOST_CHANGED);
        final RepostsStatusEvent.RepostStatus next = event.reposts().values().iterator().next();
        assertThat(next.urn()).isEqualTo(TRACK_URN);
        assertThat(next.isReposted()).isEqualTo(false);
    }

    @Test
    public void shouldRollbackStoredRepostAfterFailedApiCall() throws Exception {
        when(apiClientRx.response(argThat(isPublicApiRequestTo("PUT", "/e1/me/track_reposts/123"))))
                .thenReturn(Single.error(new IOException()));

        TestObserver<RepostOperations.RepostResult> testObserver = operations.toggleRepost(TRACK_URN, true).test();

        verify(removeRepost).toSingle(TRACK_URN);
        assertThat(testObserver.values()).containsExactly(RepostOperations.RepostResult.REPOST_FAILED);
    }

    @Test
    public void shouldPublishUnpostedEventAfterRepostFailedApiCall() throws Exception {
        when(apiClientRx.response(argThat(isPublicApiRequestTo("PUT", "/e1/me/track_reposts/123"))))
                .thenReturn(Single.error(new IOException()));

        operations.toggleRepost(TRACK_URN, true).test();

        final RepostsStatusEvent event = eventBus.lastEventOn(EventQueue.REPOST_CHANGED);
        final RepostsStatusEvent.RepostStatus next = event.reposts().values().iterator().next();
        assertThat(next.urn()).isEqualTo(TRACK_URN);
        assertThat(next.isReposted()).isEqualTo(false);
    }

    @Test
    public void shouldStoreTrackUnpostAndPushToApi() throws Exception {
        when(apiClientRx.response(argThat(isPublicApiRequestTo("DELETE", "/e1/me/track_reposts/123"))))
                .thenReturn(Single.just(TestApiResponses.status(200)));

        assertThat(operations.toggleRepost(TRACK_URN, false).test().values()).containsExactly(RepostOperations.RepostResult.UNREPOST_SUCCEEDED);
    }

    @Test
    public void shouldStorePlaylistUnpostAndPushToApi() throws Exception {
        when(apiClientRx.response(argThat(isPublicApiRequestTo("DELETE", "/e1/me/playlist_reposts/123"))))
                .thenReturn(Single.just(TestApiResponses.status(200)));

        assertThat(operations.toggleRepost(PLAYLIST_URN, false).test().values()).containsExactly(RepostOperations.RepostResult.UNREPOST_SUCCEEDED);
    }

    @Test
    public void shouldPublishEntityChangedEventForUnboltAfterSuccessfulPushToApi() throws Exception {
        when(apiClientRx.response(argThat(isPublicApiRequestTo("DELETE", "/e1/me/track_reposts/123"))))
                .thenReturn(Single.just(TestApiResponses.status(200)));

        operations.toggleRepost(TRACK_URN, false).test();

        final RepostsStatusEvent event = eventBus.lastEventOn(EventQueue.REPOST_CHANGED);
        final RepostsStatusEvent.RepostStatus next = event.reposts().values().iterator().next();
        assertThat(next.urn()).isEqualTo(TRACK_URN);
        assertThat(next.isReposted()).isEqualTo(false);
    }

    @Test
    public void shouldPublishRepostedEventIfRepostRemovalFailed() throws Exception {
        when(removeRepost.toSingle(TRACK_URN)).thenReturn(Single.error(mock(PropellerWriteException.class)));

        operations.toggleRepost(TRACK_URN, false).test();

        final RepostsStatusEvent event = eventBus.lastEventOn(EventQueue.REPOST_CHANGED);
        final RepostsStatusEvent.RepostStatus next = event.reposts().values().iterator().next();
        assertThat(next.urn()).isEqualTo(TRACK_URN);
        assertThat(next.isReposted()).isEqualTo(true);
    }

    @Test
    public void shouldRollbackRepostRemovalAfterFailedRemoteRemoval() throws Exception {
        when(apiClientRx.response(argThat(isPublicApiRequestTo("DELETE", "/e1/me/track_reposts/123"))))
                .thenReturn(Single.error(new IOException()));

        TestObserver<RepostOperations.RepostResult> testObserver = operations.toggleRepost(TRACK_URN, false).test();

        verify(addRepost).toSingle(TRACK_URN);
        assertThat(testObserver.values()).containsExactly(RepostOperations.RepostResult.UNREPOST_FAILED);
    }

    @Test
    public void shouldPublishRepostedEventAfterFailedRemoteRemoval() throws Exception {
        when(apiClientRx.response(argThat(isPublicApiRequestTo("DELETE", "/e1/me/track_reposts/123"))))
                .thenReturn(Single.error(new IOException()));

        operations.toggleRepost(TRACK_URN, false).test();

        final RepostsStatusEvent event = eventBus.lastEventOn(EventQueue.REPOST_CHANGED);
        final RepostsStatusEvent.RepostStatus next = event.reposts().values().iterator().next();
        assertThat(next.urn()).isEqualTo(TRACK_URN);
        assertThat(next.isReposted()).isEqualTo(true);
        assertThat(next.repostCount().get()).isEqualTo(REPOST_COUNT);
    }

    @Test
    public void shouldAllowUnpostOn404() throws Exception {
        when(apiClientRx.response(argThat(isPublicApiRequestTo("DELETE", "/e1/me/track_reposts/123"))))
                .thenReturn(Single.error(ApiRequestException.notFound(null, null)));

        assertThat(operations.toggleRepost(TRACK_URN, false).test().values()).containsExactly(RepostOperations.RepostResult.UNREPOST_SUCCEEDED);
    }

}
