package com.soundcloud.android.likes;

import static com.soundcloud.android.likes.UpdateLikeCommand.UpdateLikeParams;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LikesStatusEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import edu.emory.mathcs.backport.java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.Scheduler;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.io.IOException;

public class LikeOperationsTest extends AndroidUnitTest {

    private LikeOperations operations;

    @Mock private UpdateLikeCommand updateLikeCommand;
    @Mock private SyncInitiator syncInitiator;
    @Captor private ArgumentCaptor<UpdateLikeParams> commandParamsCaptor;

    private TestEventBus eventBus = new TestEventBus();
    private Scheduler scheduler = Schedulers.immediate();
    private TestSubscriber<LikeOperations.LikeResult> observer = new TestSubscriber<>();
    private Urn targetUrn = Urn.forTrack(123);

    @Before
    public void setUp() throws Exception {
        operations = new LikeOperations(
                updateLikeCommand,
                syncInitiator,
                eventBus,
                scheduler);
        when(updateLikeCommand.toObservable(any(UpdateLikeParams.class))).thenReturn(Observable.just(5));
    }

    @Test
    public void toggleLikeAddsNewLikeAndEmitsEntityChangeSet() {
        operations.toggleLike(targetUrn, true).subscribe(observer);

        verify(updateLikeCommand).toObservable(commandParamsCaptor.capture());
        assertThat(commandParamsCaptor.getValue().addLike).isTrue();
        assertThat(commandParamsCaptor.getValue().targetUrn).isEqualTo(targetUrn);
        assertThat(observer.getOnNextEvents()).containsExactly(LikeOperations.LikeResult.LIKE_SUCCEEDED);
    }

    @Test
    public void toggleLikeAddsNewLikeAndEmitsErrorResult() {
        when(updateLikeCommand.toObservable(any(UpdateLikeParams.class))).thenReturn(Observable.error(new IOException()));

        operations.toggleLike(targetUrn, true).subscribe(observer);

        verify(updateLikeCommand).toObservable(commandParamsCaptor.capture());
        assertThat(commandParamsCaptor.getValue().addLike).isTrue();
        assertThat(commandParamsCaptor.getValue().targetUrn).isEqualTo(targetUrn);
        assertThat(observer.getOnNextEvents()).containsExactly(LikeOperations.LikeResult.LIKE_FAILED);
    }

    @Test
    public void toggleLikeRemovesLikeAndEmitsEntityChangeSet() {
        operations.toggleLike(targetUrn, false).subscribe(observer);

        verify(updateLikeCommand).toObservable(commandParamsCaptor.capture());
        assertThat(commandParamsCaptor.getValue().addLike).isFalse();
        assertThat(commandParamsCaptor.getValue().targetUrn).isEqualTo(targetUrn);
        assertThat(observer.getOnNextEvents()).containsExactly(LikeOperations.LikeResult.UNLIKE_SUCCEEDED);
    }

    @Test
    public void toggleLikeRemovesLikeAndEmitsFailedResult() {
        when(updateLikeCommand.toObservable(any(UpdateLikeParams.class))).thenReturn(Observable.error(new IOException()));

        operations.toggleLike(targetUrn, false).subscribe(observer);

        verify(updateLikeCommand).toObservable(commandParamsCaptor.capture());
        assertThat(commandParamsCaptor.getValue().addLike).isFalse();
        assertThat(commandParamsCaptor.getValue().targetUrn).isEqualTo(targetUrn);
        assertThat(observer.getOnNextEvents()).containsExactly(LikeOperations.LikeResult.UNLIKE_FAILED);
    }

    @Test
    public void togglingLikePublishesPlayableChangedEvent() {
        operations.toggleLike(targetUrn, true).subscribe(observer);

        LikesStatusEvent event = eventBus.firstEventOn(EventQueue.LIKE_CHANGED);
        assertThat(event.likes()).isEqualTo(Collections.singletonMap(targetUrn, LikesStatusEvent.LikeStatus.create(targetUrn, true, 5)));
    }

    @Test
    public void togglingLikeRequestsSystemSync() {
        operations.toggleLike(targetUrn, true).subscribe(observer);

        verify(syncInitiator).requestSystemSync();
    }

}
