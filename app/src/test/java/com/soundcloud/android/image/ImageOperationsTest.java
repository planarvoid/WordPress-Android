package com.soundcloud.android.image;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.image.ImageOptionsFactory.DELAY_BEFORE_LOADING;
import static com.soundcloud.android.image.ImageOptionsFactory.PlaceholderTransitionDisplayer;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.cache.Cache;
import com.nostra13.universalimageloader.cache.disc.DiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.MemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowBitmapDrawable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

@RunWith(SoundCloudTestRunner.class)
public class ImageOperationsTest {

    private ImageOperations imageOperations;

    private static final int RES_ID = 123;

    @Mock ImageLoader imageLoader;
    @Mock ImageEndpointBuilder imageEndpointBuilder;
    @Mock PlaceholderGenerator placeholderGenerator;
    @Mock DiskCache diskCache;
    @Mock MemoryCache memoryCache;
    @Mock ImageListener imageListener;
    @Mock ImageView imageView;
    @Mock Resources resources;
    @Mock TransitionDrawable transitionDrawable;
    @Mock GradientDrawable gradientDrawable;
    @Mock View parentView;
    @Mock FailReason failReason;
    @Mock Cache placeholderCache;
    @Mock Cache blurCache;
    @Mock FallbackBitmapLoadingAdapter.Factory viewlessLoadingAdapterFactory;
    @Mock FallbackBitmapLoadingAdapter fallbackBitmapLoadingAdapter;
    @Mock FileNameGenerator fileNameGenerator;
    @Mock ImageProcessor imageProcessor;

    @Captor ArgumentCaptor<ImageViewAware> imageViewAwareCaptor;
    @Captor ArgumentCaptor<DisplayImageOptions> displayOptionsCaptor;
    @Captor ArgumentCaptor<ImageLoadingListener> imageLoadingListenerCaptor;

    final private String URL = "https://i1.sndcdn.com/artworks-000058493054-vcrifw-t500x500.jpg?b09b136";
    final private String ADJUSTED_URL = "http://i1.sndcdn.com/artworks-000058493054-vcrifw-t500x500.jpg?b09b136";

    final private static String RESOLVER_URL_LARGE = "http://api.soundcloud.com/app/mobileapps/images/soundcloud:tracks:1/large";
    final private static Urn URN = new Urn("soundcloud:tracks:1");

    private final Scheduler graphicsScheduler = Schedulers.immediate();

    @Before
    public void setUp() throws Exception {
        imageOperations = new ImageOperations(imageLoader, imageEndpointBuilder, placeholderGenerator, viewlessLoadingAdapterFactory, imageProcessor, placeholderCache, blurCache, fileNameGenerator);
        when(imageLoader.getDiskCache()).thenReturn(diskCache);
        when(imageLoader.getMemoryCache()).thenReturn(memoryCache);
        when(imageEndpointBuilder.imageUrl(URN, ApiImageSize.LARGE)).thenReturn(RESOLVER_URL_LARGE);
        when(placeholderGenerator.generateTransitionDrawable(any(String.class))).thenReturn(transitionDrawable);
        when(placeholderGenerator.generateDrawable(any(String.class))).thenReturn(gradientDrawable);
    }

    @Test
    public void NotFoundExceptionDuringAdapterViewLoadMakesNextLoadToPassNullPath() throws ExecutionException {
        when(failReason.getCause()).thenReturn(new FileNotFoundException());

        // 1st load
        imageOperations.displayInAdapterView(URN, ApiImageSize.LARGE, imageView);
        InOrder inOrder = Mockito.inOrder(imageLoader);
        inOrder.verify(imageLoader).displayImage(eq(RESOLVER_URL_LARGE), any(ImageViewAware.class), any(DisplayImageOptions.class), imageLoadingListenerCaptor.capture());
        imageLoadingListenerCaptor.getValue().onLoadingFailed(RESOLVER_URL_LARGE, imageView, failReason);

        // 2nd load
        imageOperations.displayInAdapterView(URN, ApiImageSize.LARGE, imageView);
        inOrder.verify(imageLoader).displayImage((String) isNull(), any(ImageViewAware.class), any(DisplayImageOptions.class), any(SimpleImageLoadingListener.class));
        when(placeholderCache.get(anyString(), any(Callable.class))).thenReturn(transitionDrawable);
    }

