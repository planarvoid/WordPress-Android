package com.soundcloud.android.main;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.onboarding.OnboardingOperations;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.DialogInterface;

public class EmailOptInDialogFragmentTest extends AndroidUnitTest {

    @Mock private OnboardingOperations onboardingOperations;

    private TestEventBus eventBus = new TestEventBus();
    private EmailOptInDialogFragment fragment;

    @Before
    public void setUp() throws Exception {
        fragment = new EmailOptInDialogFragment(onboardingOperations, eventBus);
    }

    @Test
    public void shouldPublishDismissEmailOptInEventOnCancel() throws Exception {
        fragment.onCancel(mock(DialogInterface.class));

        OnboardingEvent event = eventBus.firstEventOn(EventQueue.ONBOARDING);
        assertThat(event.getAttributes().get("opt_in")).isEqualTo("dismiss");
    }

}
