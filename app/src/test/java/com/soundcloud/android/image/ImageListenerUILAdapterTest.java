package com.soundcloud.android.image;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nostra13.universalimageloader.core.assist.FailReason;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.view.View;

@RunWith(MockitoJUnitRunner.class)
public class ImageListenerUILAdapterTest {

    @Mock ImageListener imageListener;
    @Mock View view;

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
