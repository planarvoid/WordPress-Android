package com.soundcloud.android.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.assist.LoadedFrom;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.DeviceHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.widget.ImageView;

public class ImageOptionsFactoryTest extends AndroidUnitTest {

    ImageViewAware imageAware;

    @Mock Bitmap bitmap;
    @Mock ImageView imageView;
    @Mock DeviceHelper deviceHelper;


    @Before
    public void setUp() throws Exception {
        imageAware = new ImageViewAware(imageView);
    }

    @Test
    public void shouldCreatePrefetchOptions() throws Exception {
        DisplayImageOptions displayImageOptions = ImageOptionsFactory.prefetch();
        assertThat(displayImageOptions.isCacheInMemory()).isFalse();
        assertThat(displayImageOptions.isCacheOnDisk()).isTrue();
    }

    @Test
    public void shouldCreateCacheOptions() throws Exception {
        DisplayImageOptions displayImageOptions = ImageOptionsFactory.cache();
        assertThat(displayImageOptions.isCacheInMemory()).isTrue();
        assertThat(displayImageOptions.isCacheOnDisk()).isTrue();
    }

    @Test
    public void shouldCreateAdapterViewOptions() throws Exception {
        Resources resources = mock(Resources.class);
        Drawable drawable = mock(Drawable.class);
        DisplayImageOptions displayImageOptions = ImageOptionsFactory.adapterView(drawable, null, deviceHelper);
        assertThat(displayImageOptions.isCacheInMemory()).isTrue();
        assertThat(displayImageOptions.isCacheOnDisk()).isTrue();
        assertThat(displayImageOptions.getImageForEmptyUri(resources)).isSameAs(drawable);
        assertThat(displayImageOptions.getImageOnFail(resources)).isSameAs(drawable);
        assertThat(displayImageOptions.getImageOnLoading(resources)).isSameAs(drawable);
        assertThat(displayImageOptions.getDisplayer()).isInstanceOf(ImageOptionsFactory.PlaceholderTransitionDisplayer.class);
    }

    @Test
    public void shouldCreateAdapterViewWithRGB565BitmapConfigForSmallImageSize() {
        Drawable drawable = mock(Drawable.class);
        DisplayImageOptions displayImageOptions = ImageOptionsFactory.adapterView(drawable, ApiImageSize.T47, deviceHelper);
        assertThat(displayImageOptions.getDecodingOptions().inPreferredConfig).isEqualTo(Bitmap.Config.RGB_565);
    }

    @Test
    public void shouldCreateAdapterViewWithRGB565BitmapConfigForLowMemoryDevices() {
        Drawable drawable = mock(Drawable.class);
        when(deviceHelper.isLowMemoryDevice()).thenReturn(true);
        DisplayImageOptions displayImageOptions = ImageOptionsFactory.adapterView(drawable, ApiImageSize.T500, deviceHelper);
        assertThat(displayImageOptions.getDecodingOptions().inPreferredConfig).isEqualTo(Bitmap.Config.RGB_565);
    }

    @Test
    public void shouldCreateAdapterViewWithRGB565BitmapConfigForTablets() {
        Drawable drawable = mock(Drawable.class);
        when(deviceHelper.isTablet()).thenReturn(true);
        DisplayImageOptions displayImageOptions = ImageOptionsFactory.adapterView(drawable, ApiImageSize.T500, deviceHelper);
        assertThat(displayImageOptions.getDecodingOptions().inPreferredConfig).isEqualTo(Bitmap.Config.RGB_565);
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
