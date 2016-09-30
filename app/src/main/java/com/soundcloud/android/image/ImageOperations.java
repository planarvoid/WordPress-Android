package com.soundcloud.android.image;

import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
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
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.cache.Cache;
import com.soundcloud.android.utils.cache.Cache.ValueProvider;
import com.soundcloud.android.utils.images.ImageUtils;
import com.soundcloud.java.optional.Optional;
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
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Set;

@Singleton
public class ImageOperations {

    private static final String TAG = ImageLoader.TAG;
    private static final String PLACEHOLDER_KEY_BASE = "%s_%s_%s";

    private final ImageLoader imageLoader;
    private final ImageUrlBuilder imageUrlBuilder;
    private final PlaceholderGenerator placeholderGenerator;

    private final Set<String> notFoundUris = new HashSet<>();
    private final FallbackBitmapLoadingAdapter.Factory adapterFactory;
    private final BitmapLoadingAdapter.Factory bitmapAdapterFactory;
    private final FileNameGenerator fileNameGenerator;
    private final UserAgentImageDownloaderFactory imageDownloaderFactory;
    private final DeviceHelper deviceHelper;

    private final CircularPlaceholderGenerator circularPlaceholderGenerator;
    private final Cache<String, TransitionDrawable> placeholderCache;
    private final Cache<Urn, Bitmap> blurredImageCache;

    private ImageProcessor imageProcessor;

    @Inject
    public ImageOperations(PlaceholderGenerator placeholderGenerator,
                           CircularPlaceholderGenerator circularPlaceholderGenerator,
                           FallbackBitmapLoadingAdapter.Factory adapterFactory,
                           BitmapLoadingAdapter.Factory bitmapAdapterFactory,
                           ImageProcessor imageProcessor,
                           ImageUrlBuilder imageUrlBuilder,
                           UserAgentImageDownloaderFactory imageDownloaderFactory,
                           DeviceHelper deviceHelper) {
        this(ImageLoader.getInstance(), imageUrlBuilder, placeholderGenerator, circularPlaceholderGenerator,
             adapterFactory, bitmapAdapterFactory, imageProcessor,
             Cache.<String, TransitionDrawable>withSoftValues(50),
             Cache.<Urn, Bitmap>withSoftValues(10),
             new HashCodeFileNameGenerator(),
             imageDownloaderFactory,
             deviceHelper);
    }

    private final FallbackImageListener notFoundListener = new FallbackImageListener(notFoundUris);

    @VisibleForTesting
    ImageOperations(ImageLoader imageLoader,
                    ImageUrlBuilder imageUrlBuilder,
                    PlaceholderGenerator placeholderGenerator,
                    CircularPlaceholderGenerator circularPlaceholderGenerator,
                    FallbackBitmapLoadingAdapter.Factory adapterFactory,
                    BitmapLoadingAdapter.Factory bitmapAdapterFactory,
                    ImageProcessor imageProcessor,
                    Cache<String, TransitionDrawable> placeholderCache,
                    Cache<Urn, Bitmap> blurredImageCache,
                    FileNameGenerator fileNameGenerator,
                    UserAgentImageDownloaderFactory imageDownloaderFactory,
                    DeviceHelper deviceHelper) {
        this.imageLoader = imageLoader;
        this.imageUrlBuilder = imageUrlBuilder;
        this.placeholderGenerator = placeholderGenerator;
        this.circularPlaceholderGenerator = circularPlaceholderGenerator;
        this.placeholderCache = placeholderCache;
        this.blurredImageCache = blurredImageCache;
        this.adapterFactory = adapterFactory;
        this.bitmapAdapterFactory = bitmapAdapterFactory;
        this.imageProcessor = imageProcessor;
        this.fileNameGenerator = fileNameGenerator;
        this.imageDownloaderFactory = imageDownloaderFactory;
        this.deviceHelper = deviceHelper;
    }

    public void initialise(Context context, ApplicationProperties properties) {
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
        imageLoader.init(builder.build());
    }

    public void clearDiskCache() {
        imageLoader.clearDiskCache();
    }

    /**
     * Load an image for a list item whereby a best attempt is made to load it directly via URL provided by the
     * item itself. This is the preferred way for loading artwork and avatars, since it's more efficient than
     * resolving the URL first.
     * <p/>
     * If no URL is provided, the indirect resolution via the image resolver is used.
     */
    public void displayInAdapterView(ImageResource imageResource, ApiImageSize apiImageSize, ImageView imageView) {
        displayInAdapterView(imageResource.getUrn(), apiImageSize, imageView,
                             buildUrlIfNotPreviouslyMissing(imageResource, apiImageSize));
    }