    @Test
    public void shouldReturnNullForCachedBitmapWhenPreviousAttemptToLoadFailedWithNotFound() {
        when(failReason.getCause()).thenReturn(new FileNotFoundException());

        imageOperations.displayInAdapterView(URN, ApiImageSize.LARGE, imageView);
        verify(imageLoader).displayImage(eq(RESOLVER_URL_LARGE), any(ImageViewAware.class), any(DisplayImageOptions.class), imageLoadingListenerCaptor.capture());
        imageLoadingListenerCaptor.getValue().onLoadingFailed(RESOLVER_URL_LARGE, imageView, failReason);

        expect(imageOperations.getCachedBitmap(URN, ApiImageSize.LARGE, 100, 100)).toBeNull();
        verifyZeroInteractions(imageLoader);
    }

    @Test
    public void IOExceptionDuringAdapterViewLoadAllowsRetryWithUrl() {
        when(failReason.getCause()).thenReturn(new IOException());

        // 1st load
        imageOperations.displayInAdapterView(URN, ApiImageSize.LARGE, imageView);
        InOrder inOrder = Mockito.inOrder(imageLoader);
        inOrder.verify(imageLoader).displayImage(eq(RESOLVER_URL_LARGE), any(ImageViewAware.class), any(DisplayImageOptions.class), imageLoadingListenerCaptor.capture());
        imageLoadingListenerCaptor.getValue().onLoadingFailed(RESOLVER_URL_LARGE, imageView, failReason);

        // 2nd load
        imageOperations.displayInAdapterView(URN, ApiImageSize.LARGE, imageView);
        inOrder.verify(imageLoader).displayImage(eq(RESOLVER_URL_LARGE), any(ImageViewAware.class), any(DisplayImageOptions.class), any(SimpleImageLoadingListener.class));
    }

    @Test
    public void NotFoundExceptionDuringPlaceholderLoadMakesNextLoadToPassNullPath() {
        when(failReason.getCause()).thenReturn(new FileNotFoundException());

        // 1st load
        imageOperations.displayWithPlaceholder(URN, ApiImageSize.LARGE, imageView);
        InOrder inOrder = Mockito.inOrder(imageLoader);
        inOrder.verify(imageLoader).displayImage(eq(RESOLVER_URL_LARGE), any(ImageViewAware.class), any(DisplayImageOptions.class), imageLoadingListenerCaptor.capture());
        imageLoadingListenerCaptor.getValue().onLoadingFailed(RESOLVER_URL_LARGE, imageView, failReason);

        // 2nd load
        imageOperations.displayWithPlaceholder(URN, ApiImageSize.LARGE, imageView);
        inOrder.verify(imageLoader).displayImage((String) isNull(), any(ImageViewAware.class), any(DisplayImageOptions.class), any(SimpleImageLoadingListener.class));
    }

    @Test
    public void displayShouldCallDisplayWithAdjustedUrlAndImageViewAware() {
        imageOperations.display(URL, imageView);
        verify(imageLoader).displayImage(eq(ADJUSTED_URL), imageViewAwareCaptor.capture());
        expect(imageViewAwareCaptor.getValue().getWrappedView()).toBe(imageView);
        expect(imageViewAwareCaptor.getValue().getWidth()).toBe(0);
    }

