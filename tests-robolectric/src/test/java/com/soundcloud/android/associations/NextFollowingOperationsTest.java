package com.soundcloud.android.associations;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.soundcloud.android.associations.UpdateFollowingCommand.UpdateFollowingParams;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.functions.Action0;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import javax.inject.Named;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class NextFollowingOperationsTest {

    private NextFollowingOperations operations;

    @Mock private SyncInitiator syncInitiator;
    @Mock private UpdateFollowingCommand updateFollowingCommand;
    @Captor private ArgumentCaptor<UpdateFollowingParams> commandParamsCaptor;

    private TestEventBus eventBus = new TestEventBus();
    private Scheduler scheduler = Schedulers.immediate();
    private TestSubscriber<PropertySet> observer = new TestSubscriber<>();
    private Urn targetUrn = Urn.forUser(123);

    @Before
    public void setUp() throws Exception {
        operations = new NextFollowingOperations(
                syncInitiator, eventBus, updateFollowingCommand, scheduler);

        when(updateFollowingCommand.toObservable(any(UpdateFollowingParams.class))).thenReturn(Observable.just(5));
    }

    @Test
    public void toggleFollowingAddsNewFollowingAndEmitsEntityChangeSet() {
        operations.toggleFollowing(targetUrn, true).subscribe(observer);

        verify(updateFollowingCommand).toObservable(commandParamsCaptor.capture());
        expect(commandParamsCaptor.getValue().following).toBeTrue();
        expect(commandParamsCaptor.getValue().targetUrn).toEqual(targetUrn);
        expect(observer.getOnNextEvents()).toContainExactly(TestPropertySets.followingEntityChangeSet(targetUrn, 5, true));
    }
}
