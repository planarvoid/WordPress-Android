package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

public class CurrentUserChangedEventTest extends AndroidUnitTest {

    @Test
    public void testForLogout() throws Exception {
        CurrentUserChangedEvent event = CurrentUserChangedEvent.forLogout();

        assertThat(event.isUserRemoved()).isTrue();
        assertThat(event.getCurrentUserUrn()).isEqualTo(Urn.NOT_SET);
    }

    @Test
    public void testForUserUpdated() throws Exception {
        CurrentUserChangedEvent event = CurrentUserChangedEvent.forUserUpdated(Urn.forUser(123));

        assertThat(event.isUserUpdated()).isTrue();
        assertThat(event.getCurrentUserUrn()).isEqualTo(Urn.forUser(123));
    }
}
