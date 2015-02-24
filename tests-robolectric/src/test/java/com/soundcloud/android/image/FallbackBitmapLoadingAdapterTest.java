package com.soundcloud.android.image;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.observers.TestSubscriber;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.view.View;

import java.util.Collections;

@RunWith(SoundCloudTestRunner.class)
public class FallbackBitmapLoadingAdapterTest {

    private FallbackBitmapLoadingAdapter adapter;

    TestSubscriber<Bitmap> subscriber = new TestSubscriber<Bitmap>();

    private @Mock View view;
    private @Mock Bitmap bitmap;
    private @Mock Bitmap fallbackImage;

    @Test
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public void onLoadingFailedFallBackToDrawable() throws Exception {
        adapter = new FallbackBitmapLoadingAdapter(subscriber, fallbackImage);
        adapter.onLoadingFailed("uri", view, "failure reason");

        expect(subscriber.getOnErrorEvents()).toBeEmpty();
        expect(subscriber.getOnNextEvents()).toEqual(Lists.newArrayList(fallbackImage));
    }

    @Test
    public void onLoadingCompleteEmitsBitmap() throws Exception {
        adapter = new FallbackBitmapLoadingAdapter(subscriber, fallbackImage);
        adapter.onLoadingComplete("uri", view, bitmap);
        subscriber.assertReceivedOnNext(Lists.newArrayList(bitmap));
    }

    @Test
    public void onLoadingCompleteRecycleFallbackBitmap() throws Exception {
        adapter = new FallbackBitmapLoadingAdapter(subscriber, fallbackImage);

        adapter.onLoadingComplete("uri", view, bitmap);

        verify(fallbackImage).recycle();
    }

    @Test
    public void onLoadingCompleteDoesNotEmitBitmapIfUnsubscribed() throws Exception {
        final Bitmap copiedBitmap = Mockito.mock(Bitmap.class);
        when(bitmap.copy(Bitmap.Config.ARGB_8888, false)).thenReturn(copiedBitmap);
        adapter = new FallbackBitmapLoadingAdapter(subscriber, fallbackImage);
        subscriber.unsubscribe();
        adapter.onLoadingComplete("uri", view, bitmap);
        subscriber.assertReceivedOnNext(Collections.<Bitmap>emptyList());
    }

    @Test
    public void onLoadingCompleteDoesNotCopyBitmapIfUnsubscribed() throws Exception {
        adapter = new FallbackBitmapLoadingAdapter(subscriber, fallbackImage);
        subscriber.unsubscribe();
        adapter.onLoadingComplete("uri", view, bitmap);
        verify(bitmap, never()).copy(any(Bitmap.Config.class), anyBoolean());
    }
}