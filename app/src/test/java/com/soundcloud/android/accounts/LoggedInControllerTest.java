package com.soundcloud.android.accounts;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.PlaybackServiceController;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.support.v7.app.AppCompatActivity;

@RunWith(MockitoJUnitRunner.class)
public class LoggedInControllerTest {
    @Mock private AccountOperations accountOperations;
    @Mock private PlaybackServiceController serviceController;
    @Mock private AppCompatActivity activity;

    private LoggedInController lightCycle;

    @Before
    public void setUp() throws Exception {
        lightCycle = new LoggedInController(accountOperations, serviceController);
    }

    @Test
    public void doNothingWhenUserIsLoggedIn() {
        when(accountOperations.isUserLoggedIn()).thenReturn(true);

        lightCycle.onResume(activity);

        verifyNoMoreInteractions(serviceController);
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void resetPlayBackServiceWhenUserIsNotLoggedIn() {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        lightCycle.onResume(activity);

        verify(serviceController).resetPlaybackService();
    }

    @Test
    public void finishActivityWhenUserIsNotLoggedIn() {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        lightCycle.onResume(activity);

        verify(activity).finish();
    }
}
