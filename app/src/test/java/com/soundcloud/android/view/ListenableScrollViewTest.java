package com.soundcloud.android.view;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.content.Context;

@RunWith(MockitoJUnitRunner.class)
public class ListenableScrollViewTest {

    @Mock private ListenableScrollView.OnScrollListener listener;
    @Mock private Context context;

    private ListenableScrollView scrollView;

    @Before
    public void setUp() {
        scrollView = new ListenableScrollView(context);
    }

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
