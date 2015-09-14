package com.soundcloud.android.accounts;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ServiceInitiator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.support.v7.app.AppCompatActivity;

@RunWith(MockitoJUnitRunner.class)
public class AccountPlaybackControllerTest {
    @Mock private AccountOperations accountOperations;
    @Mock private ServiceInitiator serviceInitiator;
    @Mock private AppCompatActivity activity;

    private AccountPlaybackController lightCycle;

    @Before
    public void setUp() throws Exception {
       lightCycle = new AccountPlaybackController(accountOperations, serviceInitiator);
    }

    @Test
    public void doNothingWhenUserIsLoggedIn() {
        when(accountOperations.isUserLoggedIn()).thenReturn(true);

        lightCycle.onResume(activity);

        verifyNoMoreInteractions(serviceInitiator);
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void resetPlayBackServiceWhenUserIsNotLoggedIn() {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        lightCycle.onResume(activity);

        verify(serviceInitiator).resetPlaybackService();
    }

    @Test
    public void finishActivityWhenUserIsNotLoggedIn() {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        lightCycle.onResume(activity);

        verify(activity).finish();
    }
}