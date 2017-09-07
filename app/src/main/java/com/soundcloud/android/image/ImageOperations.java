package com.soundcloud.android.image;

import static com.soundcloud.android.image.ImageOperations.DisplayType.CIRCULAR;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
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
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.images.ImageUtils;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Scheduler;
import rx.Subscriber;
import rx.functions.Func1;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.graphics.Palette;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Singleton
public class ImageOperations {

    public enum DisplayType {
        DEFAULT, CIRCULAR
    }

    private static final Func1<Bitmap, Observable<Palette>> BITMAP_TO_PALETTE = bitmap -> Observable.create(
            (OnSubscribe<Palette>) subscriber -> Palette.from(bitmap)
                                                        .generate(palette -> {
                                                            if (!subscriber.isUnsubscribed()) {
                                                                subscriber.onNext(
                                                                        palette);
                                                                subscriber.onCompleted();
                                                            }
                                                        }));
    private final ImageLoader imageLoader;
    private final ImageUrlBuilder imageUrlBuilder;
    private final Set<String> notFoundUris = new HashSet<>();
    private final FallbackBitmapLoadingAdapter.Factory adapterFactory;
    private final BitmapLoadingAdapter.Factory bitmapAdapterFactory;
    private final DeviceHelper deviceHelper;
    private final PlaceholderGenerator placeholderGenerator;
    private final CircularPlaceholderGenerator circularPlaceholderGenerator;
    private final ImageCache imageCache;
    private final FallbackImageListener notFoundListener = new FallbackImageListener(notFoundUris);
    private final ImageProcessor imageProcessor;

    @Inject
    public ImageOperations(
            ImageLoader imageLoader,
            ImageUrlBuilder imageUrlBuilder,
            FallbackBitmapLoadingAdapter.Factory adapterFactory,
            BitmapLoadingAdapter.Factory bitmapAdapterFactory,
            ImageProcessor imageProcessor,
            DeviceHelper deviceHelper,
            PlaceholderGenerator placeholderGenerator,
            CircularPlaceholderGenerator circularPlaceholderGenerator,
            ImageCache imageCache) {
        this.imageUrlBuilder = imageUrlBuilder;
        this.adapterFactory = adapterFactory;
        this.bitmapAdapterFactory = bitmapAdapterFactory;
        this.imageProcessor = imageProcessor;
        this.deviceHelper = deviceHelper;
        this.placeholderGenerator = placeholderGenerator;
        this.circularPlaceholderGenerator = circularPlaceholderGenerator;
        this.imageCache = imageCache;

        this.imageLoader = imageLoader;
        this.imageLoader.init(imageCache.getImageLoaderConfiguration());
    }

    public void clearDiskCache() {
        imageLoader.clearDiskCache();
    }

    public void displayInAdapterView(Urn urn, Optional<String> imageUrlTemplate, ApiImageSize apiImageSize, ImageView imageView, DisplayType displayType) {
        displayInAdapterView(apiImageSize,
                             imageView,
                             getImageUrl(imageUrlTemplate, urn, apiImageSize),
                             notFoundListener,
                             Optional.absent(),
                             displayType);
    }

    public Observable<Bitmap> displayInAdapterView(final ImageResource imageResource,
                                                   final ApiImageSize apiImageSize,
                                                   final ImageView imageView,
                                                   final Optional<Drawable> fallbackDrawable,
                                                   DisplayType displayType) {
        return Observable.create(subscriber -> displayInAdapterView(apiImageSize,
                                                                    imageView,
                                                                    getImageUrl(imageResource.getImageUrlTemplate(), imageResource.getUrn(), apiImageSize),
                                                                    buildFallbackImageListener(fromBitmapSubscriber(subscriber)),
                                                                    fallbackDrawable,
                                                                    displayType));
    }

