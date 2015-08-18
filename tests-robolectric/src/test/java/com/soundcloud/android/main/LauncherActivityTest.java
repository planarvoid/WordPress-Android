package com.soundcloud.android.main;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.rx.eventbus.EventBus;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Intent;

@RunWith(SoundCloudTestRunner.class)
public class LauncherActivityTest {

    private LauncherActivity launcherActivity;

    @Mock private AccountOperations accountOperations;
    @Mock private EventBus eventBus;

    @Before
    public void setUp() throws Exception {
        launcherActivity = new LauncherActivity(accountOperations, eventBus);
    }

    @Test
    public void triggersLoginFlowWithNoAccount() throws Exception {
        when(accountOperations.isUserLoggedIn()).thenReturn(false);

        launcherActivity.onResume();

        verify(accountOperations).triggerLoginFlow(launcherActivity);
    }

    @Test
    public void startsMainActivityWithExtrasIfAccountExists() throws Exception {
        when(accountOperations.isUserLoggedIn()).thenReturn(true);

        final Intent newIntent = new Intent();
        shadowOf(newIntent).putExtra("someKey","someValue");
        shadowOf(launcherActivity).setIntent(newIntent);

        launcherActivity.onResume();
        Robolectric.runUiThreadTasksIncludingDelayedTasks();

        Intent startedActivity = Robolectric.getShadowApplication().getNextStartedActivity();
        expect(startedActivity.getStringExtra("someKey")).toBe("someValue");
    }


}