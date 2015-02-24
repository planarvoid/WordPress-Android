package com.soundcloud.android.image;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.nostra13.universalimageloader.core.assist.FailReason;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.View;

public class ImageListenerUILAdapterTest {

    @Mock
    ImageListener imageListener;

    @Mock
    View view;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void shouldAcceptNullFailReasonOnLoadingFailed() throws Exception {
        ImageListenerUILAdapter adapter =  new ImageListenerUILAdapter(imageListener);
        adapter.onLoadingFailed("http://some-uri", view, null);
        verify(imageListener).onLoadingFailed("http://some-uri", view, null);
    }

    @Test
    public void shouldAcceptNullFailReasonCauseOnLoadingFailed() throws Exception {
        ImageListenerUILAdapter adapter =  new ImageListenerUILAdapter(imageListener);
        FailReason failReason = mock(FailReason.class);
        when(failReason.getCause()).thenReturn(null);

        adapter.onLoadingFailed("http://some-uri", view, failReason);
        verify(imageListener).onLoadingFailed("http://some-uri", view, null);
    }

    @Test
    public void shouldGetMessageForValidReasonCauseOnLoadingFailed() throws Exception {
        ImageListenerUILAdapter adapter =  new ImageListenerUILAdapter(imageListener);
        FailReason failReason = mock(FailReason.class);
        when(failReason.getCause()).thenReturn(mock(java.lang.Throwable.class));

        adapter.onLoadingFailed("http://some-uri", view, failReason);
        verify(imageListener).onLoadingFailed("http://some-uri", view, failReason.getCause().getMessage());
    }
}
