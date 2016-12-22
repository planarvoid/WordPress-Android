package com.soundcloud.android.image;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.observers.TestSubscriber;

import android.graphics.Bitmap;
import android.view.View;

import java.util.Collections;

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
        adapter = new BitmapLoadingAdapter(subscriber);
        subscriber.unsubscribe();
        adapter.onLoadingComplete("uri", view, bitmap);
        subscriber.assertReceivedOnNext(Collections.emptyList());
    }
}
