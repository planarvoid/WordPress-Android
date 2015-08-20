package com.soundcloud.android.main;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.mock;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.onboarding.OnboardingOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.DialogInterface;

@RunWith(SoundCloudTestRunner.class)
public class EmailOptInDialogFragmentTest {

    @Mock
    private OnboardingOperations onboardingOperations;

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
        expect(event.getAttributes().get("opt_in")).toEqual("dismiss");
    }

}