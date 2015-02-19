package com.soundcloud.android.lightcycle;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

@RunWith(SoundCloudTestRunner.class)
public class LightCycleSupportFragmentTest {

    private LightCycleSupportFragment fragment;
    @Mock SupportFragmentLightCycle component;
    @Mock Activity activity;

    @Before
    public void setup() {
        // registers all life cycle components, so need to do it before every test
        fragment = new LightCycleSupportFragment();
        fragment.addLifeCycleComponent(component);
        fragment.onCreate(null);
    }

    @Test
    public void shouldForwardOnCreateAndBindItself() {
        final Bundle savedInstanceState = new Bundle();
        fragment.onCreate(savedInstanceState);

        InOrder inOrder = inOrder(component);
        inOrder.verify(component).onCreate(fragment, savedInstanceState);
    }

    @Test
    public void shouldForwardOnDestroy() {
        fragment.onDestroy();
        verify(component).onDestroy(fragment);
    }

    @Test
    public void shouldForwardOnStart() {
        fragment.onStart();
        verify(component).onStart(fragment);
    }

    @Test
    public void shouldForwardOnStop() {
        fragment.onStop();
        verify(component).onStop(fragment);
    }

    @Test
    public void shouldForwardOnResume() {
        fragment.onResume();
        verify(component).onResume(fragment);
    }

    @Test
    public void shouldForwardOnPause() {
        fragment.onPause();
        verify(component).onPause(fragment);
    }

    @Test
    public void shouldForwardOnViewCreated() {
        final Bundle savedInstanceState = new Bundle();
        View view = new View(Robolectric.application);
        fragment.onViewCreated(view, savedInstanceState);
        verify(component).onViewCreated(fragment, view, savedInstanceState);
    }

    @Test
    public void shouldForwardOnDestroyView() {
        fragment.onDestroyView();
        verify(component).onDestroyView(fragment);
    }
}