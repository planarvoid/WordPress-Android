package com.soundcloud.android.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.cache.Cache;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.graphics.Bitmap;
import android.graphics.drawable.TransitionDrawable;

public class ImageCacheTest extends AndroidUnitTest {

    private static final String RESOLVER_URL = "https://api-mobile.soundcloud.com/images/soundcloud:tracks:1/large";

    private ImageCache imageCache;

    private Cache<String, TransitionDrawable> placeholderCache = Cache.withSoftValues(10);
    private Cache<Urn, Bitmap> blurredImageCache = Cache.withSoftValues(10);

    @Mock TransitionDrawable cachedTransitionDrawable;
    @Mock TransitionDrawable generatedTransitionDrawable;

    @Mock FileNameGenerator fileNameGenerator;
    @Mock UserAgentImageDownloaderFactory imageDownloaderFactory;
    @Mock DeviceHelper deviceHelper;
    @Mock ApplicationProperties properties;

    @Before
    public void setUp() throws Exception {
        imageCache = new ImageCache(placeholderCache, blurredImageCache, context(), properties, fileNameGenerator, imageDownloaderFactory, deviceHelper);
    }

    @Test
    public void getPlaceholderDrawableReturnsDrawable() throws Exception {
        placeholderCache.put(RESOLVER_URL + "_100_100", cachedTransitionDrawable);

        TransitionDrawable placeholderDrawable = imageCache.getPlaceholderDrawable(RESOLVER_URL, 100, 100, mock(PlaceholderGenerator.class));

        assertThat(placeholderDrawable).isSameAs(cachedTransitionDrawable);
    }

}