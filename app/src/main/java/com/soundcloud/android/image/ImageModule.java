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

@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod") // abstract to force @Provides methods to be static
@Module
public abstract class ImageModule {

    static final String PLACEHOLDER_CACHE = "PlaceholderCache";
    static final String BLURRED_IMAGE_CACHE = "BlurredImageCache";

    @Provides
    static ImageLoader provideImageLoader() {
        return ImageLoader.getInstance();
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

    @Provides
    static FileNameGenerator provideHashCodeFileNameGenerator() {
        return new HashCodeFileNameGenerator();
    }

}
