package com.soundcloud.android.lightcycle;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

@RunWith(SoundCloudTestRunner.class)
public class ActivityLightCycleDispatcherTest {
    @Mock private ActivityLightCycle lightCycleComponent1;
    @Mock private ActivityLightCycle lightCycleComponent2;
    @Mock private FragmentActivity activity;
    @Mock private ActionBarController actionBarController;
    private ActivityLightCycleDispatcher dispatcher;

    @Before
    public void setUp() throws Exception {
        dispatcher = new ActivityLightCycleDispatcher()
                .add(lightCycleComponent1)
                .add(lightCycleComponent2);
    }

    @Test
    public void dispatchOnCreate() {
        final Bundle bundle = new Bundle();

        dispatcher.onCreate(activity, bundle);

        verify(lightCycleComponent1).onCreate(activity, bundle);
        verify(lightCycleComponent2).onCreate(activity, bundle);
    }

    @Test
    public void dispatchOnNewIntent() {
        final Intent intent = new Intent();

        dispatcher.onNewIntent(activity, intent);

        verify(lightCycleComponent1).onNewIntent(activity, intent);
        verify(lightCycleComponent2).onNewIntent(activity, intent);
    }

    @Test
    public void dispatchOnStart() {
        dispatcher.onStart(activity);

        verify(lightCycleComponent1).onStart(activity);
        verify(lightCycleComponent2).onStart(activity);
    }

    @Test
    public void dispatchOnResume() {
        dispatcher.onResume(activity);

        verify(lightCycleComponent1).onResume(activity);
        verify(lightCycleComponent2).onResume(activity);
    }

    @Test
    public void dispatchOnPause() {
        dispatcher.onPause(activity);

        verify(lightCycleComponent1).onPause(activity);
        verify(lightCycleComponent2).onPause(activity);
    }

    @Test
    public void dispatchOnStop() {
        dispatcher.onStop(activity);

        verify(lightCycleComponent1).onStop(activity);
        verify(lightCycleComponent2).onStop(activity);
    }

    @Test
    public void dispatchOnSaveInstanceState() {
        final Bundle bundle = new Bundle();

        dispatcher.onSaveInstanceState(activity, bundle);

        verify(lightCycleComponent1).onSaveInstanceState(activity, bundle);
        verify(lightCycleComponent2).onSaveInstanceState(activity, bundle);
    }

    @Test
    public void dispatchOnRestoreInstanceState() {
        final Bundle bundle = new Bundle();

        dispatcher.onRestoreInstanceState(activity, bundle);

        verify(lightCycleComponent1).onRestoreInstanceState(activity, bundle);
        verify(lightCycleComponent2).onRestoreInstanceState(activity, bundle);
    }

    @Test
    public void dispatchOnDestroy() {
        dispatcher.onDestroy(activity);

        verify(lightCycleComponent1).onDestroy(activity);
        verify(lightCycleComponent2).onDestroy(activity);
    }

    @Test
    public void dispatchOnlyOnceToDuplicatesComponents() {
        final Bundle bundle = new Bundle();
        dispatcher
                .add(lightCycleComponent1)
                .add(lightCycleComponent1)
                .onCreate(activity, bundle);

        verify(lightCycleComponent1, times(1)).onCreate(activity, bundle);
    }
}