    @Test
    public void displayImageInAdapterViewShouldRequestImagesThroughMobileImageResolver() {
        final String imageUrl = "http://api.soundcloud.com/app/mobileapps/images/soundcloud:tracks:1/large";
        when(imageEndpointBuilder.imageUrl(URN, ApiImageSize.LARGE)).thenReturn(imageUrl);

        imageOperations.displayInAdapterView(URN, ApiImageSize.LARGE, imageView);
        verify(imageLoader).displayImage(
                eq(imageUrl), any(ImageAware.class), any(DisplayImageOptions.class), any(SimpleImageLoadingListener.class));
    }

    @Test
    public void displayImageInAdapterViewShouldWrapAndForwardTheGivenImageView() {
        imageOperations.displayInAdapterView(URN, ApiImageSize.LARGE, imageView);

        verify(imageLoader).displayImage(
                anyString(), imageViewAwareCaptor.capture(), any(DisplayImageOptions.class), any(SimpleImageLoadingListener.class));
        expect(imageViewAwareCaptor.getValue().getWrappedView()).toBe(imageView);
    }

    @Test
    public void displayImageInAdapterViewShouldShouldUseCorrectDisplayOptions() {
        imageOperations.displayInAdapterView(URN, ApiImageSize.LARGE, imageView);

        verify(imageLoader).displayImage(anyString(), any(ImageAware.class), displayOptionsCaptor.capture(), any(SimpleImageLoadingListener.class));
        expect(displayOptionsCaptor.getValue().isResetViewBeforeLoading()).toBeTrue();
        expect(displayOptionsCaptor.getValue().getDisplayer()).toBeInstanceOf(PlaceholderTransitionDisplayer.class);
        verifyFullCacheOptions();
    }

    @Test
    public void displayInFullDialogViewShouldLoadImageFromMobileImageResolver() {
        final String imageUrl = "http://api.soundcloud.com/app/mobileapps/images/soundcloud:tracks:1/t500x500";
        when(imageEndpointBuilder.imageUrl(URN, ApiImageSize.T500)).thenReturn(imageUrl);

        imageOperations.displayInFullDialogView(URN, ApiImageSize.T500, imageView, imageListener);

        verify(imageLoader).displayImage(
                eq(imageUrl), any(ImageAware.class), any(DisplayImageOptions.class), any(ImageListenerUILAdapter.class));
    }

    @Test
    public void displayInFullDialogViewShouldWrapAndForwardTheGivenImageView() {
        imageOperations.displayInFullDialogView(URN, ApiImageSize.T500, imageView, imageListener);

        verify(imageLoader).displayImage(
                anyString(), imageViewAwareCaptor.capture(), any(DisplayImageOptions.class), any(ImageListenerUILAdapter.class));
        expect(imageViewAwareCaptor.getValue().getWrappedView()).toBe(imageView);
    }

    @Test
    public void displayInFullDialogViewShouldUseCorrectDisplayOptions() {
        imageOperations.displayInFullDialogView(URN, ApiImageSize.T500, imageView, imageListener);

        verify(imageLoader).displayImage(
                anyString(), any(ImageAware.class), displayOptionsCaptor.capture(), any(ImageListenerUILAdapter.class));
        expect(displayOptionsCaptor.getValue().getDelayBeforeLoading()).toEqual(DELAY_BEFORE_LOADING);
        expect(displayOptionsCaptor.getValue().getDisplayer()).toBeInstanceOf(FadeInBitmapDisplayer.class);
        expect(displayOptionsCaptor.getValue().isCacheOnDisk()).toBeTrue();
    }

    @Test
    public void displayWithPlaceholderShouldLoadImageFromMobileApiAndPlaceholderOptions() throws ExecutionException {
        final String imageUrl = RESOLVER_URL_LARGE;
        when(imageEndpointBuilder.imageUrl(URN, ApiImageSize.LARGE)).thenReturn(imageUrl);
        when(placeholderCache.get(anyString(), any(Callable.class))).thenReturn(transitionDrawable);

        imageOperations.displayWithPlaceholder(URN, ApiImageSize.LARGE, imageView);

        verify(imageLoader).displayImage(eq(imageUrl), imageViewAwareCaptor.capture(), displayOptionsCaptor.capture(), any(SimpleImageLoadingListener.class));
        expect(imageViewAwareCaptor.getValue().getWrappedView()).toBe(imageView);
        verifyFallbackDrawableOptions(RES_ID);
        verifyFullCacheOptions();
    }

