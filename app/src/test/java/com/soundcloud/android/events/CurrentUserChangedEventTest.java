package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class CurrentUserChangedEventTest {

    @Test
    public void testForLogout() throws Exception {
        CurrentUserChangedEvent event = CurrentUserChangedEvent.forLogout();
        expect(event.getKind()).toBe(CurrentUserChangedEvent.USER_REMOVED);
        expect(event.getCurrentUser()).toBeNull();
    }

    @Test
    public void testForUserUpdated() throws Exception {
        final User currentUser = new User();
        CurrentUserChangedEvent event = CurrentUserChangedEvent.forUserUpdated(currentUser);
        expect(event.getKind()).toBe(CurrentUserChangedEvent.USER_UPDATED);
        expect(event.getCurrentUser()).toBe(currentUser);
    }
}
