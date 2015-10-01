package com.soundcloud.android.image;

import android.graphics.Bitmap;
import android.view.View;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.verification.VerificationMode;

import java.util.Collections;

import rx.observers.TestSubscriber;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BitmapLoadingAdapterTest {

    private BitmapLoadingAdapter adapter;

    TestSubscriber<Bitmap> subscriber = new TestSubscriber<>();

    private @Mock View view;
    private @Mock Bitmap bitmap;

    @Test
    public void onLoadingCompleteEmitsBitmap() throws Exception {
        adapter = new BitmapLoadingAdapter(subscriber);
        adapter.onLoadingComplete("uri", view, bitmap);
        subscriber.assertValue(bitmap);
    }

    @Test
    public void onLoadingCompleteDoesNotEmitBitmapIfUnsubscribed() throws Exception {
        final Bitmap copiedBitmap = Mockito.mock(Bitmap.class);
        when(bitmap.copy(Bitmap.Config.ARGB_8888, false)).thenReturn(copiedBitmap);
        adapter = new BitmapLoadingAdapter(subscriber);
        subscriber.unsubscribe();
        adapter.onLoadingComplete("uri", view, bitmap);
        subscriber.assertReceivedOnNext(Collections.<Bitmap>emptyList());
    }

    @Test
    public void onLoadingCompleteDoesNotEmitNullBitmap() throws Exception {
        adapter = new BitmapLoadingAdapter(subscriber);
        adapter.onLoadingComplete("uri", view, null);
        subscriber.assertReceivedOnNext(Collections.<Bitmap>emptyList());
    }

    @Test
    public void onLoadingCompleteDoesNotCopyBitmapIfUnsubscribed() throws Exception {
        adapter = new BitmapLoadingAdapter(subscriber);
        subscriber.unsubscribe();
        adapter.onLoadingComplete("uri", view, bitmap);
        verify(bitmap, never()).copy(any(Bitmap.Config.class), anyBoolean());
    }

}
