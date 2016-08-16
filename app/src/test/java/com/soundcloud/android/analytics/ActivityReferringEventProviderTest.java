package com.soundcloud.android.analytics;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.os.Bundle;

public class ActivityReferringEventProviderTest extends AndroidUnitTest {
    @Mock ReferringEventProvider referringEventProvider;
    private ActivityReferringEventProvider activityReferringEventProvider;

    @Before
    public void setUp() {
        activityReferringEventProvider = new ActivityReferringEventProvider(referringEventProvider);
    }

    @Test
    public void shouldSetupReferringEventOnCreate() {
        activityReferringEventProvider.onCreate(fragmentActivity(), new Bundle());

        verify(referringEventProvider).setupReferringEvent();
    }

    @Test
    public void shouldRestoreReferringEvent() {
        final Bundle bundle = new Bundle();

        activityReferringEventProvider.onRestoreInstanceState(fragmentActivity(), bundle);

        verify(referringEventProvider).restoreReferringEvent(bundle);
    }

    @Test
    public void shouldSaveReferringEvent() {
        final Bundle bundle = new Bundle();

        activityReferringEventProvider.onSaveInstanceState(fragmentActivity(), bundle);

        verify(referringEventProvider).saveReferringEvent(bundle);
    }
}
