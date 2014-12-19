package com.soundcloud.android.accounts;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.support.v4.app.FragmentActivity;

@RunWith(SoundCloudTestRunner.class)
public class AccountPlaybackControlLightCycleTest {
    @Mock private AccountOperations accountOperations;
    @Mock private PlaybackOperations playbackOperations;
    @Mock private FragmentActivity activity;

    private AccountPlaybackControlLightCycle lightCycle;

    @Before
    public void setUp() throws Exception {
       lightCycle = new AccountPlaybackControlLightCycle(accountOperations, playbackOperations);
    }

    @Test
    public void doNothingWhenUserIsLoggedIn() {
        when(accountOperations.isUserLoggedIn()).thenReturn(true);

        lightCycle.onResume(activity);

        verifyNoMoreInteractions(playbackOperations);
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void resetPlayBackServiceWhenUserIsNotLoggedIn() {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        lightCycle.onResume(activity);

        verify(playbackOperations).resetService();
    }

    @Test
    public void finishActivityWhenUserIsNotLoggedIn() {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        lightCycle.onResume(activity);

        verify(activity).finish();
    }
}