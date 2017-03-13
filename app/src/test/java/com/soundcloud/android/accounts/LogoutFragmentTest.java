package com.soundcloud.android.accounts;

import static com.soundcloud.android.testsupport.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.offline.OfflineContentService;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.TestFragmentController;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.subjects.PublishSubject;

import android.support.v4.app.FragmentActivity;

public class LogoutFragmentTest extends AndroidUnitTest {

    private TestFragmentController fragmentController;

    @Mock private AccountOperations accountOperations;
    @Mock private FeatureOperations featureOperations;
    @Mock private Observer observer;

    private TestEventBus eventBus = new TestEventBus();

    @Before
    public void setup() {
        when(accountOperations.logout()).thenReturn(Observable.empty());
        LogoutFragment fragment = new LogoutFragment(eventBus, accountOperations, featureOperations);
        fragmentController = TestFragmentController.of(fragment);
    }

    @Test
    public void shouldRemoveCurrentUserAccountInOnCreate() {
        final PublishSubject<Void> logOutOperation = PublishSubject.create();
        when(accountOperations.logout()).thenReturn(logOutOperation);

        fragmentController.create();

        assertThat(logOutOperation.hasObservers()).isTrue();
    }

    @Test
    public void shouldUnsubscribeFromEventBusInOnDestroy() {
        fragmentController.create();
        fragmentController.destroy();

        eventBus.verifyNoEventsOn(EventQueue.CURRENT_USER_CHANGED);
    }

    @Test
    public void shouldFinishActivityAndTriggerLoginOnCurrentUserRemovedEvent() {
        FragmentActivity activity = fragmentController.getActivity();
        fragmentController.create();

        eventBus.publish(EventQueue.CURRENT_USER_CHANGED, CurrentUserChangedEvent.forLogout());
        verify(accountOperations).triggerLoginFlow(activity);

        assertThat(activity.isFinishing()).isTrue();
    }

    @Test
    public void shouldFinishCurrentActivityWhenAccountRemoveFails() {
        when(accountOperations.logout()).thenReturn(Observable.error(new Exception()));
        FragmentActivity activity = fragmentController.getActivity();
        fragmentController.create();

        assertThat(activity.isFinishing()).isTrue();
    }

    @Test
    public void shouldStopOfflineContentServiceIfFeatureEnabled() {
        when(featureOperations.isOfflineContentEnabled()).thenReturn(true);
        FragmentActivity activity = fragmentController.getActivity();
        fragmentController.create();

        assertThat(activity).nextStartedService()
                            .containsAction("action_stop_download")
                            .startsService(OfflineContentService.class);
    }
}
