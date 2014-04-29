package com.soundcloud.android.main;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.onboarding.OnboardingOperations;
import com.soundcloud.android.robolectric.EventMonitor;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.DialogInterface;

@RunWith(SoundCloudTestRunner.class)
public class EmailOptInDialogFragmentTest {

    @Mock
    private OnboardingOperations onboardingOperations;

    @Mock
    private EventBus eventBus;

    private EmailOptInDialogFragment fragment;
    private EventMonitor eventMonitor;

    @Before
    public void setUp() throws Exception {
        eventMonitor = EventMonitor.on(eventBus);
        fragment = new EmailOptInDialogFragment(onboardingOperations, eventBus);
    }

    @Test
    public void shouldPublishDismissEmailOptInEventOnCancel() throws Exception {
        fragment.onCancel(mock(DialogInterface.class));
        OnboardingEvent event = eventMonitor.verifyEventOn(EventQueue.ONBOARDING);
        expect(event.getAttributes().get("opt_in")).toEqual("dismiss");
    }

}