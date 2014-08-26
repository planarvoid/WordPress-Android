package com.soundcloud.android.image;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;
import com.nostra13.universalimageloader.cache.disc.naming.HashCodeFileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageSize;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.nostra13.universalimageloader.core.listener.PauseOnScrollListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.utils.MemoryCacheUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.cache.FileCache;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.utils.images.ImageUtils;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Subscriber;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
public class ImageOperations {

    private static final int LOW_MEM_DEVICE_THRESHOLD = 50 * 1024 * 1024; // available mem in bytes
    private static final Pattern PATTERN = Pattern.compile("^https?://(.+)");
    private static final String URL_BASE = "http://%s";
    private static final String PLACEHOLDER_KEY_BASE = "%s_%s_%s";

    private final ImageLoader imageLoader;
    private final ImageEndpointBuilder imageEndpointBuilder;
    private final PlaceholderGenerator placeholderGenerator;

    private final Set<String> notFoundUris = Sets.newHashSet();
    private final Cache<String, Drawable> placeholderCache;
    private final ViewlessLoadingAdapter.Factory viewlessLoadingAdapterFactory;
    private final FileNameGenerator fileNameGenerator;

    @Inject
    public ImageOperations(ImageEndpointBuilder imageEndpointBuilder, PlaceholderGenerator placeholderGenerator,
                           ViewlessLoadingAdapter.Factory viewlessLoadingAdapterFactory) {
        this(ImageLoader.getInstance(), imageEndpointBuilder, placeholderGenerator, viewlessLoadingAdapterFactory,
                CacheBuilder.newBuilder().weakValues().maximumSize(50).<String, Drawable>build(),
                new HashCodeFileNameGenerator());

    }

    private final SimpleImageLoadingListener notFoundListener = new SimpleImageLoadingListener() {

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            super.onLoadingComplete(imageUri, view, loadedImage);
            if (loadedImage == null) {
                animatePlaceholder(view);
            }
        }