    @Test
    public void displayInPlayerShouldLoadImageFromMobileApiAndPlaceholderOptions() throws ExecutionException {
        final String imageUrl = RESOLVER_URL_LARGE;
        when(imageEndpointBuilder.imageUrl(URN, ApiImageSize.LARGE)).thenReturn(imageUrl);
        when(placeholderCache.get(anyString(), any(Callable.class))).thenReturn(transitionDrawable);

        Bitmap bitmap = Bitmap.createBitmap(0,0, Bitmap.Config.RGB_565);
        imageOperations.displayInPlayer(URN, ApiImageSize.LARGE, imageView, bitmap, true);

        verify(imageLoader).displayImage(eq(imageUrl), imageViewAwareCaptor.capture(), displayOptionsCaptor.capture(), any(SimpleImageLoadingListener.class));
        verifyFullCacheOptions();
        expect(imageViewAwareCaptor.getValue().getWrappedView()).toBe(imageView);
        expect(displayOptionsCaptor.getValue().getDelayBeforeLoading()).toEqual(0);
        expect(((ShadowBitmapDrawable) shadowOf(displayOptionsCaptor.getValue().getImageForEmptyUri(resources))).getBitmap()).toBe(bitmap);
    }

    @Test
    public void displayInPlayerShouldDelayLoadingIfHighPriorityFlagIsNotSet() throws ExecutionException {
        final String imageUrl = RESOLVER_URL_LARGE;
        when(imageEndpointBuilder.imageUrl(URN, ApiImageSize.LARGE)).thenReturn(imageUrl);
        when(placeholderCache.get(anyString(), any(Callable.class))).thenReturn(transitionDrawable);

        Bitmap bitmap = Bitmap.createBitmap(0,0, Bitmap.Config.RGB_565);
        imageOperations.displayInPlayer(URN, ApiImageSize.LARGE, imageView, bitmap, false);

        verify(imageLoader).displayImage(eq(imageUrl), imageViewAwareCaptor.capture(), displayOptionsCaptor.capture(), any(SimpleImageLoadingListener.class));
        expect(displayOptionsCaptor.getValue().getDelayBeforeLoading()).toEqual(DELAY_BEFORE_LOADING);
    }

    @Test
    public void displayAdInPlayerShouldNotCacheImageToDisk() {
        imageOperations.displayAdInPlayer(Uri.parse(URL), imageView, transitionDrawable);

        verify(imageLoader).displayImage(eq(URL), imageViewAwareCaptor.capture(), displayOptionsCaptor.capture());
        expect(displayOptionsCaptor.getValue().isCacheOnDisk()).toBeFalse();
        expect(displayOptionsCaptor.getValue().isCacheInMemory()).toBeTrue();
        expect(imageViewAwareCaptor.getValue().getWrappedView()).toBe(imageView);
        verifyFallbackDrawableOptions(RES_ID);
    }

    @Test
    public void displayLeaveBehindDoesNotCacheAndHasNoPlaceholder() {
        imageOperations.displayLeaveBehind(Uri.parse(URL), imageView, imageListener);

        verify(imageLoader).displayImage(eq(URL), imageViewAwareCaptor.capture(), displayOptionsCaptor.capture(), any(SimpleImageLoadingListener.class));
        expect(displayOptionsCaptor.getValue().isCacheOnDisk()).toBeFalse();
        expect(displayOptionsCaptor.getValue().isCacheInMemory()).toBeTrue();
        expect(displayOptionsCaptor.getValue().shouldShowImageOnLoading()).toBeFalse();
        expect(displayOptionsCaptor.getValue().shouldShowImageOnFail()).toBeFalse();
        expect(displayOptionsCaptor.getValue().shouldShowImageForEmptyUri()).toBeFalse();
        expect(imageViewAwareCaptor.getValue().getWrappedView()).toBe(imageView);
    }

