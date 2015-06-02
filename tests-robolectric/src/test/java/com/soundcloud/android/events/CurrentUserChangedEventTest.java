package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
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
        final PublicApiUser publicApiUser = ModelFixtures.create(PublicApiUser.class);
        CurrentUserChangedEvent event = CurrentUserChangedEvent.forUserUpdated(publicApiUser);
        expect(event.getKind()).toBe(CurrentUserChangedEvent.USER_UPDATED);
        expect(event.getCurrentUser()).toEqual(publicApiUser.toPropertySet());
    }
}