    private void displayInAdapterView(ApiImageSize apiImageSize,
                                      ImageView imageView,
                                      String imageUrl,
                                      FallbackImageListener imageListener,
                                      Optional<Drawable> placeholderDrawable,
                                      DisplayType displayType) {
        final ImageViewAware imageAware = new ImageViewAware(imageView, false);
        final Drawable drawable = placeholderDrawable.isPresent() ?
                                  placeholderDrawable.get() :
                                  getPlaceholderDrawable(imageUrl, imageAware.getWidth(), imageAware.getHeight(), displayType);

        DisplayImageOptions options = ImageOptionsFactory.adapterView(drawable, apiImageSize, deviceHelper);
        if (displayType.equals(DisplayType.CIRCULAR) && placeholderDrawable.isPresent()) {
            options = ImageOptionsFactory.adapterViewCircular(placeholderDrawable.get(), apiImageSize, deviceHelper);
        }
        imageLoader.displayImage(imageUrl, imageAware, options, imageListener);
    }

    public Observable<Palette> displayCircularInAdapterViewAndGeneratePalette(final ImageResource imageResource,
                                                                              final ApiImageSize apiImageSize,
                                                                              final ImageView imageView) {
        return displayInAdapterView(imageResource, apiImageSize, imageView, Optional.absent(), DisplayType.CIRCULAR).flatMap(BITMAP_TO_PALETTE);
    }

    public void displayDefaultPlaceholder(ImageView imageView) {
        displayWithPlaceholder(imageView, null, Optional.absent());
    }

    public void displayWithPlaceholder(Urn urn, Optional<String> imageUrlTemplate, ApiImageSize apiImageSize, ImageView imageView) {
        displayWithPlaceholder(imageView, getImageUrl(imageUrlTemplate, urn, apiImageSize), Optional.absent());
    }

    private void displayWithPlaceholder(ImageView imageView, @Nullable String imageUrl, Optional<ImageLoadingListener> imageListener) {
        final ImageViewAware imageAware = new ImageViewAware(imageView, false);
        imageLoader.displayImage(
                imageUrl,
                imageAware,
                ImageOptionsFactory.placeholder(getPlaceholderDrawable(imageUrl, imageAware.getWidth(), imageAware.getHeight(), DisplayType.DEFAULT)),
                imageListener.isPresent() ? imageListener.get() : notFoundListener);
    }

    public Observable<Bitmap> displayWithPlaceholderObservable(ImageResource imageResource, ApiImageSize apiImageSize, ImageView imageView) {
        return Observable.create(subscriber -> displayWithPlaceholder(imageView,
                                                                      getImageUrl(imageResource.getImageUrlTemplate(), imageResource.getUrn(), apiImageSize),
                                                                      Optional.of(buildFallbackImageListener(bitmapAdapterFactory.create(subscriber)))));
    }


    public void displayCircular(String imageUrl, ImageView imageView) {
        imageLoader.displayImage(imageUrl, new ImageViewAware(imageView, false),
                                 ImageOptionsFactory.placeholderCircular(imageView.getResources()
                                                                                  .getDrawable(R.drawable.circular_placeholder)));
    }

    public void displayCircularWithPlaceholder(Urn urn, Optional<String> imageUrlTemplate,
                                               ApiImageSize apiImageSize,
                                               ImageView imageView) {
        String imageUrl = getImageUrl(imageUrlTemplate, urn, apiImageSize);
        final ImageViewAware imageAware = new ImageViewAware(imageView, false);
        final DisplayImageOptions options = ImageOptionsFactory.placeholderCircular(
                imageCache.getPlaceholderDrawable(imageUrl, imageAware.getWidth(), imageAware.getHeight(), circularPlaceholderGenerator));
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
                                             getPlaceholderDrawable(imageResource.getUrn().toString(), imageAware.getWidth(), imageAware.getHeight(), DisplayType.DEFAULT);

        imageLoader.displayImage(
                getImageUrl(imageResource.getImageUrlTemplate(), imageResource.getUrn(), apiImageSize),
                imageAware,
                ImageOptionsFactory.player(placeholderDrawable, isHighPriority),
                notFoundListener);
    }

    public void displayAdImage(Urn urn, String imageUri, ImageView imageView, final ImageListener listener) {
        displayAdImage(urn, imageUri, imageView, new ImageListenerUILAdapter(listener));
    }

    public void displayLeaveBehind(Uri uri, ImageView imageView, ImageListener imageListener) {
        final ImageViewAware imageAware = new ImageViewAware(imageView, false);
        imageLoader.displayImage(
                uri.toString(),
                imageAware,
                ImageOptionsFactory.adImage(),
                new ImageListenerUILAdapter(imageListener));
    }

