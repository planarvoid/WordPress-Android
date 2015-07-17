package com.soundcloud.android.image;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.assist.ViewScaleType;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.nostra13.universalimageloader.core.imageaware.NonViewAware;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiUrlBuilder;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.utils.cache.Cache;
import com.soundcloud.android.utils.cache.Cache.ValueProvider;
import com.soundcloud.android.utils.images.ImageUtils;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.FileNotFoundException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class ImageOperations {

    private static final int LOW_MEM_DEVICE_THRESHOLD = 50 * 1024 * 1024; // available mem in bytes
    private static final Pattern PATTERN = Pattern.compile("^https?://(.+)");
    private static final String URL_BASE = "http://%s";
    private static final String PLACEHOLDER_KEY_BASE = "%s_%s_%s";

    private final ImageLoader imageLoader;
    private final ApiUrlBuilder urlBuilder;
    private final PlaceholderGenerator placeholderGenerator;

    private final Set<String> notFoundUris = Sets.newHashSet();
    private final FallbackBitmapLoadingAdapter.Factory adapterFactory;
    private final FileNameGenerator fileNameGenerator;

    private final Cache<String, TransitionDrawable> placeholderCache;
    private final Cache<Urn, Bitmap> blurredImageCache;

    private final Func1<Bitmap, Bitmap> blurBitmap = new Func1<Bitmap, Bitmap>() {
        @Override
        public Bitmap call(Bitmap bitmap) {
            return imageProcessor.blurBitmap(bitmap);
        }
    };
    private ImageProcessor imageProcessor;

    @Inject
    public ImageOperations(ApiUrlBuilder urlBuilder, PlaceholderGenerator placeholderGenerator,
                           FallbackBitmapLoadingAdapter.Factory adapterFactory, ImageProcessor imageProcessor) {
        this(ImageLoader.getInstance(), urlBuilder, placeholderGenerator, adapterFactory, imageProcessor,
                Cache.<String, TransitionDrawable>withSoftValues(50),
                Cache.<Urn, Bitmap>withSoftValues(10),
                new HashCodeFileNameGenerator());

    }

    private final FallbackImageListener notFoundListener = new FallbackImageListener(notFoundUris);

    @VisibleForTesting
    ImageOperations(ImageLoader imageLoader, ApiUrlBuilder urlBuilder, PlaceholderGenerator placeholderGenerator,
                    FallbackBitmapLoadingAdapter.Factory adapterFactory, ImageProcessor imageProcessor,
                    Cache<String, TransitionDrawable> placeholderCache,
                    Cache<Urn, Bitmap> blurredImageCache, FileNameGenerator fileNameGenerator) {
        this.imageLoader = imageLoader;
        this.urlBuilder = urlBuilder;
        this.placeholderGenerator = placeholderGenerator;
        this.placeholderCache = placeholderCache;
        this.blurredImageCache = blurredImageCache;
        this.adapterFactory = adapterFactory;
        this.imageProcessor = imageProcessor;
        this.fileNameGenerator = fileNameGenerator;
    }

    public void initialise(Context context, ApplicationProperties properties) {
        final Context appContext = context.getApplicationContext();

        final ImageLoaderConfiguration.Builder builder = new ImageLoaderConfiguration.Builder(appContext);
        if (properties.useVerboseLogging()) {
            builder.writeDebugLogs();
        }

        builder.defaultDisplayImageOptions(ImageOptionsFactory.cache());
        builder.diskCacheFileNameGenerator(fileNameGenerator);
        final long availableMemory = Runtime.getRuntime().maxMemory();
        // Here are some reference values for available mem: Wildfire: 16,777,216; Nexus S: 33,554,432; Nexus 4: 201,326,592
        if (availableMemory < LOW_MEM_DEVICE_THRESHOLD) {
            // cut down to half of what UIL would reserve by default (div 8) on low mem devices
            builder.memoryCacheSize((int) (availableMemory / 16));
        }
        imageLoader.init(builder.build());
    }

    public void clearDiskCache() {
        imageLoader.clearDiskCache();
    }

    public void displayInAdapterView(Urn urn, ApiImageSize apiImageSize, ImageView imageView) {
        final ImageViewAware imageAware = new ImageViewAware(imageView, false);
        imageLoader.displayImage(
                buildUrlIfNotPreviouslyMissing(urn, apiImageSize),
                imageAware,
                ImageOptionsFactory.adapterView(getPlaceholderDrawable(urn, imageAware), apiImageSize), notFoundListener);
    }

    public void displayWithPlaceholder(Urn urn, ApiImageSize apiImageSize, ImageView imageView) {
        final ImageViewAware imageAware = new ImageViewAware(imageView, false);
        imageLoader.displayImage(
                buildUrlIfNotPreviouslyMissing(urn, apiImageSize),
                imageAware,
                ImageOptionsFactory.placeholder(getPlaceholderDrawable(urn, imageAware)),
                notFoundListener);
    }

    public void displayInPlayer(Urn urn, ApiImageSize apiImageSize, ImageView imageView, Bitmap placeholder, boolean isHighPriority) {
        final ImageViewAware imageAware = new ImageViewAware(imageView, false);
        final Drawable placeholderDrawable = placeholder != null ? new BitmapDrawable(placeholder) :
                getPlaceholderDrawable(urn, imageAware);

        imageLoader.displayImage(
                buildUrlIfNotPreviouslyMissing(urn, apiImageSize),
                imageAware,
                ImageOptionsFactory.player(placeholderDrawable, isHighPriority),
                notFoundListener);
    }

    public void displayAdInPlayer(Uri uri, ImageView imageView, Drawable placeholderDrawable) {
        final ImageViewAware imageAware = new ImageViewAware(imageView, false);
        imageLoader.displayImage(
                uri.toString(),
                imageAware,
                ImageOptionsFactory.playerAd(placeholderDrawable));
    }

    public void displayLeaveBehind(Uri uri, ImageView imageView, ImageListener imageListener) {
        final ImageViewAware imageAware = new ImageViewAware(imageView, false);
        imageLoader.displayImage(
                uri.toString(),
                imageAware,
                ImageOptionsFactory.playerLeaveBehind(),
                new ImageListenerUILAdapter(imageListener));
    }

    public void displayInFullDialogView(Urn urn, ApiImageSize apiImageSize, ImageView imageView, ImageListener imageListener) {
        imageLoader.displayImage(
                buildUrlIfNotPreviouslyMissing(urn, apiImageSize),
                new ImageViewAware(imageView, false),
                ImageOptionsFactory.fullImageDialog(),
                new ImageListenerUILAdapter(imageListener));
    }

    private void load(Urn urn, ApiImageSize apiImageSize, ImageListener imageListener) {
        imageLoader.loadImage(
                buildUrlIfNotPreviouslyMissing(urn, apiImageSize),
                new ImageListenerUILAdapter(imageListener));
    }

    private void load(Urn urn, ApiImageSize apiImageSize, int targetWidth, int targetHeight, ImageListener imageListener) {
        ImageSize targetSize = new ImageSize(targetWidth, targetHeight);
        ImageAware imageAware = new NonViewAware(targetSize, ViewScaleType.CROP);
        imageLoader.displayImage(buildUrlIfNotPreviouslyMissing(urn, apiImageSize), imageAware, new ImageListenerUILAdapter(imageListener));
    }

    @Deprecated // use the variants that take URNs instead
    public void display(String imageUrl, ImageView imageView) {
        imageLoader.displayImage(adjustUrl(imageUrl), new ImageViewAware(imageView, false));
    }

    public Observable<Bitmap> artwork(final Urn resourceUrn, final ApiImageSize apiImageSize) {
        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber) {
                final Bitmap fallback = createFallbackBitmap(resourceUrn, apiImageSize);
                load(resourceUrn, apiImageSize, adapterFactory.create(subscriber, fallback));
            }
        });
    }

    public Observable<Bitmap> artwork(final Urn resourceUrn, final ApiImageSize apiImageSize, final int targetWidth,
                                      final int targetHeight) {
        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber) {
                final GradientDrawable fallbackDrawable = placeholderGenerator.generateDrawable(resourceUrn.toString());
                final Bitmap fallback = ImageUtils.toBitmap(fallbackDrawable, targetWidth, targetHeight);
                load(resourceUrn, apiImageSize, targetWidth, targetHeight, adapterFactory.create(subscriber, fallback));
            }
        });
    }

    public Observable<Bitmap> blurredPlayerArtwork(final Resources resources, final Urn resourceUrn,
                                                   Scheduler scheduleOn, Scheduler observeOn) {
        final Bitmap cachedBlurImage = blurredImageCache.get(resourceUrn);
        if (cachedBlurImage != null) {
            return Observable.just(cachedBlurImage);
        } else {
            final Bitmap cached = getCachedListItemBitmap(resources, resourceUrn);
            if (cached == null) {
                return artwork(resourceUrn, ApiImageSize.getListItemImageSize(resources))
                        .map(blurBitmap)
                        .subscribeOn(scheduleOn)
                        .observeOn(observeOn)
                        .doOnNext(cacheBlurredBitmap(resourceUrn));
            } else {
                return blurBitmap(cached)
                        .subscribeOn(scheduleOn)
                        .observeOn(observeOn)
                        .doOnNext(cacheBlurredBitmap(resourceUrn));
            }
        }

    }

    private Action1<Bitmap> cacheBlurredBitmap(final Urn resourceUrn) {
        return new Action1<Bitmap>() {
            @Override
            public void call(Bitmap bitmap) {
                blurredImageCache.put(resourceUrn, bitmap);
            }
        };
    }

    private Observable<Bitmap> blurBitmap(final Bitmap original) {
        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber) {
                subscriber.onNext(imageProcessor.blurBitmap(original));
                subscriber.onCompleted();
            }
        });
    }

    @Nullable
    public Bitmap getCachedListItemBitmap(Resources resources, Urn resourceUrn) {
        return getCachedBitmap(resourceUrn, ApiImageSize.getListItemImageSize(resources),
                resources.getDimensionPixelSize(R.dimen.list_item_image_dimension),
                resources.getDimensionPixelSize(R.dimen.list_item_image_dimension));
    }

    private Bitmap createFallbackBitmap(Urn resourceUrn, ApiImageSize apiImageSize) {
        // This bitmap is only used by the current track for the components that can't use
        // drawables (i.e. the notification and the remote client for the lock screen)
        //
        // Unless we refactor the cache to store a /GradientDrawable/ and not a /TransitionDrawable/
        // we don't have a cache for this guy.
        //
        // Also, we don't cache bitmap in the /ImageOperations/ since it does not worth it. A cache
        // may have a impact on the memory usage and without the performance seems pretty good, though.
        final GradientDrawable fallbackDrawable = placeholderGenerator.generateDrawable(resourceUrn.toString());
        return ImageUtils.toBitmap(fallbackDrawable, apiImageSize.width, apiImageSize.height);
    }

    @Nullable
    public Bitmap getCachedBitmap(Urn resourceUrn, ApiImageSize apiImageSize) {
        return getCachedBitmap(resourceUrn, apiImageSize, apiImageSize.width, apiImageSize.height);
    }

    @Nullable
    public Bitmap getCachedBitmap(Urn resourceUrn, ApiImageSize apiImageSize, int targetWidth, int targetHeight) {
        final String imageUrl = getImageUrl(resourceUrn, apiImageSize);
        if (notFoundUris.contains(imageUrl)) {
            return null;
        }

        final String key = MemoryCacheUtils.generateKey(imageUrl, new ImageSize(targetWidth, targetHeight));
        return imageLoader.getMemoryCache().get(key);
    }

    void resume() {
        imageLoader.resume();
    }

    void pause() {
        imageLoader.resume();
    }

    public AbsListView.OnScrollListener createScrollPauseListener(boolean pauseOnScroll, boolean pauseOnFling,
                                                                  AbsListView.OnScrollListener customListener) {
        return new PauseOnScrollListener(imageLoader, pauseOnScroll, pauseOnFling, customListener);
    }

    public AbsListView.OnScrollListener createScrollPauseListener(boolean pauseOnScroll, boolean pauseOnFling) {
        return new PauseOnScrollListener(imageLoader, pauseOnScroll, pauseOnFling);
    }

    public String getUrlForLargestImage(Resources resources, Urn urn) {
        return buildUrlIfNotPreviouslyMissing(urn, ApiImageSize.getFullImageSize(resources));
    }

    @Nullable
    public Bitmap decodeResource(Resources resources, int resId){
        try {
            return BitmapFactory.decodeResource(resources, resId);
        } catch (OutOfMemoryError e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Adjust urls to use insecure protocol. Will result in more cache hits
     */
    private String adjustUrl(String url) {
        if (ScTextUtils.isNotBlank(url)) {
            Matcher matcher = PATTERN.matcher(url);
            if (matcher.find() && matcher.groupCount() == 1) {
                return String.format(URL_BASE, matcher.group(1));
            }
        }
        return url; // fallback to original url
    }

    private Drawable getPlaceholderDrawable(final Urn urn, ImageViewAware imageViewAware) {
        return getPlaceholderDrawable(urn, imageViewAware.getWidth(), imageViewAware.getHeight());

    }

    /**
     * We have to store these so so we don't animate on every load attempt. this prevents flickering
     */
    @Nullable
    private TransitionDrawable getPlaceholderDrawable(final Urn urn, int width, int height) {
        final String key = String.format(PLACEHOLDER_KEY_BASE, urn, String.valueOf(width), String.valueOf(height));
        return placeholderCache.get(key, new ValueProvider<String, TransitionDrawable>() {
            @Override
            public TransitionDrawable get(String key) {
                return placeholderGenerator.generateTransitionDrawable(urn.toString());
            }
        });
    }

    @Nullable
    private String buildUrlIfNotPreviouslyMissing(Urn urn, ApiImageSize apiImageSize) {
        final String imageUrl = getImageUrl(urn, apiImageSize);
        return notFoundUris.contains(imageUrl) ? null : imageUrl;
    }

    private String getImageUrl(Urn urn, ApiImageSize apiImageSize) {
        return urlBuilder.from(ApiEndpoints.IMAGES, urn, apiImageSize.sizeSpec).build();
    }

    @VisibleForTesting
    static class FallbackImageListener implements ImageLoadingListener {
        private final ImageListenerUILAdapter listenerAdapter;
        private final Set<String> notFoundUris;

        public FallbackImageListener(Set<String> notFoundUris) {
            this(null, notFoundUris);
        }

        public FallbackImageListener(@Nullable ImageListener imageListener, Set<String> notFoundUris) {
            this.notFoundUris = notFoundUris;
            listenerAdapter = imageListener != null ? new ImageListenerUILAdapter(imageListener) : null;
        }

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            if (loadedImage == null) {
                animatePlaceholder(view);
            }
            if (listenerAdapter != null) {
                listenerAdapter.onLoadingComplete(imageUri, view, loadedImage);
            }
        }

        @Override
        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
            if (failReason.getCause() instanceof FileNotFoundException) {
                notFoundUris.add(imageUri);
            }
            animatePlaceholder(view);
            if (listenerAdapter != null) {
                listenerAdapter.onLoadingFailed(imageUri, view, failReason);
            }
        }

        @Override
        public void onLoadingStarted(String imageUri, View view) {
            if (listenerAdapter != null) {
                listenerAdapter.onLoadingStarted(imageUri, view);
            }
        }

        @Override
        public void onLoadingCancelled(String imageUri, View view) {
            if (listenerAdapter != null) {
                listenerAdapter.onLoadingCancelled(imageUri, view);
            }
        }

        private void animatePlaceholder(View view) {
            if (view instanceof ImageView && ((ImageView) view).getDrawable() instanceof OneShotTransitionDrawable) {
                final OneShotTransitionDrawable transitionDrawable = (OneShotTransitionDrawable) ((ImageView) view).getDrawable();
                transitionDrawable.startTransition(ImageUtils.DEFAULT_TRANSITION_DURATION);
            }
        }
    }
}
