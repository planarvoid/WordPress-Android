package com.soundcloud.android.events;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.soundcloud.android.model.Urn;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CurrentUserChangedEventTest {

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
