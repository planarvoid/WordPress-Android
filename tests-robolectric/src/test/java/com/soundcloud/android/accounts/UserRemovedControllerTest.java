package com.soundcloud.android.accounts;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.support.v7.app.AppCompatActivity;

@RunWith(SoundCloudTestRunner.class)
public class UserRemovedControllerTest {
    @Mock private AppCompatActivity activity;
    private TestEventBus eventBus;
    private UserRemovedController lightCycle;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        lightCycle = new UserRemovedController(eventBus);
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

        eventBus.publish(EventQueue.CURRENT_USER_CHANGED, CurrentUserChangedEvent.forUserUpdated(ModelFixtures.create(PublicApiUser.class)));
        verify(activity, never()).finish();
    }
}