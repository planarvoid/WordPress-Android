package com.soundcloud.android.image;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.LoadedFrom;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.widget.ImageView;

@RunWith(SoundCloudTestRunner.class)
public class ImageOptionsFactoryTest {

    ImageViewAware imageAware;

    @Mock
    Bitmap bitmap;
    @Mock
    ImageView imageView;


    @Before
    public void setUp() throws Exception {
        imageAware = new ImageViewAware(imageView);
    }

    @Test
    public void shouldCreatePrefetchOptions() throws Exception {
        DisplayImageOptions displayImageOptions = ImageOptionsFactory.prefetch();
        expect(displayImageOptions.isCacheInMemory()).toBeFalse();
        expect(displayImageOptions.isCacheOnDisk()).toBeTrue();
    }

    @Test
    public void shouldCreateCacheOptions() throws Exception {
        DisplayImageOptions displayImageOptions = ImageOptionsFactory.cache();
        expect(displayImageOptions.isCacheInMemory()).toBeTrue();
        expect(displayImageOptions.isCacheOnDisk()).toBeTrue();
    }

    @Test
    public void shouldCreateAdapterViewOptions() throws Exception {
        Resources resources = mock(Resources.class);
        Drawable drawable = mock(Drawable.class);
        DisplayImageOptions displayImageOptions = ImageOptionsFactory.adapterView(drawable, null);
        expect(displayImageOptions.isCacheInMemory()).toBeTrue();
        expect(displayImageOptions.isCacheOnDisk()).toBeTrue();
        expect(displayImageOptions.getImageForEmptyUri(resources)).toBe(drawable);
        expect(displayImageOptions.getImageOnFail(resources)).toBe(drawable);
        expect(displayImageOptions.getImageOnLoading(resources)).toBe(drawable);
        expect(displayImageOptions.getDisplayer()).toBeInstanceOf(ImageOptionsFactory.PlaceholderTransitionDisplayer.class);
    }

    @Test
    public void shouldCreateAdapterViewWithRGB565BitmapConfigForSmallImageSize() {
        Drawable drawable = mock(Drawable.class);
        DisplayImageOptions displayImageOptions = ImageOptionsFactory.adapterView(drawable, ApiImageSize.MINI);
        expect(displayImageOptions.getDecodingOptions().inPreferredConfig).toEqual(Bitmap.Config.RGB_565);
    }

    @Test
    public void shouldNotTransitionIfLoadedViaMemory() throws Exception {
        new ImageOptionsFactory.PlaceholderTransitionDisplayer().display(bitmap, imageAware, LoadedFrom.MEMORY_CACHE);
        verify(imageView).setImageBitmap(bitmap);
    }

    @Test
    public void shouldTransitionIfLoadedFromDisc() throws Exception {
        new ImageOptionsFactory.PlaceholderTransitionDisplayer().display(bitmap, imageAware, LoadedFrom.DISC_CACHE);
        verify(imageView).setImageDrawable(any(TransitionDrawable.class));
    }

    @Test
    public void shouldTransitionIfLoadedFromNetwork() throws Exception {
        new ImageOptionsFactory.PlaceholderTransitionDisplayer().display(bitmap, imageAware, LoadedFrom.NETWORK);
        verify(imageView).setImageDrawable(any(TransitionDrawable.class));
    }

    @Test
    public void shouldUDrawableWithPlaceholderTransition() throws Exception {
        new ImageOptionsFactory.PlaceholderTransitionDisplayer().display(bitmap, imageAware, LoadedFrom.NETWORK);
        verify(imageView).getDrawable();
        verify(imageView, never()).getBackground();
    }
}
