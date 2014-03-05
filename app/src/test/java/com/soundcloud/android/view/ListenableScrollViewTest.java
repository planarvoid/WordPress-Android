package com.soundcloud.android.view;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class ListenableScrollViewTest {

    private ListenableScrollView scrollView = new ListenableScrollView(Robolectric.application);

    @Mock
    private ListenableScrollView.OnScrollListener listener;

    @Test
    public void shouldCallBackToAttachedListener() {
        scrollView.setOnScrollListener(listener);
        scrollView.onScrollChanged(0, 1, 0, 2);
        verify(listener).onScroll(1, 2);
    }

    @Test
    public void shouldIgnoreScrollEventsWithoutListener() {
        scrollView.onScrollChanged(0, 1, 0, 2);
        verifyZeroInteractions(listener);
    }
}
