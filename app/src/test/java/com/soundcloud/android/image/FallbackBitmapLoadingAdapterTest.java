package com.soundcloud.android.image;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.observers.TestSubscriber;

import android.graphics.Bitmap;
import android.view.View;

import java.io.IOException;
import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class FallbackBitmapLoadingAdapterTest {

    private FallbackBitmapLoadingAdapter adapter;

    TestSubscriber<Bitmap> subscriber = new TestSubscriber<>();

    private @Mock View view;
    private @Mock Bitmap bitmap;
    private @Mock Bitmap fallbackImage;

    @Test
    public void onLoadingFailedFallBackToDrawable() throws Exception {
        adapter = new FallbackBitmapLoadingAdapter(subscriber, fallbackImage);
        adapter.onLoadingFailed("uri", view, new IOException());

        assertThat(subscriber.getOnErrorEvents()).isEmpty();
        assertThat(subscriber.getOnNextEvents()).containsExactly(fallbackImage);
    }

    @Test
    public void onLoadingCompleteEmitsBitmap() throws Exception {
        adapter = new FallbackBitmapLoadingAdapter(subscriber, fallbackImage);
        adapter.onLoadingComplete("uri", view, bitmap);
        subscriber.assertValue(bitmap);
    }

    @Test
    public void onLoadingCompleteRecycleFallbackBitmap() throws Exception {
        adapter = new FallbackBitmapLoadingAdapter(subscriber, fallbackImage);

        adapter.onLoadingComplete("uri", view, bitmap);

        verify(fallbackImage).recycle();
    }

    @Test
    public void onLoadingCompleteDoesNotEmitBitmapIfUnsubscribed() throws Exception {
        adapter = new FallbackBitmapLoadingAdapter(subscriber, fallbackImage);
        subscriber.unsubscribe();
        adapter.onLoadingComplete("uri", view, bitmap);
        subscriber.assertReceivedOnNext(Collections.emptyList());
    }

    @Test
    public void onLoadingCompleteDoesNotCopyBitmapIfUnsubscribed() throws Exception {
        adapter = new FallbackBitmapLoadingAdapter(subscriber, fallbackImage);
        subscriber.unsubscribe();
        adapter.onLoadingComplete("uri", view, bitmap);
        verify(bitmap, never()).copy(any(Bitmap.Config.class), anyBoolean());
    }
}
