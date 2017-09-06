package com.soundcloud.android.likes;

import static com.soundcloud.android.likes.UpdateLikeCommand.UpdateLikeParams;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import edu.emory.mathcs.backport.java.util.Collections;
import io.reactivex.Completable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.IOException;

public class LikeOperationsTest extends AndroidUnitTest {

    private LikeOperations operations;

    @Mock private UpdateLikeCommand updateLikeCommand;
    @Mock private SyncInitiator syncInitiator;
    @Captor private ArgumentCaptor<UpdateLikeParams> commandParamsCaptor;

    private TestEventBusV2 eventBus = new TestEventBusV2();
    private Scheduler scheduler = Schedulers.trampoline();
    private Urn targetUrn = Urn.forTrack(123);

    @Before
    public void setUp() throws Exception {
        operations = new LikeOperations(
                updateLikeCommand,
                syncInitiator,
                eventBus,
                scheduler);
        when(updateLikeCommand.toSingle(any(UpdateLikeParams.class))).thenReturn(Single.just(5));
        when(syncInitiator.requestSystemSync()).thenReturn(Completable.complete());
    }

    @Test
    public void toggleLikeAddsNewLikeAndEmitsEntityChangeSet() {
        final TestObserver<LikeOperations.LikeResult> observer = operations.toggleLike(targetUrn, true).test();

        verify(updateLikeCommand).toSingle(commandParamsCaptor.capture());
        assertThat(commandParamsCaptor.getValue().addLike).isTrue();
        assertThat(commandParamsCaptor.getValue().targetUrn).isEqualTo(targetUrn);
        assertThat(observer.values()).containsExactly(LikeOperations.LikeResult.LIKE_SUCCEEDED);
    }

    @Test
    public void toggleLikeAddsNewLikeAndEmitsErrorResult() {
        when(updateLikeCommand.toSingle(any(UpdateLikeParams.class))).thenReturn(Single.error(new IOException()));

        final TestObserver<LikeOperations.LikeResult> observer = operations.toggleLike(targetUrn, true).test();

        verify(updateLikeCommand).toSingle(commandParamsCaptor.capture());
        assertThat(commandParamsCaptor.getValue().addLike).isTrue();
        assertThat(commandParamsCaptor.getValue().targetUrn).isEqualTo(targetUrn);
        assertThat(observer.values()).containsExactly(LikeOperations.LikeResult.LIKE_FAILED);
    }

    @Test
    public void toggleLikeRemovesLikeAndEmitsEntityChangeSet() {
        final TestObserver<LikeOperations.LikeResult> observer = operations.toggleLike(targetUrn, false).test();

        verify(updateLikeCommand).toSingle(commandParamsCaptor.capture());
        assertThat(commandParamsCaptor.getValue().addLike).isFalse();
        assertThat(commandParamsCaptor.getValue().targetUrn).isEqualTo(targetUrn);
        assertThat(observer.values()).containsExactly(LikeOperations.LikeResult.UNLIKE_SUCCEEDED);
    }

    @Test
    public void toggleLikeRemovesLikeAndEmitsFailedResult() {
        when(updateLikeCommand.toSingle(any(UpdateLikeParams.class))).thenReturn(Single.error(new IOException()));

        final TestObserver<LikeOperations.LikeResult> observer = operations.toggleLike(targetUrn, false).test();

        verify(updateLikeCommand).toSingle(commandParamsCaptor.capture());
        assertThat(commandParamsCaptor.getValue().addLike).isFalse();
        assertThat(commandParamsCaptor.getValue().targetUrn).isEqualTo(targetUrn);
        assertThat(observer.values()).containsExactly(LikeOperations.LikeResult.UNLIKE_FAILED);
    }

    @Test
    public void togglingLikePublishesPlayableChangedEvent() {
        operations.toggleLike(targetUrn, true).test();

        LikesStatusEvent event = eventBus.firstEventOn(EventQueue.LIKE_CHANGED);
        assertThat(event.likes()).isEqualTo(Collections.singletonMap(targetUrn, LikesStatusEvent.LikeStatus.create(targetUrn, true, 5)));
    }

    @Test
    public void togglingLikeRequestsSystemSync() {
        operations.toggleLike(targetUrn, true).test();

        verify(syncInitiator).requestSystemSync();
    }

}
