package com.soundcloud.android.main;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class EnterScreenDispatcherTest extends AndroidUnitTest {
    @Mock ScreenStateProvider screenStateProvider;
    @Mock EnterScreenDispatcher.Listener listener;
    @Mock RootActivity rootActivity;

    private EnterScreenDispatcher enterScreenDispatcher;

    @Before
    public void setUp() throws Exception {
        enterScreenDispatcher = new EnterScreenDispatcher(screenStateProvider);
    }

    @Test
    public void shouldCallListenerOnResumeWhenEnteringScreen() throws Exception {
        when(screenStateProvider.isEnteringScreen()).thenReturn(true);

        enterScreenDispatcher.setListener(listener);
        enterScreenDispatcher.onResume(rootActivity);

        verify(listener).onEnterScreen(rootActivity);
    }

    @Test
    public void shouldNotCallListenerOnResumeWhenNotEnteringScreen() throws Exception {
        when(screenStateProvider.isEnteringScreen()).thenReturn(false);

        enterScreenDispatcher.setListener(listener);
        enterScreenDispatcher.onResume(rootActivity);

        verifyZeroInteractions(listener);
    }

    @Test
    public void shouldNotCallListenerOnResumeWhenListenerNotPresent() throws Exception {
        when(screenStateProvider.isEnteringScreen()).thenReturn(true);

        enterScreenDispatcher.onResume(rootActivity);

        verifyZeroInteractions(listener);
    }

    @Test
    public void shouldCallListenerOnPageSelectedWhenActivityPresent() throws Exception {
        when(screenStateProvider.isEnteringScreen()).thenReturn(false);

        enterScreenDispatcher.setListener(listener);
        enterScreenDispatcher.onResume(rootActivity);
        enterScreenDispatcher.onPageSelected(0);

        verify(listener).onEnterScreen(rootActivity);
    }

    @Test
    public void shouldNotCallListenerOnPageSelectedWhenActivityNotPresent() throws Exception {
        when(screenStateProvider.isEnteringScreen()).thenReturn(false);

        enterScreenDispatcher.setListener(listener);
        enterScreenDispatcher.onPageSelected(0);

        verifyZeroInteractions(listener);
    }

    @Test
    public void shouldNotCallListenerOnPageSelectedWhenListenerNotPresent() throws Exception {
        when(screenStateProvider.isEnteringScreen()).thenReturn(false);

        enterScreenDispatcher.onResume(rootActivity);
        enterScreenDispatcher.onPageSelected(0);

        verifyZeroInteractions(listener);
    }
}
