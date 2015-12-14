package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import org.junit.Test;

public class CurrentUserChangedEventTest extends AndroidUnitTest {

    @Test
    public void testForLogout() throws Exception {
        CurrentUserChangedEvent event = CurrentUserChangedEvent.forLogout();

        assertThat(event.getKind()).isEqualTo(CurrentUserChangedEvent.USER_REMOVED);
        assertThat(event.getCurrentUser()).isNull();
    }

    @Test
    public void testForUserUpdated() throws Exception {
        final PublicApiUser publicApiUser = ModelFixtures.create(PublicApiUser.class);
        CurrentUserChangedEvent event = CurrentUserChangedEvent.forUserUpdated(publicApiUser);

        assertThat(event.getKind()).isEqualTo(CurrentUserChangedEvent.USER_UPDATED);
        assertThat(event.getCurrentUser()).isEqualTo(publicApiUser.toPropertySet());
    }
}
