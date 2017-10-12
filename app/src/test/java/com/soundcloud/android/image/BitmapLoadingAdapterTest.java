package com.soundcloud.android.image;

import static org.mockito.Mockito.verify;

import io.reactivex.SingleEmitter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.graphics.Bitmap;
import android.view.View;

@RunWith(MockitoJUnitRunner.class)
public class BitmapLoadingAdapterTest {

    private BitmapLoadingAdapter adapter;

    @Mock SingleEmitter<Bitmap> singleEmitter;

    private @Mock View view;
    private @Mock Bitmap bitmap;

    @Test
    public void onLoadingCompleteEmitsBitmap() throws Exception {
        adapter = new BitmapLoadingAdapter(singleEmitter);
        adapter.onLoadingComplete("uri", view, bitmap);
        verify(singleEmitter).onSuccess(bitmap);
    }
}
