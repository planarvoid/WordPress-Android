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
public class LifecycleDispatcherTest {
    @Mock private LifecycleComponent lifecycleComponent1;
    @Mock private LifecycleComponent lifecycleComponent2;
    @Mock private Activity activity;
    @Mock private ActionBarController actionBarController;
    private LifecycleDispatcher.Notifier notifier;

    @Before
    public void setUp() throws Exception {
        notifier = new LifecycleDispatcher()
                .add(lifecycleComponent1)
                .add(lifecycleComponent2)
                .attach(activity, actionBarController);
    }

    @Test
    public void shouldAttachModulesInOrder() {
        InOrder inOrder = inOrder(lifecycleComponent1, lifecycleComponent2);
        inOrder.verify(lifecycleComponent1).attach(activity, actionBarController);
        inOrder.verify(lifecycleComponent2).attach(activity, actionBarController);
    }

    @Test
    public void shouldNotifyOnCreate() {
        final Bundle bundle = new Bundle();

        notifier.onCreate(bundle);

        InOrder inOrder = inOrder(lifecycleComponent1, lifecycleComponent2);
        inOrder.verify(lifecycleComponent1).onCreate(bundle);
        inOrder.verify(lifecycleComponent2).onCreate(bundle);
    }

    @Test
    public void shouldNotifyOnStart(){
        notifier.onStart();

        InOrder inOrder = inOrder(lifecycleComponent1, lifecycleComponent2);
        inOrder.verify(lifecycleComponent1).onStart();
        inOrder.verify(lifecycleComponent2).onStart();
    }

    @Test
    public void shouldNotifyOnResume(){
        notifier.onResume();

        InOrder inOrder = inOrder(lifecycleComponent1, lifecycleComponent2);
        inOrder.verify(lifecycleComponent1).onResume();
        inOrder.verify(lifecycleComponent2).onResume();
    }

    @Test
    public void shouldNotifyOnPause(){
        notifier.onPause();

        InOrder inOrder = inOrder(lifecycleComponent1, lifecycleComponent2);
        inOrder.verify(lifecycleComponent1).onPause();
        inOrder.verify(lifecycleComponent2).onPause();
    }

    @Test
    public void shouldNotifyOnStop(){
        notifier.onStop();

        InOrder inOrder = inOrder(lifecycleComponent1, lifecycleComponent2);
        inOrder.verify(lifecycleComponent1).onStop();
        inOrder.verify(lifecycleComponent2).onStop();
    }

    @Test
    public void shouldNotifyOnSaveInstanceState(){
        final Bundle bundle = new Bundle();

        notifier.onSaveInstanceState(bundle);

        InOrder inOrder = inOrder(lifecycleComponent1, lifecycleComponent2);
        inOrder.verify(lifecycleComponent1).onSaveInstanceState(bundle);
        inOrder.verify(lifecycleComponent2).onSaveInstanceState(bundle);
    }

    @Test
    public void shouldNotifyOnDestroy(){
        notifier.onDestroy();

        InOrder inOrder = inOrder(lifecycleComponent1, lifecycleComponent2);
        inOrder.verify(lifecycleComponent1).onDestroy();
        inOrder.verify(lifecycleComponent2).onDestroy();
    }
}