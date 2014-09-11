package com.soundcloud.android.main;

import static org.mockito.Matchers.same;
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
public class DefaultFragmentTest {

    private DefaultFragment fragment = new DefaultFragment() {
        @Override
        public void addLifeCycleComponents() {
            addLifeCycleComponent(component);
        }
    };

    @Mock FragmentLifeCycle component;
    @Mock Activity activity;

    @Before
    public void setup() {
        // registers all life cycle components, so need to do it before every test
        fragment.onAttach(activity);
    }

    @Test
    public void shouldForwardOnAttach() {
        verify(component).onAttach(same(activity));
    }

    @Test
    public void shouldForwardOnDetach() {
        fragment.onDetach();
        verify(component).onDetach();
    }

    @Test
    public void shouldForwardOnCreateAndBindItself() {
        final Bundle savedInstanceState = new Bundle();
        fragment.onCreate(savedInstanceState);

        InOrder inOrder = inOrder(component);
        inOrder.verify(component).onBind(fragment);
        inOrder.verify(component).onCreate(savedInstanceState);
    }

    @Test
    public void shouldForwardOnDestroy() {
        fragment.onDestroy();
        verify(component).onDestroy();
    }

    @Test
    public void shouldForwardOnStart() {
        fragment.onStart();
        verify(component).onStart();
    }

    @Test
    public void shouldForwardOnStop() {
        fragment.onStop();
        verify(component).onStop();
    }

    @Test
    public void shouldForwardOnResume() {
        fragment.onResume();
        verify(component).onResume();
    }

    @Test
    public void shouldForwardOnPause() {
        fragment.onPause();
        verify(component).onPause();
    }

    @Test
    public void shouldForwardOnViewCreated() {
        final Bundle savedInstanceState = new Bundle();
        View view = new View(Robolectric.application);
        fragment.onViewCreated(view, savedInstanceState);
        verify(component).onViewCreated(view, savedInstanceState);
    }

    @Test
    public void shouldForwardOnDestroyView() {
        fragment.onDestroyView();
        verify(component).onDestroyView();
    }
}