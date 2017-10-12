package com.soundcloud.android.image;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.reactivex.SingleEmitter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.graphics.Bitmap;
import android.view.View;

import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class FallbackBitmapLoadingAdapterTest {

    private FallbackBitmapLoadingAdapter adapter;

    @Mock SingleEmitter<Bitmap> singleEmitter;

    private @Mock View view;
    private @Mock Bitmap bitmap;
    private @Mock Bitmap fallbackImage;

    @Test
    public void onLoadingFailedFallBackToDrawable() throws Exception {
        adapter = new FallbackBitmapLoadingAdapter(singleEmitter, fallbackImage);
        adapter.onLoadingFailed("uri", view, new IOException());
        verify(singleEmitter, never()).onError(any());
        verify(singleEmitter).onSuccess(fallbackImage);
    }

    @Test
    public void onLoadingCompleteEmitsBitmap() throws Exception {
        adapter = new FallbackBitmapLoadingAdapter(singleEmitter, fallbackImage);
        adapter.onLoadingComplete("uri", view, bitmap);
        verify(singleEmitter).onSuccess(bitmap);
    }

    @Test
    public void onLoadingCompleteRecycleFallbackBitmap() throws Exception {
        adapter = new FallbackBitmapLoadingAdapter(singleEmitter, fallbackImage);

        adapter.onLoadingComplete("uri", view, bitmap);

        verify(fallbackImage).recycle();
    }

    @Test
    public void onLoadingCompleteDoesNotEmitBitmapIfUnsubscribed() throws Exception {
        when(singleEmitter.isDisposed()).thenReturn(true);
        adapter = new FallbackBitmapLoadingAdapter(singleEmitter, fallbackImage);
        adapter.onLoadingComplete("uri", view, bitmap);
        verify(singleEmitter, never()).onSuccess(any());
    }

    @Test
    public void onLoadingCompleteDoesNotCopyBitmapIfUnsubscribed() throws Exception {
        when(singleEmitter.isDisposed()).thenReturn(true);
        adapter = new FallbackBitmapLoadingAdapter(singleEmitter, fallbackImage);
        adapter.onLoadingComplete("uri", view, bitmap);
        verify(bitmap, never()).copy(any(Bitmap.Config.class), anyBoolean());
    }
}