    /**
     * Load an image for a list item by using the image resolver on the given URN.
     * <p/>
     * This kind of resolution puts more pressure on our backends, so always prefer to load via {@link ImageResource}
     * if possible.
     */
    @Deprecated // use the ImageResource variant instead
    public void displayInAdapterView(Urn urn, ApiImageSize apiImageSize, ImageView imageView) {
        displayInAdapterView(urn, apiImageSize, imageView, buildUrlIfNotPreviouslyMissing(urn, apiImageSize));
    }

    private void displayInAdapterView(Urn urn, ApiImageSize apiImageSize, ImageView imageView, String imageUrl) {
        final ImageViewAware imageAware = new ImageViewAware(imageView, false);
        final DisplayImageOptions options = ImageOptionsFactory.adapterView(
                getPlaceholderDrawable(urn, imageAware), apiImageSize, deviceHelper);
        imageLoader.displayImage(
                imageUrl,
                imageAware,
                options, notFoundListener);
    }

    /**
     * @see {@link #displayInAdapterView(ImageResource, ApiImageSize, ImageView)}
     */
    public void displayCircularInAdapterView(ImageResource imageResource,
                                             ApiImageSize apiImageSize,
                                             ImageView imageView) {
        final String imageUrl = buildUrlIfNotPreviouslyMissing(imageResource, apiImageSize);
        displayCircularInAdapterView(imageResource.getUrn(), apiImageSize, imageView, imageUrl);
    }

    /**
     * @see {@link #displayInAdapterView(Urn, ApiImageSize, ImageView)}
     */
    @Deprecated // use the ImageResource variant instead
    public void displayCircularInAdapterView(Urn urn, ApiImageSize apiImageSize, ImageView imageView) {
        final String imageUrl = buildUrlIfNotPreviouslyMissing(urn, apiImageSize);
        displayCircularInAdapterView(urn, apiImageSize, imageView, imageUrl);
    }

    private void displayCircularInAdapterView(Urn urn,
                                              ApiImageSize apiImageSize,
                                              ImageView imageView,
                                              String imageUrl) {
        final ImageViewAware imageAware = new ImageViewAware(imageView, false);
        final TransitionDrawable placeholderDrawable = getCircularPlaceholderDrawable(urn,
                                                                                      imageAware.getWidth(),
                                                                                      imageAware.getHeight());
        imageLoader.displayImage(
                imageUrl,
                imageAware,
                ImageOptionsFactory.adapterViewCircular(placeholderDrawable, apiImageSize, deviceHelper),
                notFoundListener);
    }

    public void displayWithPlaceholder(ImageResource imageResource, ApiImageSize apiImageSize, ImageView imageView) {
        displayWithPlaceholder(imageResource.getUrn(), imageView,
                               buildUrlIfNotPreviouslyMissing(imageResource, apiImageSize));
    }

    @Deprecated // use the ImageResource variant instead
    public void displayWithPlaceholder(Urn urn, ApiImageSize apiImageSize, ImageView imageView) {
        displayWithPlaceholder(urn, imageView, buildUrlIfNotPreviouslyMissing(urn, apiImageSize));
    }

    private void displayWithPlaceholder(Urn urn, ImageView imageView, String imageUrl) {
        final ImageViewAware imageAware = new ImageViewAware(imageView, false);
        imageLoader.displayImage(
                imageUrl,
                imageAware,
                ImageOptionsFactory.placeholder(getPlaceholderDrawable(urn, imageAware)),
                notFoundListener);
    }

    public void displayCircularWithPlaceholder(ImageResource imageResource,
                                               ApiImageSize apiImageSize,
                                               ImageView imageView) {
        displayCircularWithPlaceholder(imageResource.getUrn(), imageView,
                                       buildUrlIfNotPreviouslyMissing(imageResource, apiImageSize));
    }

    private void displayCircularWithPlaceholder(Urn urn, ImageView imageView, String imageUrl) {
        final ImageViewAware imageAware = new ImageViewAware(imageView, false);
        final DisplayImageOptions options = ImageOptionsFactory.placeholderCircular(
                getCircularPlaceholderDrawable(urn, imageAware.getWidth(), imageAware.getHeight()));
        imageLoader.displayImage(
                imageUrl,
                imageAware,
                options,
                notFoundListener);
    }

