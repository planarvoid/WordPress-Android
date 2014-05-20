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

import android.graphics.Bitmap;
import android.view.View;

import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class ViewlessLoadingAdapterTest {

    private ViewlessLoadingAdapter adapter;

    TestSubscriber<Bitmap> subscriber = new TestSubscriber<Bitmap>();

    @Mock
    View view;
    @Mock
    Bitmap bitmap;

    @Test
    public void onLoadingFailedEmitsError() throws Exception {
        adapter = new ViewlessLoadingAdapter(subscriber, false);
        adapter.onLoadingFailed("uri", view, "failure reason");
        final List<Throwable> onErrorEvents = subscriber.getOnErrorEvents();
        expect(onErrorEvents.size()).toBe(1);
    }

    @Test
    public void onLoadingCompleteEmitsBitmap() throws Exception {
        adapter = new ViewlessLoadingAdapter(subscriber, false);
        adapter.onLoadingComplete("uri", view, bitmap);
        subscriber.assertReceivedOnNext(Lists.newArrayList(bitmap));

    }

    @Test
    public void onLoadingCompleteEmitsImmutableARGB_8888BitmapCopy() throws Exception {
        final Bitmap copiedBitmap = Mockito.mock(Bitmap.class);
        when(bitmap.copy(Bitmap.Config.ARGB_8888, false)).thenReturn(copiedBitmap);
        adapter = new ViewlessLoadingAdapter(subscriber, true);
        adapter.onLoadingComplete("uri", view, bitmap);
        subscriber.assertReceivedOnNext(Lists.newArrayList(copiedBitmap));
    }

    @Test
    public void onLoadingCompleteDoesNotEmitBitmapIfUnsubscribed() throws Exception {
        final Bitmap copiedBitmap = Mockito.mock(Bitmap.class);
        when(bitmap.copy(Bitmap.Config.ARGB_8888, false)).thenReturn(copiedBitmap);
        adapter = new ViewlessLoadingAdapter(subscriber, true);
        subscriber.unsubscribe();
        adapter.onLoadingComplete("uri", view, bitmap);
        subscriber.assertReceivedOnNext(Collections.<Bitmap>emptyList());
    }

    @Test
    public void onLoadingCompleteDoesNotCopyBitmapIfUnsubscribed() throws Exception {
        adapter = new ViewlessLoadingAdapter(subscriber, true);
        subscriber.unsubscribe();
        adapter.onLoadingComplete("uri", view, bitmap);
        verify(bitmap, never()).copy(any(Bitmap.Config.class), anyBoolean());
    }
}