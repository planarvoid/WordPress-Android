package com.soundcloud.android.accounts;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.support.v4.app.FragmentActivity;

@RunWith(SoundCloudTestRunner.class)
public class UserRemovedLightCycleTest {
    @Mock private FragmentActivity activity;
    private TestEventBus eventBus;
    private UserRemovedLightCycle lightCycle;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        lightCycle = new UserRemovedLightCycle(eventBus);
    }

    @Test
    public void finishActivityWhenUserIsLoggedOut() {
        lightCycle.onCreate(activity, null);

        eventBus.publish(EventQueue.CURRENT_USER_CHANGED, CurrentUserChangedEvent.forLogout());
        verify(activity).finish();
    }

    @Test
    public void stopListeningToEventsWhenActivityDestrouyed() {
        lightCycle.onCreate(activity, null);
        lightCycle.onDestroy(activity);

        eventBus.publish(EventQueue.CURRENT_USER_CHANGED, CurrentUserChangedEvent.forLogout());
        verify(activity, never()).finish();
    }

    @Test
    public void ignoreUpdatedUserEvents() {
        lightCycle.onCreate(activity, null);
        lightCycle.onDestroy(activity);

        eventBus.publish(EventQueue.CURRENT_USER_CHANGED, CurrentUserChangedEvent.forUserUpdated(new PublicApiUser(123L)));
        verify(activity, never()).finish();
    }
}