    public void displayInPlayer(ImageResource imageResource,
                                ApiImageSize apiImageSize,
                                ImageView imageView,
                                Bitmap placeholder,
                                boolean isHighPriority) {
        final ImageViewAware imageAware = new ImageViewAware(imageView, false);
        final Drawable placeholderDrawable = placeholder != null ? new BitmapDrawable(placeholder) :
                                             getPlaceholderDrawable(imageResource.getUrn(), imageAware);

        imageLoader.displayImage(
                buildUrlIfNotPreviouslyMissing(imageResource, apiImageSize),
                imageAware,
                ImageOptionsFactory.player(placeholderDrawable, isHighPriority),
                notFoundListener);
    }

    public void displayLeaveBehind(Uri uri, ImageView imageView, ImageListener imageListener) {
        final ImageViewAware imageAware = new ImageViewAware(imageView, false);
        imageLoader.displayImage(
                uri.toString(),
                imageAware,
                ImageOptionsFactory.adImage(),
                new ImageListenerUILAdapter(imageListener));
    }

    @Deprecated // use the ImageResource variant instead
    public void displayInFullDialogView(Urn urn,
                                        ApiImageSize apiImageSize,
                                        ImageView imageView,
                                        ImageListener imageListener) {
        imageLoader.displayImage(
                buildUrlIfNotPreviouslyMissing(urn, apiImageSize),
                new ImageViewAware(imageView, false),
                ImageOptionsFactory.fullImageDialog(),
                new ImageListenerUILAdapter(imageListener));
    }

    public void precacheArtwork(ImageResource imageResource, ApiImageSize apiImageSize) {
        String url = buildUrlIfNotPreviouslyMissing(imageResource, apiImageSize);
        imageLoader.loadImage(url, ImageOptionsFactory.prefetch(), null);
    }

    private void load(ImageResource imageResource,
                      ApiImageSize apiImageSize,
                      int targetWidth,
                      int targetHeight,
                      ImageListener imageListener) {
        ImageSize targetSize = new ImageSize(targetWidth, targetHeight);
        ImageAware imageAware = new NonViewAware(targetSize, ViewScaleType.CROP);
        imageLoader.displayImage(buildUrlIfNotPreviouslyMissing(imageResource, apiImageSize),
                                 imageAware,
                                 new ImageListenerUILAdapter(imageListener));
    }

    public void displayCircular(String imageUrl, ImageView imageView) {
        imageLoader.displayImage(imageUrl, new ImageViewAware(imageView, false),
                                 ImageOptionsFactory.placeholderCircular(imageView.getResources()
                                                                                  .getDrawable(R.drawable.circular_placeholder)));
    }