    @Test
    public void displayImageInAdapterViewShouldUsePlaceholderFromCache() throws ExecutionException {
        when(imageView.getLayoutParams()).thenReturn(new ViewGroup.LayoutParams(100, 100));
        when(placeholderCache.get(eq("soundcloud:tracks:1_100_100"), any(Callable.class))).thenReturn(transitionDrawable);
        imageOperations.displayInAdapterView(URN, ApiImageSize.LARGE, imageView);

        verify(imageLoader).displayImage(eq(RESOLVER_URL_LARGE), any(ImageAware.class), displayOptionsCaptor.capture(), any(ImageLoadingListener.class));
        expect(displayOptionsCaptor.getValue().getImageOnLoading(Robolectric.application.getResources())).toBe(transitionDrawable);
        expect(displayOptionsCaptor.getValue().getImageOnFail(Robolectric.application.getResources())).toBe(transitionDrawable);
        expect(displayOptionsCaptor.getValue().getImageForEmptyUri(Robolectric.application.getResources())).toBe(transitionDrawable);
    }

    @Test
    public void displayWithPlaceholderShouldUsePlaceholderFromCache() throws ExecutionException {
        when(imageView.getLayoutParams()).thenReturn(new ViewGroup.LayoutParams(100, 100));
        when(placeholderCache.get(eq("soundcloud:tracks:1_100_100"), any(Callable.class))).thenReturn(transitionDrawable);
        imageOperations.displayWithPlaceholder(URN, ApiImageSize.LARGE, imageView);

        verify(imageLoader).displayImage(eq(RESOLVER_URL_LARGE), any(ImageAware.class), displayOptionsCaptor.capture(), any(ImageLoadingListener.class));
        expect(displayOptionsCaptor.getValue().getImageOnLoading(Robolectric.application.getResources())).toBe(transitionDrawable);
        expect(displayOptionsCaptor.getValue().getImageOnFail(Robolectric.application.getResources())).toBe(transitionDrawable);
        expect(displayOptionsCaptor.getValue().getImageForEmptyUri(Robolectric.application.getResources())).toBe(transitionDrawable);
    }

    @Test
    public void artworkObservablePassesBitmapFromLoadCompleteToLoadingAdapter() {
        final Bitmap bitmap = Mockito.mock(Bitmap.class);
        ArgumentCaptor<ImageLoadingListener> captor = ArgumentCaptor.forClass(ImageLoadingListener.class);

        Observable<Bitmap> observable = imageOperations.artwork(URN, ApiImageSize.LARGE);
        TestSubscriber<Bitmap> subscriber = new TestSubscriber<Bitmap>();
        when(viewlessLoadingAdapterFactory.create(any(Subscriber.class), any(Bitmap.class))).thenReturn(fallbackBitmapLoadingAdapter);
        observable.subscribe(subscriber);

        verify(imageLoader).loadImage(eq(RESOLVER_URL_LARGE), captor.capture());
        captor.getValue().onLoadingComplete("asdf", imageView, bitmap);
        verify(fallbackBitmapLoadingAdapter).onLoadingComplete("asdf", imageView, bitmap);
    }

    @Test
    public void artworkObservablePassesLoadFailedToLoadingAdapter() {
        ArgumentCaptor<ImageLoadingListener> captor = ArgumentCaptor.forClass(ImageLoadingListener.class);

        Observable<Bitmap> observable = imageOperations.artwork(URN, ApiImageSize.LARGE);
        TestSubscriber<Bitmap> subscriber = new TestSubscriber<Bitmap>();
        when(viewlessLoadingAdapterFactory.create(any(Subscriber.class), any(Bitmap.class))).thenReturn(fallbackBitmapLoadingAdapter);
        observable.subscribe(subscriber);

        verify(imageLoader).loadImage(eq(RESOLVER_URL_LARGE), captor.capture());
        captor.getValue().onLoadingFailed("asdf", imageView, new FailReason(FailReason.FailType.DECODING_ERROR, new Exception("Decoding error")));
        verify(fallbackBitmapLoadingAdapter).onLoadingFailed("asdf", imageView, "Decoding error");
    }

