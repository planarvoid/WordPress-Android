package com.soundcloud.android.accounts;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestEventBus;
import com.soundcloud.android.rx.TestObservables;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

import android.support.v4.app.FragmentActivity;

@RunWith(SoundCloudTestRunner.class)
public class LogoutFragmentTest {

    private LogoutFragment fragment;
    private TestEventBus eventBus = new TestEventBus();

    @Mock
    private AccountOperations accountOperations;

    @Mock
    private Observer observer;

    @Before
    public void setup() {
        fragment = new LogoutFragment(eventBus, accountOperations);
        when(accountOperations.logout()).thenReturn(Observable.<Void>empty());
    }

    @Test
    public void shouldRemoveCurrentUserAccountInOnCreate() {
        TestObservables.MockObservable accountRemovingObservable = TestObservables.emptyObservable();
        when(accountOperations.logout()).thenReturn(accountRemovingObservable);
        fragment.onCreate(null);
        expect(accountRemovingObservable.subscribedTo()).toBeTrue();
    }

    @Test
    public void shouldUnsubscribeFromEventBusInOnDestroy() {
        fragment.onCreate(null);
        fragment.onDestroy();
        eventBus.verifyNoEventsOn(EventQueue.CURRENT_USER_CHANGED);
    }

    @Test
    public void shouldFinishActivityAndTriggerLoginOnCurrentUserRemovedEvent() {
        final FragmentActivity activity = new FragmentActivity();
        shadowOf(fragment).setActivity(activity);

        fragment.onCreate(null);

        eventBus.publish(EventQueue.CURRENT_USER_CHANGED, CurrentUserChangedEvent.forLogout());
        verify(accountOperations).triggerLoginFlow(activity);
        expect(activity.isFinishing()).toBeTrue();
    }

    @Test
    public void shouldFinishCurrentActivityWhenAccountRemoveFails() {
        when(accountOperations.logout()).thenReturn(Observable.<Void>error(new Exception()));
        final FragmentActivity activity = new FragmentActivity();
        shadowOf(fragment).setActivity(activity);

        fragment.onCreate(null);

        expect(activity.isFinishing()).toBeTrue();
    }
}