    public Observable<Bitmap> bitmap(final Uri uri) {
        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber) {
                // We pass NonViewAware to circumvent ImageLoader cancelling requests (https://github.com/nostra13/Android-Universal-Image-Loader/issues/681)
                imageLoader.displayImage(uri.toString(),
                                         new NonViewAware(new ImageSize(0, 0), ViewScaleType.CROP),
                                         ImageOptionsFactory.adImage(),
                                         new ImageListenerUILAdapter(bitmapAdapterFactory.create(subscriber)));
            }
        });
    }

    public Observable<Bitmap> bitmap(final ImageResource imageResource, ApiImageSize size) {
        return bitmap(Uri.parse(buildUrlIfNotPreviouslyMissing(imageResource, size)));
    }

    public Observable<Bitmap> artwork(final ImageResource imageResource, final ApiImageSize apiImageSize) {
        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber) {
                final Bitmap fallback = createFallbackBitmap(imageResource.getUrn(), apiImageSize);
                imageLoader.loadImage(
                        buildUrlIfNotPreviouslyMissing(imageResource, apiImageSize),
                        new ImageListenerUILAdapter(adapterFactory.create(subscriber, fallback)));
            }
        });
    }

    public Observable<Bitmap> artwork(final ImageResource imageResource,
                                      final ApiImageSize apiImageSize,
                                      final int targetWidth,
                                      final int targetHeight) {
        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber) {
                final GradientDrawable fallbackDrawable = placeholderGenerator.generateDrawable(imageResource.getUrn()
                                                                                                             .toString());
                final Bitmap fallback = ImageUtils.toBitmap(fallbackDrawable, targetWidth, targetHeight);
                load(imageResource,
                     apiImageSize,
                     targetWidth,
                     targetHeight,
                     adapterFactory.create(subscriber, fallback));
            }
        });
    }

    public Observable<Bitmap> blurredPlayerArtwork(final Resources resources, final ImageResource imageResource,
                                                   Scheduler scheduleOn, Scheduler observeOn) {
        return blurredArtwork(resources, imageResource, Optional.<Float>absent(), scheduleOn, observeOn);
    }

    public Observable<Bitmap> blurredArtwork(final Resources resources,
                                             final ImageResource imageResource,
                                             Optional<Float> blurRadius,
                                             Scheduler scheduleOn, Scheduler observeOn) {
        final Bitmap cachedBlurImage = blurredImageCache.get(imageResource.getUrn());
        if (cachedBlurImage != null) {
            return Observable.just(cachedBlurImage);
        } else {
            final Bitmap cached = getCachedListItemBitmap(resources, imageResource);
            if (cached == null) {
                return artwork(imageResource, ApiImageSize.getListItemImageSize(resources))
                        .map(blurBitmap(blurRadius))
                        .subscribeOn(scheduleOn)
                        .observeOn(observeOn)
                        .doOnNext(cacheBlurredBitmap(imageResource.getUrn()));
            } else {
                return blurBitmap(cached, blurRadius)
                        .subscribeOn(scheduleOn)
                        .observeOn(observeOn)
                        .doOnNext(cacheBlurredBitmap(imageResource.getUrn()));
            }
        }
    }

    private Func1<Bitmap, Bitmap> blurBitmap(final Optional<Float> blurRadius) {
        return new Func1<Bitmap, Bitmap>() {
            @Override
            public Bitmap call(Bitmap bitmap) {
                return imageProcessor.blurBitmap(bitmap, blurRadius);
            }
        };
    }

    private Action1<Bitmap> cacheBlurredBitmap(final Urn resourceUrn) {
        return new Action1<Bitmap>() {
            @Override
            public void call(Bitmap bitmap) {
                blurredImageCache.put(resourceUrn, bitmap);
            }
        };
    }

    private Observable<Bitmap> blurBitmap(final Bitmap original, final Optional<Float> blurRadius) {
        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber) {
                subscriber.onNext(imageProcessor.blurBitmap(original, blurRadius));
                subscriber.onCompleted();
            }
        });
    }

    @Nullable
    public Bitmap getCachedListItemBitmap(Resources resources, ImageResource imageResource) {
        return getCachedBitmap(imageResource, ApiImageSize.getListItemImageSize(resources),
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
    public Bitmap getCachedBitmap(ImageResource imageResource,
                                  ApiImageSize apiImageSize,
                                  int targetWidth,
                                  int targetHeight) {
        final String imageUrl = imageUrlBuilder.buildUrl(imageResource, apiImageSize);
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
    public Bitmap decodeResource(Resources resources, int resId) {
        try {
            return BitmapFactory.decodeResource(resources, resId);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
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
    private TransitionDrawable getCircularPlaceholderDrawable(final Urn urn, int width, int height) {
        final String key = String.format(PLACEHOLDER_KEY_BASE, urn, String.valueOf(width), String.valueOf(height));
        return placeholderCache.get(key, new ValueProvider<String, TransitionDrawable>() {
            @Override
            public TransitionDrawable get(String key) {
                return circularPlaceholderGenerator.generateTransitionDrawable(urn.toString());
            }
        });
    }

    @Nullable
    private String buildUrlIfNotPreviouslyMissing(ImageResource imageResource, ApiImageSize apiImageSize) {
        final String imageUrl = imageUrlBuilder.buildUrl(imageResource, apiImageSize);
        Log.d(TAG, "ImageResource " + imageResource.getUrn() + "; url=" + imageUrl);
        return notFoundUris.contains(imageUrl) ? null : imageUrl;
    }

    @Nullable
    private String buildUrlIfNotPreviouslyMissing(Urn urn, ApiImageSize apiImageSize) {
        final String imageUrl = imageUrlBuilder.imageResolverUrl(urn, apiImageSize);
        Log.d(TAG, "URN " + urn + "; url=" + imageUrl);
        return notFoundUris.contains(imageUrl) ? null : imageUrl;
    }

    @Nullable
    public String getImageUrl(Urn urn, ApiImageSize apiImageSize) {
        return buildUrlIfNotPreviouslyMissing(urn, apiImageSize);
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
                Log.d(TAG, "404 Not Found for " + imageUri);
                notFoundUris.add(imageUri);
            } else {
                Log.d(TAG, "Failed loading " + imageUri + "; reason: " + failReason.getType());
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
