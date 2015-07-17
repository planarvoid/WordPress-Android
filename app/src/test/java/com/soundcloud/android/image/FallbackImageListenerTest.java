package com.soundcloud.android.image;

import static com.soundcloud.android.image.ImageOperations.FallbackImageListener;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Sets;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

import android.graphics.Bitmap;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.util.Set;

public class FallbackImageListenerTest extends AndroidUnitTest {

    private final String IMAGE_URI = "http://image.com";
    private final FailReason FAIL_FILE_NOT_FOUND = new FailReason(FailReason.FailType.UNKNOWN, new FileNotFoundException());
    private final FailReason FAIL_ANY_ERROR = new FailReason(FailReason.FailType.UNKNOWN, new RuntimeException());

    private Set<String> notFoundsUri;
    private ImageView view;
    private Bitmap bitmap;

    @Before
    public void setUp() throws Exception {
        notFoundsUri = Sets.newHashSet();
        view = new ImageView(context());
        bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
    }

    @Test
    public void listenerDoesNotChangeNoFoundUriSetWhenSuccess() {
        final FallbackImageListener listener = new FallbackImageListener(notFoundsUri);

        listener.onLoadingComplete(IMAGE_URI, view, bitmap);

        assertThat(notFoundsUri).isEmpty();
    }

    @Test
    public void listenerForwardOnCompleteToTheImageListener() {
        final ImageListener imageListener = mock(ImageListener.class);
        final FallbackImageListener listener = new FallbackImageListener(imageListener, notFoundsUri);

        listener.onLoadingComplete(IMAGE_URI, view, bitmap);

        verify(imageListener).onLoadingComplete(IMAGE_URI, view, bitmap);
    }

    @Test
    public void listenerForwardOnLoadingFailedToTheImageListener() {
        final ImageListener imageListener = mock(ImageListener.class);
        final FallbackImageListener listener = new FallbackImageListener(imageListener, notFoundsUri);

        listener.onLoadingFailed(IMAGE_URI, view, FAIL_FILE_NOT_FOUND);

        verify(imageListener).onLoadingFailed(eq(IMAGE_URI), eq(view), anyString());
    }

    @Test
    public void listenerForwardOnLoadingStartedToTheImageListener() {
        final ImageListener imageListener = mock(ImageListener.class);
        final FallbackImageListener listener = new FallbackImageListener(imageListener, notFoundsUri);

        listener.onLoadingStarted(IMAGE_URI, view);

        verify(imageListener).onLoadingStarted(IMAGE_URI, view);
    }

    @Test
    public void listenerShouldDisplayFallbackDrawableWhenImageNoFound() {
        final FallbackImageListener listener = new FallbackImageListener(notFoundsUri);
        final OneShotTransitionDrawable transitionDrawable = mock(OneShotTransitionDrawable.class);
        view.setImageDrawable(transitionDrawable);

        listener.onLoadingFailed(IMAGE_URI, view, FAIL_ANY_ERROR);

        verify(transitionDrawable).startTransition(anyInt());
    }

    @Test
    public void listenerShouldNotDisplayFallbackDrawableWhenImageSuccessfullyLoaded() {
        final FallbackImageListener listener = new FallbackImageListener(notFoundsUri);
        final OneShotTransitionDrawable transitionDrawable = mock(OneShotTransitionDrawable.class);
        view.setImageDrawable(transitionDrawable);

        listener.onLoadingComplete(IMAGE_URI, view, bitmap);

        verify(transitionDrawable, never()).startTransition(anyInt());
    }

    @Test
    public void listenerShouldAddUriToTheNotFoundListWhenFileNotFound() {
        final FallbackImageListener listener = new FallbackImageListener(notFoundsUri);

        listener.onLoadingFailed(IMAGE_URI, view, FAIL_FILE_NOT_FOUND);

        assertThat(notFoundsUri).contains(IMAGE_URI);
    }

    @Test
    public void listenerShouldAddUriToTheNotFoundListWhenUnknownError() {
        final FallbackImageListener listener = new FallbackImageListener(notFoundsUri);

        listener.onLoadingFailed(IMAGE_URI, view, FAIL_ANY_ERROR);

        assertThat(notFoundsUri).isEmpty();
    }
}