    public void displayInFullDialogView(ImageResource imageResource,
                                        ApiImageSize apiImageSize,
                                        ImageView imageView,
                                        ImageListener imageListener) {
        imageLoader.displayImage(
                getImageUrl(imageResource.getImageUrlTemplate(), imageResource.getUrn(), apiImageSize),
                new ImageViewAware(imageView, false),
                ImageOptionsFactory.fullImageDialog(),
                new ImageListenerUILAdapter(imageListener));
    }

    public Observable<Bitmap> bitmap(final Uri uri) {
        return Observable.create(subscriber -> {
            // We pass NonViewAware to circumvent ImageLoader cancelling requests (https://github.com/nostra13/Android-Universal-Image-Loader/issues/681)
            imageLoader.displayImage(uri.toString(),
                                     new NonViewAware(new ImageSize(0, 0), ViewScaleType.CROP),
                                     ImageOptionsFactory.adImage(),
                                     new ImageListenerUILAdapter(bitmapAdapterFactory.create(subscriber)));
        });
    }

    public Observable<Bitmap> artwork(final ImageResource imageResource, final ApiImageSize apiImageSize) {
        return Observable.create(subscriber -> {
            final Bitmap fallback = createFallbackBitmap(imageResource.getUrn(), apiImageSize);
            imageLoader.loadImage(
                    getImageUrl(imageResource.getImageUrlTemplate(), imageResource.getUrn(), apiImageSize),
                    new ImageListenerUILAdapter(adapterFactory.create(subscriber, fallback)));
        });
    }

    public Observable<Bitmap> artwork(final ImageResource imageResource,
                                      final ApiImageSize apiImageSize,
                                      final int targetWidth,
                                      final int targetHeight) {
        return Observable.create(subscriber -> {
            final GradientDrawable fallbackDrawable = generateDrawable(imageResource);
            final Bitmap fallback = ImageUtils.toBitmap(fallbackDrawable, targetWidth, targetHeight);
            load(imageResource,
                 apiImageSize,
                 targetWidth,
                 targetHeight,
                 adapterFactory.create(subscriber, fallback));
        });
    }

    public void precacheArtwork(ImageResource imageResource, ApiImageSize apiImageSize) {
        String url = getImageUrl(imageResource.getImageUrlTemplate(), imageResource.getUrn(), apiImageSize);
        imageLoader.loadImage(url, ImageOptionsFactory.prefetch(), null);
    }

    @Nullable
    public Bitmap getCachedListItemBitmap(Resources resources, ImageResource imageResource) {
        return getCachedBitmap(imageResource, ApiImageSize.getListItemImageSize(resources),
                               resources.getDimensionPixelSize(R.dimen.list_item_image_dimension),
                               resources.getDimensionPixelSize(R.dimen.list_item_image_dimension));
    }

    @Nullable
    public Bitmap getCachedBitmap(ImageResource imageResource,
                                  ApiImageSize apiImageSize,
                                  int targetWidth,
                                  int targetHeight) {
        final String imageUrl = imageUrlBuilder.buildUrl(imageResource.getImageUrlTemplate(), imageResource.getUrn(), apiImageSize);
        final String key = MemoryCacheUtils.generateKey(imageUrl, new ImageSize(targetWidth, targetHeight));
        return imageLoader.getMemoryCache().get(key);
    }

    public AbsListView.OnScrollListener createScrollPauseListener(boolean pauseOnScroll, boolean pauseOnFling,
                                                                  AbsListView.OnScrollListener customListener) {
        return new PauseOnScrollListener(imageLoader, pauseOnScroll, pauseOnFling, customListener);
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

    private DefaultImageListener fromBitmapSubscriber(final Subscriber<? super Bitmap> subscriber) {
        return new DefaultImageListener() {
            @Override
            public void onLoadingFailed(String imageUri,
                                        View view,
                                        @Nullable Throwable cause) {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onError(cause == null ? new IOException("Failed to load bitmap for Unknown reason") : cause);
                }
            }

            @Override
            public void onLoadingComplete(String imageUri,
                                          View view,
                                          Bitmap loadedImage) {
                if (subscriber.isUnsubscribed()) {
                    return;
                }

                if (imageUri == null && loadedImage == null) {
                    subscriber.onError(new IOException(
                            "Image loading failed."));
                    return;
                }
                subscriber.onNext(loadedImage);
                subscriber.onCompleted();
            }
        };
    }

