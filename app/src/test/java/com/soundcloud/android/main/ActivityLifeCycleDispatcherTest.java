package com.soundcloud.android.main;

import static org.mockito.Mockito.inOrder;

import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

@RunWith(SoundCloudTestRunner.class)
public class ActivityLifeCycleDispatcherTest {
    @Mock private ActivityLifeCycle lifeCycleComponent1;
    @Mock private ActivityLifeCycle lifeCycleComponent2;
    @Mock private Activity activity;
    @Mock private ActionBarController actionBarController;
    private ActivityLifeCycleDispatcher dispatcher;

    @Before
    public void setUp() throws Exception {
        dispatcher = new ActivityLifeCycleDispatcher.Builder<>()
                .add(lifeCycleComponent1)
                .add(lifeCycleComponent2)
                .build();
    }

    @Test
    public void shouldNotifyOnBind() {
        dispatcher.onBind(activity);

        InOrder inOrder = inOrder(lifeCycleComponent1, lifeCycleComponent2);
        inOrder.verify(lifeCycleComponent1).onBind(activity);
        inOrder.verify(lifeCycleComponent2).onBind(activity);
    }

    @Test
    public void shouldNotifyOnCreate() {
        final Bundle bundle = new Bundle();

        dispatcher.onCreate(bundle);

        InOrder inOrder = inOrder(lifeCycleComponent1, lifeCycleComponent2);
        inOrder.verify(lifeCycleComponent1).onCreate(bundle);
        inOrder.verify(lifeCycleComponent2).onCreate(bundle);
    }

    @Test
    public void shouldNotifyOnNewIntent() {
        final Intent intent = new Intent();

        dispatcher.onNewIntent(intent);

        InOrder inOrder = inOrder(lifeCycleComponent1, lifeCycleComponent2);
        inOrder.verify(lifeCycleComponent1).onNewIntent(intent);
        inOrder.verify(lifeCycleComponent2).onNewIntent(intent);
    }

    @Test
    public void shouldNotifyOnStart() {
        dispatcher.onStart();

        InOrder inOrder = inOrder(lifeCycleComponent1, lifeCycleComponent2);
        inOrder.verify(lifeCycleComponent1).onStart();
        inOrder.verify(lifeCycleComponent2).onStart();
    }

    @Test
    public void shouldNotifyOnResume() {
        dispatcher.onResume();

        InOrder inOrder = inOrder(lifeCycleComponent1, lifeCycleComponent2);
        inOrder.verify(lifeCycleComponent1).onResume();
        inOrder.verify(lifeCycleComponent2).onResume();
    }

    @Test
    public void shouldNotifyOnPause() {
        dispatcher.onPause();

        InOrder inOrder = inOrder(lifeCycleComponent1, lifeCycleComponent2);
        inOrder.verify(lifeCycleComponent1).onPause();
        inOrder.verify(lifeCycleComponent2).onPause();
    }

    @Test
    public void shouldNotifyOnStop() {
        dispatcher.onStop();

        InOrder inOrder = inOrder(lifeCycleComponent1, lifeCycleComponent2);
        inOrder.verify(lifeCycleComponent1).onStop();
        inOrder.verify(lifeCycleComponent2).onStop();
    }

    @Test
    public void shouldNotifyOnSaveInstanceState() {
        final Bundle bundle = new Bundle();

        dispatcher.onSaveInstanceState(bundle);

        InOrder inOrder = inOrder(lifeCycleComponent1, lifeCycleComponent2);
        inOrder.verify(lifeCycleComponent1).onSaveInstanceState(bundle);
        inOrder.verify(lifeCycleComponent2).onSaveInstanceState(bundle);
    }

    @Test
    public void shouldNotifyOnRestoreInstanceState() {
        final Bundle bundle = new Bundle();

        dispatcher.onRestoreInstanceState(bundle);

        InOrder inOrder = inOrder(lifeCycleComponent1, lifeCycleComponent2);
        inOrder.verify(lifeCycleComponent1).onRestoreInstanceState(bundle);
        inOrder.verify(lifeCycleComponent2).onRestoreInstanceState(bundle);
    }

    @Test
    public void shouldNotifyOnDestroy() {
        dispatcher.onDestroy();

        InOrder inOrder = inOrder(lifeCycleComponent1, lifeCycleComponent2);
        inOrder.verify(lifeCycleComponent1).onDestroy();
        inOrder.verify(lifeCycleComponent2).onDestroy();
    }

}