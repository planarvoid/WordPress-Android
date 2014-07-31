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
import android.os.Bundle;

@RunWith(SoundCloudTestRunner.class)
public class LifeCycleDispatcherTest {
    @Mock private LifeCycleComponent lifeCycleComponent1;
    @Mock private LifeCycleComponent lifeCycleComponent2;
    @Mock private Activity activity;
    @Mock private ActionBarController actionBarController;
    private LifeCycleDispatcher.Notifier notifier;

    @Before
    public void setUp() throws Exception {
        notifier = new LifeCycleDispatcher()
                .add(lifeCycleComponent1)
                .add(lifeCycleComponent2)
                .attach(activity, actionBarController);
    }

    @Test
    public void shouldAttachModulesInOrder() {
        InOrder inOrder = inOrder(lifeCycleComponent1, lifeCycleComponent2);
        inOrder.verify(lifeCycleComponent1).attach(activity, actionBarController);
        inOrder.verify(lifeCycleComponent2).attach(activity, actionBarController);
    }

    @Test
    public void shouldNotifyOnCreate() {
        final Bundle bundle = new Bundle();

        notifier.onCreate(bundle);

        InOrder inOrder = inOrder(lifeCycleComponent1, lifeCycleComponent2);
        inOrder.verify(lifeCycleComponent1).onCreate(bundle);
        inOrder.verify(lifeCycleComponent2).onCreate(bundle);
    }

    @Test
    public void shouldNotifyOnStart(){
        notifier.onStart();

        InOrder inOrder = inOrder(lifeCycleComponent1, lifeCycleComponent2);
        inOrder.verify(lifeCycleComponent1).onStart();
        inOrder.verify(lifeCycleComponent2).onStart();
    }

    @Test
    public void shouldNotifyOnResume(){
        notifier.onResume();

        InOrder inOrder = inOrder(lifeCycleComponent1, lifeCycleComponent2);
        inOrder.verify(lifeCycleComponent1).onResume();
        inOrder.verify(lifeCycleComponent2).onResume();
    }

    @Test
    public void shouldNotifyOnPause(){
        notifier.onPause();

        InOrder inOrder = inOrder(lifeCycleComponent1, lifeCycleComponent2);
        inOrder.verify(lifeCycleComponent1).onPause();
        inOrder.verify(lifeCycleComponent2).onPause();
    }

    @Test
    public void shouldNotifyOnStop(){
        notifier.onStop();

        InOrder inOrder = inOrder(lifeCycleComponent1, lifeCycleComponent2);
        inOrder.verify(lifeCycleComponent1).onStop();
        inOrder.verify(lifeCycleComponent2).onStop();
    }

    @Test
    public void shouldNotifyOnSaveInstanceState(){
        final Bundle bundle = new Bundle();

        notifier.onSaveInstanceState(bundle);

        InOrder inOrder = inOrder(lifeCycleComponent1, lifeCycleComponent2);
        inOrder.verify(lifeCycleComponent1).onSaveInstanceState(bundle);
        inOrder.verify(lifeCycleComponent2).onSaveInstanceState(bundle);
    }

    @Test
    public void shouldNotifyOnDestroy(){
        notifier.onDestroy();

        InOrder inOrder = inOrder(lifeCycleComponent1, lifeCycleComponent2);
        inOrder.verify(lifeCycleComponent1).onDestroy();
        inOrder.verify(lifeCycleComponent2).onDestroy();
    }
}