    private void displayAdImage(Urn urn, String imageUri, ImageView imageView, ImageLoadingListener listener) {
        final ImageViewAware imageAware = new ImageViewAware(imageView, false);
        final Drawable drawable = getPlaceholderDrawable(urn.toString(), imageAware.getWidth(), imageAware.getHeight(), DisplayType.DEFAULT);
        final DisplayImageOptions options = ImageOptionsFactory.streamAdImage(drawable, deviceHelper);

        imageLoader.displayImage(imageUri, imageAware, options, listener);
    }

    private void load(ImageResource imageResource,
                      ApiImageSize apiImageSize,
                      int targetWidth,
                      int targetHeight,
                      ImageListener imageListener) {
        ImageSize targetSize = new ImageSize(targetWidth, targetHeight);
        ImageAware imageAware = new NonViewAware(targetSize, ViewScaleType.CROP);
        imageLoader.displayImage(getImageUrl(imageResource.getImageUrlTemplate(), imageResource.getUrn(), apiImageSize),
                                 imageAware,
                                 new ImageListenerUILAdapter(imageListener));
    }

    private Func1<Bitmap, Bitmap> blurBitmap(final Optional<Float> blurRadius) {
        return bitmap -> imageProcessor.blurBitmap(bitmap, blurRadius);
    }


    private Observable<Bitmap> blurBitmap(final Bitmap original, final Optional<Float> blurRadius) {
        return Observable.create(subscriber -> {
            subscriber.onNext(imageProcessor.blurBitmap(original, blurRadius));
            subscriber.onCompleted();
        });
    }

    @Nullable
    public String getImageUrl(Optional<String> imageUrlTemplate, Urn urn, ApiImageSize apiImageSize) {
        final String imageUrl = imageUrlBuilder.buildUrl(imageUrlTemplate, urn, apiImageSize);
        return notFoundUris.contains(imageUrl) ? null : imageUrl;
    }

    @NonNull
    private FallbackImageListener buildFallbackImageListener(ImageListener imageListener) {
        return new FallbackImageListener(imageListener, notFoundUris);
    }

    public Observable<Bitmap> blurredArtwork(final Resources resources,
                                             final ImageResource imageResource,
                                             Optional<Float> blurRadius,
                                             Scheduler scheduleOn, Scheduler observeOn) {
        final Bitmap cachedBlurImage = imageCache.getBlurredImage(imageResource.getUrn());
        if (cachedBlurImage != null) {
            return Observable.just(cachedBlurImage);
        } else {
            final Bitmap cached = getCachedListItemBitmap(resources, imageResource);
            if (cached == null) {
                return artwork(imageResource, ApiImageSize.getListItemImageSize(resources))
                        .map(blurBitmap(blurRadius))
                        .subscribeOn(scheduleOn)
                        .observeOn(observeOn)
                        .doOnNext(imageCache.cacheBlurredBitmap(imageResource.getUrn()));
            } else {
                return blurBitmap(cached, blurRadius)
                        .subscribeOn(scheduleOn)
                        .observeOn(observeOn)
                        .doOnNext(imageCache.cacheBlurredBitmap(imageResource.getUrn()));
            }
        }
    }

    GradientDrawable generateDrawable(ImageResource imageResource) {
        return placeholderGenerator.generateDrawable(imageResource.getUrn().toString());
    }


    @Nullable
    TransitionDrawable getPlaceholderDrawable(@Nullable final String imageUrl, int width, int height, ImageOperations.DisplayType displayType) {
        PlaceholderGenerator placeholderGenerator = displayType.equals(CIRCULAR) ? this.circularPlaceholderGenerator : this.placeholderGenerator;
        return imageCache.getPlaceholderDrawable(imageUrl,
                                                 width,
                                                 height,
                                                 placeholderGenerator);
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

    void resume() {
        imageLoader.resume();
    }

    void pause() {
        imageLoader.pause();
    }
}