package com.soundcloud.android.main;

import static org.mockito.Mockito.inOrder;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class FragmentLifeCycleDispatcherTest {
    @Mock private FragmentLifeCycle lifeCycleComponent1;
    @Mock private FragmentLifeCycle lifeCycleComponent2;
    @Mock private Fragment fragment;
    @Mock private Activity activity;
    private FragmentLifeCycleDispatcher dispatcher;

    @Before
    public void setUp() throws Exception {
        dispatcher = new FragmentLifeCycleDispatcher()
                .add(lifeCycleComponent1)
                .add(lifeCycleComponent2);
    }

    @Test
    public void shouldNotifyOnBind() {
        dispatcher.onBind(fragment);

        InOrder inOrder = inOrder(lifeCycleComponent1, lifeCycleComponent2);
        inOrder.verify(lifeCycleComponent1).onBind(fragment);
        inOrder.verify(lifeCycleComponent2).onBind(fragment);
    }

    @Test
    public void shouldNotifyOnAttach() {
        dispatcher.onAttach(activity);

        InOrder inOrder = inOrder(lifeCycleComponent1, lifeCycleComponent2);
        inOrder.verify(lifeCycleComponent1).onAttach(activity);
        inOrder.verify(lifeCycleComponent2).onAttach(activity);
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

    @Test
    public void shouldNotifyOnDetach() {
        dispatcher.onDetach();

        InOrder inOrder = inOrder(lifeCycleComponent1, lifeCycleComponent2);
        inOrder.verify(lifeCycleComponent1).onDetach();
        inOrder.verify(lifeCycleComponent2).onDetach();
    }

    @Test
    public void shouldNotifyOnViewCreated() {
        final Bundle bundle = new Bundle();
        final View view = new View(Robolectric.application);

        dispatcher.onViewCreated(view, bundle);

        InOrder inOrder = inOrder(lifeCycleComponent1, lifeCycleComponent2);
        inOrder.verify(lifeCycleComponent1).onViewCreated(view, bundle);
        inOrder.verify(lifeCycleComponent2).onViewCreated(view, bundle);
    }

    @Test
    public void shouldNotifyOnDestroyView() {
        dispatcher.onDestroyView();

        InOrder inOrder = inOrder(lifeCycleComponent1, lifeCycleComponent2);
        inOrder.verify(lifeCycleComponent1).onDestroyView();
        inOrder.verify(lifeCycleComponent2).onDestroyView();
    }
}