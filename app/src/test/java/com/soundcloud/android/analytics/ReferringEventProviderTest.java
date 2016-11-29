package com.soundcloud.android.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.ReferringEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.os.Bundle;
import android.os.Parcelable;

public class ReferringEventProviderTest extends AndroidUnitTest {
    public static final String ID = "id";
    public static final String KIND = "kind";
    public static final Optional<ReferringEvent> REFERRING_EVENT = Optional.of(ReferringEvent.create(ID, KIND));
    public static final Optional<ReferringEvent> REFERRING_EVENT_TWO = Optional.of(ReferringEvent.create(ID, KIND));

    @Mock TrackingStateProvider trackingStateProvider;
    private ReferringEventProvider referringEventProvider;

    @Before
    public void setUp() throws Exception {
        when(trackingStateProvider.getLastEvent()).thenReturn(REFERRING_EVENT);

        referringEventProvider = new ReferringEventProvider(trackingStateProvider);
    }

    @Test
    public void shouldCaptureLatestTrackingStateOnSetup() throws Exception {
        referringEventProvider.setupReferringEvent();

        assertThat(referringEventProvider.getReferringEvent()).isEqualTo(REFERRING_EVENT);
    }

    @Test
    public void shouldSaveReferringEventInABundle() throws Exception {
        final Bundle bundle = new Bundle();

        referringEventProvider.setupReferringEvent();
        referringEventProvider.saveReferringEvent(bundle);

        assertThat(bundle.<Parcelable>getParcelable(ReferringEvent.REFERRING_EVENT_KEY)).isEqualTo(REFERRING_EVENT.get());
    }

    @Test
    public void shouldRestoreReferringEventFromBundle() throws Exception {
        final Bundle bundle = new Bundle();
        bundle.putParcelable(ReferringEvent.REFERRING_EVENT_KEY, REFERRING_EVENT_TWO.get());

        referringEventProvider.setupReferringEvent();
        referringEventProvider.restoreReferringEvent(bundle);

        assertThat(referringEventProvider.getReferringEvent()).isEqualTo(REFERRING_EVENT_TWO);
    }
}
