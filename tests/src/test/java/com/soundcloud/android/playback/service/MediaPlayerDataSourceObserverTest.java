package com.soundcloud.android.playback.service;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.playback.service.MediaPlayerDataSourceObserver;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.MockitoAnnotations;

import android.media.MediaPlayer;
import android.net.Uri;

import java.io.IOException;

@RunWith(DefaultTestRunner.class)
public class MediaPlayerDataSourceObserverTest {
    private MediaPlayerDataSourceObserver subject;
    private final Uri uri = Uri.parse("http://test.com");

    @MockitoAnnotations.Mock MediaPlayer mediaPlayer;
    @MockitoAnnotations.Mock MediaPlayer.OnErrorListener errorListener;

    @Before
    public void before() {
        initMocks(this);
        subject = new MediaPlayerDataSourceObserver(mediaPlayer, errorListener);
    }

    @Test
    public void shouldSetDataSourceAndPrepareAsyncWhenReceivingUri() throws Exception {
        subject.onNext(uri);
        InOrder inOrder = inOrder(mediaPlayer);

        inOrder.verify(mediaPlayer).setDataSource(uri.toString());
        inOrder.verify(mediaPlayer).prepareAsync();
        verifyNoMoreInteractions(mediaPlayer);
    }

    @Test
    public void shouldCallErrorListenerIfObserverReceivesThrowable() throws Exception {
        Throwable throwable = mock(Throwable.class);
        subject.onError(throwable);

        verify(errorListener).onError(mediaPlayer, 0, 0);
        verifyZeroInteractions(mediaPlayer);
    }

    @Test
    public void shouldCallErrorListenerIfSetDataSourceErrors() throws Exception {
        doThrow(new IOException("Fail")).when(mediaPlayer).setDataSource(uri.toString());
        subject.onNext(uri);
        verify(errorListener).onError(mediaPlayer, 0, 0);
    }

    @Test
    public void shouldCallErrorListenerIfPrepareAsyncErrors() throws Exception {
        doThrow(new IllegalStateException("Fail")).when(mediaPlayer).prepareAsync();
        subject.onNext(uri);
        verify(errorListener).onError(mediaPlayer, 0, 0);
    }
}
