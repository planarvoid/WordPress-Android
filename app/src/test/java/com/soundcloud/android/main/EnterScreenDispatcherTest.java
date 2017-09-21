package com.soundcloud.android.main;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EnterScreenDispatcherTest {
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

        verify(listener).onReenterScreen(rootActivity);
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
        enterScreenDispatcher.onResume(rootActivity);

        verifyZeroInteractions(listener);
    }

    @Test
    public void shouldCallListenerOnPageSelectedWhenActivityPresent() throws Exception {
        when(screenStateProvider.isEnteringScreen()).thenReturn(false);

        enterScreenDispatcher.setListener(listener);
        enterScreenDispatcher.onResume(rootActivity);
        enterScreenDispatcher.onPageSelected(0);

        verify(listener).onEnterScreen(rootActivity, 0);
    }

    @Test
    public void shouldNotCallListenerOnPageSelectedWhenActivityNotPresent() throws Exception {
        enterScreenDispatcher.setListener(listener);
        enterScreenDispatcher.onPageSelected(0);

        verifyZeroInteractions(listener);
    }

    @Test
    public void shouldNotCallListenerOnPageSelectedWhenListenerNotPresent() throws Exception {
        enterScreenDispatcher.onResume(rootActivity);
        enterScreenDispatcher.onPageSelected(0);

        verifyZeroInteractions(listener);
    }
}