    @Test
    public void blurredPlayerArtworkReturnsBlurredImageFromCache() throws Exception {
        final TestSubscriber<Bitmap> subscriber = new TestSubscriber<Bitmap>();
        Bitmap blurredBitmap = Bitmap.createBitmap(0, 1, Bitmap.Config.RGB_565);

        when(blurCache.getIfPresent(URN)).thenReturn(blurredBitmap);

        imageOperations.blurredPlayerArtwork(Robolectric.application.getResources(), URN,
                Schedulers.immediate(), Schedulers.immediate()).subscribe(subscriber);

        expect(subscriber.getOnNextEvents()).toContainExactly(blurredBitmap);
    }

    @Test
    public void blurredPlayerArtworkCreatesBlurredImageFromCache() throws Exception {
        final TestSubscriber<Bitmap> subscriber = new TestSubscriber<Bitmap>();
        Bitmap cachedBitmaop = Bitmap.createBitmap(1,0, Bitmap.Config.RGB_565);
        Bitmap blurredBitmap = Bitmap.createBitmap(0,1, Bitmap.Config.RGB_565);

        when(imageEndpointBuilder.imageUrl(URN, ApiImageSize.BADGE)).thenReturn(RESOLVER_URL_LARGE);
        when(memoryCache.get(anyString())).thenReturn(cachedBitmaop);
        when(imageProcessor.blurBitmap(cachedBitmaop)).thenReturn(blurredBitmap);

        imageOperations.blurredPlayerArtwork(Robolectric.application.getResources(), URN,
                Schedulers.immediate(), Schedulers.immediate()).subscribe(subscriber);

        expect(subscriber.getOnNextEvents()).toContainExactly(blurredBitmap);
    }

    @Test
    public void blurredPlayerArtworkCachesBlurredImage() throws Exception {
        final TestSubscriber<Bitmap> subscriber = new TestSubscriber<Bitmap>();
        Bitmap cachedBitmaop = Bitmap.createBitmap(1,0, Bitmap.Config.RGB_565);
        Bitmap blurredBitmap = Bitmap.createBitmap(0,1, Bitmap.Config.RGB_565);

        when(imageEndpointBuilder.imageUrl(URN, ApiImageSize.BADGE)).thenReturn(RESOLVER_URL_LARGE);
        when(memoryCache.get(anyString())).thenReturn(cachedBitmaop);
        when(imageProcessor.blurBitmap(cachedBitmaop)).thenReturn(blurredBitmap);

        imageOperations.blurredPlayerArtwork(Robolectric.application.getResources(), URN,
                Schedulers.immediate(), Schedulers.immediate()).subscribe(subscriber);

        verify(blurCache).put(URN, blurredBitmap);
    }

    private void verifyFullCacheOptions() {
        expect(displayOptionsCaptor.getValue().isCacheOnDisk()).toBeTrue();
        expect(displayOptionsCaptor.getValue().isCacheInMemory()).toBeTrue();
    }

    private void verifyFallbackDrawableOptions(int fallbackImageResId) {
        when(resources.getDrawable(fallbackImageResId)).thenReturn(transitionDrawable);
        expect(displayOptionsCaptor.getValue().getImageForEmptyUri(resources)).toBe(transitionDrawable);
        expect(displayOptionsCaptor.getValue().getImageOnFail(resources)).toBe(transitionDrawable);
        expect(displayOptionsCaptor.getValue().getImageOnLoading(resources)).toBe(transitionDrawable);
    }
}
