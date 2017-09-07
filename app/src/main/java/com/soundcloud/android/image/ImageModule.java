package com.soundcloud.android.image;

import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.cache.Cache;
import dagger.Module;
import dagger.Provides;

import android.graphics.Bitmap;
import android.graphics.drawable.TransitionDrawable;

import javax.inject.Named;

@Module
public class ImageModule {

    static final String PLACEHOLDER_CACHE = "PlaceholderCache";
    static final String BLURRED_IMAGE_CACHE = "BlurredImageCache";

    @Provides
    public ImageLoader provideImageLoader() {
        return ImageLoader.getInstance();
    }

    @Provides
    @Named(PLACEHOLDER_CACHE)
    public Cache<String, TransitionDrawable> providePlaceholderCache() {
        return Cache.withSoftValues(50);
    }

    @Provides
    @Named(BLURRED_IMAGE_CACHE)
    public Cache<Urn, Bitmap> provideBlurredImageCache() {
        return Cache.withSoftValues(10);
    }

    @Provides
    public FileNameGenerator provideHashCodeFileNameGenerator() {
        return new HashCodeFileNameGenerator();
    }

}
