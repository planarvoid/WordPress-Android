package com.soundcloud.android.image;

import static com.soundcloud.android.image.ImageModule.BLURRED_IMAGE_CACHE;
import static com.soundcloud.android.image.ImageModule.PLACEHOLDER_CACHE;

import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.cache.Cache;
import com.soundcloud.java.optional.Optional;
import rx.functions.Action1;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.TransitionDrawable;
import android.support.annotation.Nullable;

import javax.inject.Inject;
import javax.inject.Named;

class ImageCache {

    private static final String DEFAULT_CACHE_KEY = "default_cache_key";
    private static final String PLACEHOLDER_KEY_BASE = "%s_%s_%s";

    private final Cache<String, TransitionDrawable> placeholderCache;
    private final Cache<Urn, Bitmap> blurredImageCache;

    private final Context context;
    private final ApplicationProperties properties;
    private final FileNameGenerator fileNameGenerator;
    private final UserAgentImageDownloaderFactory imageDownloaderFactory;
    private final DeviceHelper deviceHelper;

    @Inject
    ImageCache(@Named(PLACEHOLDER_CACHE) Cache<String, TransitionDrawable> placeholderCache,
               @Named(BLURRED_IMAGE_CACHE) Cache<Urn, Bitmap> blurredImageCache,
               Context context,
               ApplicationProperties properties,
               FileNameGenerator fileNameGenerator,
               UserAgentImageDownloaderFactory imageDownloaderFactory,
               DeviceHelper deviceHelper) {
        this.placeholderCache = placeholderCache;
        this.blurredImageCache = blurredImageCache;
        this.context = context;
        this.properties = properties;
        this.fileNameGenerator = fileNameGenerator;
        this.imageDownloaderFactory = imageDownloaderFactory;
        this.deviceHelper = deviceHelper;
    }

    ImageLoaderConfiguration getImageLoaderConfiguration() {
        final Context appContext = context.getApplicationContext();

        final ImageLoaderConfiguration.Builder builder = new ImageLoaderConfiguration.Builder(appContext);
        if (properties.useVerboseLogging()) {
            builder.writeDebugLogs();
        }

        builder.defaultDisplayImageOptions(ImageOptionsFactory.cache());
        builder.diskCacheFileNameGenerator(fileNameGenerator);
        builder.imageDownloader(imageDownloaderFactory.create(context));
        builder.memoryCache(new WeakMemoryCache());
        if (deviceHelper.isLowMemoryDevice()) {
            // Cut down to half of what UIL would reserve by default (div 8) on low mem devices
            builder.memoryCacheSize((int) (Runtime.getRuntime().maxMemory() / 16));
        }
        return builder.build();
    }

    private String cacheKeyForImageUrl(@Nullable String imageUrl) {
        return Optional.fromNullable(imageUrl).or(DEFAULT_CACHE_KEY);
    }

    @Nullable
    TransitionDrawable getPlaceholderDrawable(String imageUrl, int width, int height, PlaceholderGenerator placeholderGenerator) {
        final String widthHeightSpecificKey = String.format(PLACEHOLDER_KEY_BASE, cacheKeyForImageUrl(imageUrl), String.valueOf(width), String.valueOf(height));
        return placeholderCache.get(widthHeightSpecificKey, placeholderGenerator::generateTransitionDrawable);
    }

    @Nullable
    Bitmap getBlurredImage(Urn urn) {
        return blurredImageCache.get(urn);
    }

    Action1<Bitmap> cacheBlurredBitmap(final Urn resourceUrn) {
        return bitmap -> blurredImageCache.put(resourceUrn, bitmap);
    }

}
