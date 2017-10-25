package com.soundcloud.android.image;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.cache.Cache;
import dagger.Module;
import dagger.Provides;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.TransitionDrawable;

import javax.inject.Named;

@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod") // abstract to force @Provides methods to be static
@Module
public abstract class ImageModule {

    static final String PLACEHOLDER_CACHE = "PlaceholderCache";
    static final String BLURRED_IMAGE_CACHE = "BlurredImageCache";

    @Provides
    static ImageLoader provideImageLoader(ImageCache imageCache,
                                          PlaceholderGenerator placeholderGenerator,
                                          CircularPlaceholderGenerator circularPlaceholderGenerator,
                                          DeviceHelper deviceHelper,
                                          UniversalImageOptionsFactory imageOptionsFactory,
                                          Context context,
                                          ApplicationProperties properties,
                                          UniversalImageDownloader.Factory imageDownloaderFactory) {
        return new UniversalImageLoader(com.nostra13.universalimageloader.core.ImageLoader.getInstance(),
                                        imageCache,
                                        placeholderGenerator,
                                        circularPlaceholderGenerator,
                                        imageOptionsFactory,
                                        deviceHelper,
                                        context,
                                        properties,
                                        imageDownloaderFactory);
    }

    @Provides
    @Named(PLACEHOLDER_CACHE)
    static Cache<String, TransitionDrawable> providePlaceholderCache() {
        return Cache.withSoftValues(50);
    }

    @Provides
    @Named(BLURRED_IMAGE_CACHE)
    static Cache<Urn, Bitmap> provideBlurredImageCache() {
        return Cache.withSoftValues(10);
    }

}