        @Override
        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
            super.onLoadingFailed(imageUri, view, failReason);
            if (failReason.getCause() instanceof FileNotFoundException) {
                notFoundUris.add(imageUri);
            }
            animatePlaceholder(view);
        }

        private void animatePlaceholder(View view) {
            if (view instanceof ImageView && ((ImageView) view).getDrawable() instanceof OneShotTransitionDrawable) {
                final OneShotTransitionDrawable transitionDrawable = (OneShotTransitionDrawable) ((ImageView) view).getDrawable();
                transitionDrawable.startTransition(ImageUtils.DEFAULT_TRANSITION_DURATION);
            }
        }
    };

    @VisibleForTesting
    ImageOperations(ImageLoader imageLoader, ImageEndpointBuilder imageEndpointBuilder, PlaceholderGenerator placeholderGenerator, ViewlessLoadingAdapter.Factory viewlessLoadingAdapterFactory, Cache<String, Drawable> placeholderCache,
                    FileNameGenerator fileNameGenerator) {
        this.imageLoader = imageLoader;
        this.imageEndpointBuilder = imageEndpointBuilder;
        this.placeholderGenerator = placeholderGenerator;
        this.placeholderCache = placeholderCache;
        this.viewlessLoadingAdapterFactory = viewlessLoadingAdapterFactory;
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

        FileCache.installFileCache(IOUtils.getCacheDir(appContext));
    }

    public void displayInAdapterView(Urn urn, ApiImageSize apiImageSize, ImageView imageView) {
        final ImageViewAware imageAware = new ImageViewAware(imageView, false);
        imageLoader.displayImage(
                buildUrlIfNotPreviouslyMissing(urn, apiImageSize),
                imageAware,
                ImageOptionsFactory.adapterView(getPlaceholderDrawable(urn, imageAware)), notFoundListener);
    }

    public void displayWithPlaceholder(Urn urn, ApiImageSize apiImageSize, ImageView imageView) {
        final ImageViewAware imageAware = new ImageViewAware(imageView, false);
        imageLoader.displayImage(
                buildUrlIfNotPreviouslyMissing(urn, apiImageSize),
                imageAware,
                ImageOptionsFactory.placeholder(getPlaceholderDrawable(urn, imageAware)),
                notFoundListener);
    }

    public void displayInPlayer(Urn urn, ApiImageSize apiImageSize, ImageView imageView, ImageListener imageListener, Bitmap placeholder, boolean isHighPriority) {
        final ImageViewAware imageAware = new ImageViewAware(imageView, false);
        final Drawable placeholderDrawable = placeholder != null ? new BitmapDrawable(placeholder) :
                getPlaceholderDrawable(urn, imageAware);

        imageLoader.displayImage(
                buildUrlIfNotPreviouslyMissing(urn, apiImageSize),
                imageAware,
                ImageOptionsFactory.player(placeholderDrawable, isHighPriority),
                new ImageListenerUILAdapter(imageListener));
    }

    public void displayAdInPlayer(Uri uri, ImageView imageView, Drawable placeholderDrawable) {
        final ImageViewAware imageAware = new ImageViewAware(imageView, false);
        imageLoader.displayImage(
                uri.toString(),
                imageAware,
                ImageOptionsFactory.playerAd(placeholderDrawable));
    }

    public void displayInFullDialogView(Urn urn, ApiImageSize apiImageSize, ImageView imageView, ImageListener imageListener) {
        imageLoader.displayImage(
                buildUrlIfNotPreviouslyMissing(urn, apiImageSize),
                new ImageViewAware(imageView, false),
                ImageOptionsFactory.fullImageDialog(),
                new ImageListenerUILAdapter(imageListener));
    }

    public void load(Urn urn, ApiImageSize apiImageSize, ImageListener imageListener) {
        imageLoader.loadImage(
                buildUrlIfNotPreviouslyMissing(urn, apiImageSize),
                new ImageListenerUILAdapter(imageListener));
    }

    public void load(Urn urn, ApiImageSize apiImageSize, int targetWidth, int targetHeight, ImageListener imageListener) {
        imageLoader.loadImage(
                buildUrlIfNotPreviouslyMissing(urn, apiImageSize),
                new ImageSize(targetWidth, targetHeight),
                new ImageListenerUILAdapter(imageListener));
    }

    @Deprecated // use the variants that take URNs instead
    public void load(String imageUrl, ImageListener imageListener) {
        imageLoader.loadImage(adjustUrl(imageUrl), new ImageListenerUILAdapter(imageListener));
    }

    @Deprecated // use the variants that take URNs instead
    public void display(String imageUrl, ImageView imageView) {
        imageLoader.displayImage(adjustUrl(imageUrl), new ImageViewAware(imageView, false));
    }

    @Deprecated // use the variants that take URNs instead
    public void prefetch(String imageUrl) {
        imageLoader.loadImage(adjustUrl(imageUrl), ImageOptionsFactory.prefetch(), null);
    }

    public Observable<Bitmap> image(final Urn resourceUrn, final ApiImageSize apiImageSize, final boolean emitCopy) {
        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber) {
                load(resourceUrn, apiImageSize, viewlessLoadingAdapterFactory.create(subscriber, emitCopy));
            }
        });
    }

    public Observable<Bitmap> image(final Urn resourceUrn, final ApiImageSize apiImageSize, final int targetWidth,
                                    final int targetHeight, final boolean emitCopy) {
        return Observable.create(new Observable.OnSubscribe<Bitmap>() {
            @Override
            public void call(Subscriber<? super Bitmap> subscriber) {
                load(resourceUrn, apiImageSize, targetWidth, targetHeight, viewlessLoadingAdapterFactory.create(subscriber, emitCopy));
            }
        });
    }

    public Bitmap getCachedListItemBitmap(Resources resources, Urn resourceUrn){
        return getCachedBitmap(resourceUrn, ApiImageSize.getListItemImageSize(resources),
                resources.getDimensionPixelSize(R.dimen.list_item_image_dimension),
                resources.getDimensionPixelSize(R.dimen.list_item_image_dimension));
    }

    @Nullable
    public Bitmap getCachedBitmap(Urn resourceUrn, ApiImageSize apiImageSize, int targetWidth, int targetHeight){
        final String imageUrl = imageEndpointBuilder.imageUrl(resourceUrn, apiImageSize);
        if (notFoundUris.contains(imageUrl)) {
            return null;
        }

        final String key = MemoryCacheUtils.generateKey(imageUrl, new ImageSize(targetWidth, targetHeight));
        return imageLoader.getMemoryCache().get(key);
    }

    public Uri getLocalImageUri(Urn resourceUrn, ApiImageSize apiImageSize){
        final String imageUri = buildUrlIfNotPreviouslyMissing(resourceUrn, apiImageSize);
        if (imageUri != null){
            final File cacheDir = imageLoader.getDiskCache().getDirectory().getAbsoluteFile();
            final File imageFile = new File(cacheDir, fileNameGenerator.generate(imageUri));
            return Uri.fromFile(imageFile);
        }
        return null;
    }

    public void resume() {
        imageLoader.resume();
    }

    public void cancel(ImageView imageView) {
        imageLoader.cancelDisplayTask(imageView);
    }

    public AbsListView.OnScrollListener createScrollPauseListener(boolean pauseOnScroll, boolean pauseOnFling,
                                                                  AbsListView.OnScrollListener customListener) {
        return new PauseOnScrollListener(imageLoader, pauseOnScroll, pauseOnFling, customListener);
    }

    public AbsListView.OnScrollListener createScrollPauseListener(boolean pauseOnScroll, boolean pauseOnFling) {
        return new PauseOnScrollListener(imageLoader, pauseOnScroll, pauseOnFling);
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

    /**
     * We have to store these so so we don't animate on every load attempt. this prevents flickering
     */
    @Nullable
    private Drawable getPlaceholderDrawable(final Urn urn, ImageViewAware imageViewAware) {
        final String key = String.format(PLACEHOLDER_KEY_BASE, urn,
                String.valueOf(imageViewAware.getWidth()), String.valueOf(imageViewAware.getHeight()));
        try {
            return placeholderCache.get(key, new Callable<Drawable>() {
                @Override
                public Drawable call() throws Exception {
                    return placeholderGenerator.generate(urn.toString());
                }
            });
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }

    }

    @Nullable
    private String buildUrlIfNotPreviouslyMissing(Urn urn, ApiImageSize apiImageSize) {
        final String imageUrl = imageEndpointBuilder.imageUrl(urn, apiImageSize);
        return notFoundUris.contains(imageUrl) ? null : imageUrl;
    }
}
