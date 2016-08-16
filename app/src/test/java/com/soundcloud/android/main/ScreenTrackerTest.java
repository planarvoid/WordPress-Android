package com.soundcloud.android.main;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.ActivityReferringEventProvider;
import com.soundcloud.android.analytics.TheTracker;
import com.soundcloud.android.events.ReferringEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class ScreenTrackerTest extends AndroidUnitTest {
    @Mock private ActivityReferringEventProvider referringEventProvider;
    @Mock private EnterScreenDispatcher enterScreenDispatcher;
    @Mock private TheTracker theTracker;
    @Mock private RootActivity activity;
    @Mock private Optional<ReferringEvent> referringEvent;
    private ScreenTracker screenTracker;

    @Before
    public void setUp() throws Exception {
        screenTracker = new ScreenTracker(referringEventProvider, enterScreenDispatcher, theTracker);
    }

    @Test
    public void shouldTrackScreenEventWhenNotUnknown() {
        final Screen screen = Screen.ACTIVITIES;
        when(activity.getScreen()).thenReturn(screen);
        when(referringEventProvider.getReferringEvent()).thenReturn(referringEvent);
        screenTracker.onEnterScreen(activity);
        verify(theTracker).trackScreen(any(ScreenEvent.class), eq(referringEvent));
    }

    @Test
    public void shouldNotTrackScreenEventWhenScreenIsUnknown() {
        final Screen screen = Screen.UNKNOWN;
        when(activity.getScreen()).thenReturn(screen);
        when(referringEventProvider.getReferringEvent()).thenReturn(referringEvent);
        screenTracker.onEnterScreen(activity);
        verifyZeroInteractions(theTracker);
    }
}
