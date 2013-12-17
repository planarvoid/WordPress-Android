package com.soundcloud.android.utils.images;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.LoadedFrom;
import com.soundcloud.android.image.ImageOptionsFactory;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.graphics.Bitmap;
import android.graphics.drawable.TransitionDrawable;
import android.widget.ImageView;

@RunWith(SoundCloudTestRunner.class)
public class ImageOptionsFactoryTest {
    @Mock
    Bitmap bitmap;
    @Mock
    ImageView imageView;

    @Test
    public void shouldCreatePrefetchOptions() throws Exception {
        DisplayImageOptions displayImageOptions = ImageOptionsFactory.prefetch();
        expect(displayImageOptions.isCacheInMemory()).toBeFalse();
        expect(displayImageOptions.isCacheOnDisc()).toBeTrue();
    }

    @Test
    public void shouldCreateCacheOptions() throws Exception {
        DisplayImageOptions displayImageOptions = ImageOptionsFactory.cache();
        expect(displayImageOptions.isCacheInMemory()).toBeTrue();
        expect(displayImageOptions.isCacheOnDisc()).toBeTrue();
    }

    @Test
    public void shouldCreatePlaceholderOptions() throws Exception {
        DisplayImageOptions displayImageOptions = ImageOptionsFactory.placeholder(123);
        expect(displayImageOptions.isCacheInMemory()).toBeTrue();
        expect(displayImageOptions.isCacheOnDisc()).toBeTrue();
        expect(displayImageOptions.getImageForEmptyUri()).toBe(123);
        expect(displayImageOptions.getImageOnFail()).toBe(123);
        expect(displayImageOptions.getStubImage()).toBe(123);
    }

    @Test
    public void shouldCreateAdapterViewOptions() throws Exception {
        DisplayImageOptions displayImageOptions = ImageOptionsFactory.adapterView(123);
        expect(displayImageOptions.isCacheInMemory()).toBeTrue();
        expect(displayImageOptions.isCacheOnDisc()).toBeTrue();
        expect(displayImageOptions.getImageForEmptyUri()).toBe(123);
        expect(displayImageOptions.getImageOnFail()).toBe(123);
        expect(displayImageOptions.getStubImage()).toBe(123);
        expect(displayImageOptions.getDisplayer()).toBeInstanceOf(ImageOptionsFactory.PlaceholderTransitionDisplayer.class);
    }

    @Test
    public void shouldCreateGridViewOptions() throws Exception {
        DisplayImageOptions displayImageOptions = ImageOptionsFactory.gridView();
        expect(displayImageOptions.isCacheInMemory()).toBeTrue();
        expect(displayImageOptions.isCacheOnDisc()).toBeTrue();
        expect(displayImageOptions.getDisplayer()).toBeInstanceOf(ImageOptionsFactory.BackgroundTransitionDisplayer.class);
    }

    @Test
    public void shouldNotTransitionIfLoadedViaMemory() throws Exception {
        new ImageOptionsFactory.BackgroundTransitionDisplayer().display(bitmap, imageView, LoadedFrom.MEMORY_CACHE);
        verify(imageView).setImageBitmap(bitmap);
    }

    @Test
    public void shouldTransitionIfLoadedFromDisc() throws Exception {
        new ImageOptionsFactory.BackgroundTransitionDisplayer().display(bitmap, imageView, LoadedFrom.DISC_CACHE);
        verify(imageView).setImageDrawable(any(TransitionDrawable.class));
    }

    @Test
    public void shouldTransitionIfLoadedFromNetwork() throws Exception {
        new ImageOptionsFactory.BackgroundTransitionDisplayer().display(bitmap, imageView, LoadedFrom.NETWORK);
        verify(imageView).setImageDrawable(any(TransitionDrawable.class));
    }

    @Test
    public void shouldUseBackgroundDrawableWithBackgroundTransition() throws Exception {
        new ImageOptionsFactory.BackgroundTransitionDisplayer().display(bitmap, imageView, LoadedFrom.NETWORK);
        verify(imageView).getBackground();
        verify(imageView, never()).getDrawable();
    }

    @Test
    public void shouldUDrawableWithPlaceholderTransition() throws Exception {
        new ImageOptionsFactory.PlaceholderTransitionDisplayer().display(bitmap, imageView, LoadedFrom.NETWORK);
        verify(imageView).getDrawable();
        verify(imageView, never()).getBackground();
    }
}
