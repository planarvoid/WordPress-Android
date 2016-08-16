package com.soundcloud.android.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.events.ReferringEvent;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;

public class TrackingStateProviderTest {

    public static final String KIND = "kind";
    public static final String ID = "id";
    public static final ReferringEvent REFERRING_EVENT = ReferringEvent.create(ID, KIND);

    private TrackingStateProvider trackingStateProvider;

    @Before
    public void setUp() throws Exception {
        trackingStateProvider = new TrackingStateProvider();
    }

    @Test
    public void shouldBeAbleToUpdateProvider() throws Exception {
        trackingStateProvider.update(REFERRING_EVENT);

        assertThat(trackingStateProvider.getLastEvent()).isEqualTo(Optional.of(REFERRING_EVENT));
    }
}
