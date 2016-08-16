package com.soundcloud.android.analytics;

import static org.mockito.Mockito.verify;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

public class FragmentReferringEventProviderTest extends AndroidUnitTest {
    @Mock ReferringEventProvider referringEventProvider;
    private FragmentReferringEventProvider fragmentReferringEventProvider;

    @Before
    public void setUp() {
        fragmentReferringEventProvider = new FragmentReferringEventProvider(referringEventProvider);
    }

    @Test
    public void shouldSetupReferringEventOnViewCreatedWithNullBundle() {
        fragmentReferringEventProvider.onViewCreated(new Fragment(), new View(context()), null);

        verify(referringEventProvider).setupReferringEvent();
    }

    @Test
    public void shouldRestoreReferringEventOnViewCreatedWithPopulatedBundle() {
        final Bundle savedInstanceState = new Bundle();

        fragmentReferringEventProvider.onViewCreated(new Fragment(), new View(context()), savedInstanceState);

        verify(referringEventProvider).restoreReferringEvent(savedInstanceState);
    }

    @Test
    public void shouldSaveReferringEvent() {
        final Bundle bundle = new Bundle();

        fragmentReferringEventProvider.onSaveInstanceState(new Fragment(), bundle);

        verify(referringEventProvider).saveReferringEvent(bundle);
    }
}
