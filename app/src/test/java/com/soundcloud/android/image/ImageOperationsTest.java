package com.soundcloud.android.image;

import static com.soundcloud.android.image.ImageOptionsFactory.DELAY_BEFORE_LOADING;
import static com.soundcloud.android.image.ImageOptionsFactory.PlaceholderTransitionDisplayer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nostra13.universalimageloader.cache.disc.DiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.MemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.nostra13.universalimageloader.core.imageaware.NonViewAware;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiUrlBuilder;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.DisplayMetricsStub;
import com.soundcloud.android.utils.cache.Cache;
import com.soundcloud.android.utils.cache.Cache.ValueProvider;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;
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

import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class ImageOperationsTest extends AndroidUnitTest {

    private static final int RES_ID = 123;
    private static final String URL = "https://i1.sndcdn.com/artworks-000058493054-vcrifw-t500x500.jpg?b09b136";
    private static final String RESOLVER_URL = "https://api-mobile.soundcloud.com/images/soundcloud:tracks:1/large";
    private static final String CDN_URL = "https://i1.sndcdn.com/artworks-000004997420-uc1lir-t120x120.jpg";
    private static final Urn URN = new Urn("soundcloud:tracks:1");
    private static final Optional<String> ARTWORK_TEMPLATE_URL = Optional.of(
            "https://i1.sndcdn.com/artworks-000004997420-uc1lir-{size}.jpg");

    private ImageOperations imageOperations;
    private Scheduler scheduler;
    private DisplayMetrics displayMetrics = new DisplayMetricsStub();
    private ImageResource imageResource = new ImageResource() {
        @Override
        public Urn getUrn() {
            return URN;
        }

        @Override
        public Optional<String> getImageUrlTemplate() {
            return ARTWORK_TEMPLATE_URL;
        }
    };

    @Mock ImageLoader imageLoader;
    @Mock ApiUrlBuilder apiUrlBuilder;
    @Mock PlaceholderGenerator placeholderGenerator;
    @Mock CircularPlaceholderGenerator circlePlaceholderGenerator;
    @Mock DiskCache diskCache;
    @Mock MemoryCache memoryCache;
    @Mock ImageListener imageListener;
    @Mock ImageView imageView;
    @Mock Resources resources;
    @Mock TransitionDrawable transitionDrawable;
    @Mock GradientDrawable gradientDrawable;
    @Mock View parentView;
    @Mock FailReason failReason;
    @Mock Cache<String, TransitionDrawable> placeholderCache;
    @Mock Cache<Urn, Bitmap> blurCache;
    @Mock FallbackBitmapLoadingAdapter.Factory viewlessLoadingAdapterFactory;
    @Mock FallbackBitmapLoadingAdapter fallbackBitmapLoadingAdapter;
    @Mock BitmapLoadingAdapter.Factory bitmapLoadingAdapterFactory;
    @Mock BitmapLoadingAdapter bitmapLoadingAdapter;
    @Mock FileNameGenerator fileNameGenerator;
    @Mock ImageProcessor imageProcessor;
    @Mock Configuration configuration;

    @Captor ArgumentCaptor<ImageViewAware> imageViewAwareCaptor;
    @Captor ArgumentCaptor<DisplayImageOptions> displayOptionsCaptor;
    @Captor ArgumentCaptor<ImageLoadingListener> imageLoadingListenerCaptor;

    @Before
    public void setUp() throws Exception {
        imageOperations = new ImageOperations(
                imageLoader,
                new ImageUrlBuilder(apiUrlBuilder),
                placeholderGenerator,
                circlePlaceholderGenerator,
                viewlessLoadingAdapterFactory,
                bitmapLoadingAdapterFactory,
                imageProcessor,
                placeholderCache,
                blurCache,
                fileNameGenerator);
        scheduler = Schedulers.immediate();

        when(imageLoader.getDiskCache()).thenReturn(diskCache);
        when(imageLoader.getMemoryCache()).thenReturn(memoryCache);
        when(placeholderGenerator.generateTransitionDrawable(any(String.class))).thenReturn(transitionDrawable);
        when(placeholderGenerator.generateDrawable(any(String.class))).thenReturn(gradientDrawable);
        when(apiUrlBuilder.from(eq(ApiEndpoints.IMAGES), eq(URN), anyString())).thenReturn(apiUrlBuilder);
        when(apiUrlBuilder.build()).thenReturn(RESOLVER_URL);
        when(resources.getDisplayMetrics()).thenReturn(displayMetrics);
    }

    @Test
    public void NotFoundExceptionDuringAdapterViewLoadMakesNextLoadToPassNullPath() throws ExecutionException {
        when(failReason.getCause()).thenReturn(new FileNotFoundException());

        // 1st load
        imageOperations.displayInAdapterView(URN, ApiImageSize.T120, imageView);
        InOrder inOrder = Mockito.inOrder(imageLoader);
        inOrder.verify(imageLoader)
               .displayImage(eq(RESOLVER_URL), any(ImageViewAware.class), any(DisplayImageOptions.class),
                             imageLoadingListenerCaptor.capture());
        imageLoadingListenerCaptor.getValue().onLoadingFailed(RESOLVER_URL, imageView, failReason);

        // 2nd load
        imageOperations.displayInAdapterView(URN, ApiImageSize.T120, imageView);
        inOrder.verify(imageLoader).displayImage((String) isNull(), any(ImageViewAware.class),
                                                 any(DisplayImageOptions.class), any(SimpleImageLoadingListener.class));
        when(placeholderCache.get(anyString(), any(ValueProvider.class))).thenReturn(transitionDrawable);
    }

    @Test
    public void IOExceptionDuringAdapterViewLoadAllowsRetryWithUrl() {
        when(failReason.getCause()).thenReturn(new IOException());

        // 1st load
        imageOperations.displayInAdapterView(URN, ApiImageSize.T120, imageView);
        InOrder inOrder = Mockito.inOrder(imageLoader);
        inOrder.verify(imageLoader).displayImage(eq(RESOLVER_URL), any(ImageViewAware.class),
                                                 any(DisplayImageOptions.class), imageLoadingListenerCaptor.capture());
        imageLoadingListenerCaptor.getValue().onLoadingFailed(RESOLVER_URL, imageView, failReason);

        // 2nd load
        imageOperations.displayInAdapterView(URN, ApiImageSize.T120, imageView);
        inOrder.verify(imageLoader).displayImage(eq(RESOLVER_URL), any(ImageViewAware.class),
                                                 any(DisplayImageOptions.class), any(SimpleImageLoadingListener.class));
    }

    @Test
    public void NotFoundExceptionDuringPlaceholderLoadMakesNextLoadToPassNullPath() {
        when(failReason.getCause()).thenReturn(new FileNotFoundException());

        // 1st load
        imageOperations.displayWithPlaceholder(URN, ApiImageSize.T120, imageView);
        InOrder inOrder = Mockito.inOrder(imageLoader);
        inOrder.verify(imageLoader).displayImage(eq(RESOLVER_URL), any(ImageViewAware.class),
                                                 any(DisplayImageOptions.class), imageLoadingListenerCaptor.capture());
        imageLoadingListenerCaptor.getValue().onLoadingFailed(RESOLVER_URL, imageView, failReason);

        // 2nd load
        imageOperations.displayWithPlaceholder(URN, ApiImageSize.T120, imageView);
        inOrder.verify(imageLoader).displayImage((String) isNull(), any(ImageViewAware.class),
                                                 any(DisplayImageOptions.class), any(SimpleImageLoadingListener.class));
    }

    @Test
    public void displayImageInAdapterViewShouldRequestImagesThroughMobileImageResolver() {
        imageOperations.displayInAdapterView(URN, ApiImageSize.T120, imageView);
        verify(imageLoader).displayImage(
                eq(RESOLVER_URL), any(ImageAware.class),
                any(DisplayImageOptions.class), any(SimpleImageLoadingListener.class));
    }

    @Test
    public void displayImageInAdapterViewShouldWrapAndForwardTheGivenImageView() {
        imageOperations.displayInAdapterView(URN, ApiImageSize.T120, imageView);

        verify(imageLoader).displayImage(
                anyString(), imageViewAwareCaptor.capture(),
                any(DisplayImageOptions.class), any(SimpleImageLoadingListener.class));
        assertThat(imageViewAwareCaptor.getValue().getWrappedView()).isEqualTo(imageView);
    }

    @Test
    public void displayImageInAdapterViewShouldShouldUseCorrectDisplayOptions() {
        imageOperations.displayInAdapterView(URN, ApiImageSize.T120, imageView);

        verify(imageLoader).displayImage(anyString(), any(ImageAware.class),
                                         displayOptionsCaptor.capture(), any(SimpleImageLoadingListener.class));

        assertThat(displayOptionsCaptor.getValue().isResetViewBeforeLoading()).isTrue();
        assertThat(displayOptionsCaptor.getValue().getDisplayer()).isInstanceOf(PlaceholderTransitionDisplayer.class);
        verifyFullCacheOptions();
    }

    @Test
    public void displayInFullDialogViewShouldLoadImageFromMobileImageResolver() {
        imageOperations.displayInFullDialogView(URN, ApiImageSize.T500, imageView, imageListener);

        verify(imageLoader).displayImage(
                eq(RESOLVER_URL), any(ImageAware.class),
                any(DisplayImageOptions.class), any(ImageListenerUILAdapter.class));
    }

    @Test
    public void displayInFullDialogViewShouldWrapAndForwardTheGivenImageView() {
        imageOperations.displayInFullDialogView(URN, ApiImageSize.T500, imageView, imageListener);

        verify(imageLoader).displayImage(
                anyString(), imageViewAwareCaptor.capture(),
                any(DisplayImageOptions.class), any(ImageListenerUILAdapter.class));
        assertThat(imageViewAwareCaptor.getValue().getWrappedView()).isEqualTo(imageView);
    }

    @Test
    public void displayInFullDialogViewShouldUseCorrectDisplayOptions() {
        imageOperations.displayInFullDialogView(URN, ApiImageSize.T500, imageView, imageListener);

        verify(imageLoader).displayImage(
                anyString(), any(ImageAware.class), displayOptionsCaptor.capture(), any(ImageListenerUILAdapter.class));
        assertThat(displayOptionsCaptor.getValue().getDelayBeforeLoading()).isEqualTo(DELAY_BEFORE_LOADING);
        assertThat(displayOptionsCaptor.getValue().getDisplayer()).isInstanceOf(FadeInBitmapDisplayer.class);
        assertThat(displayOptionsCaptor.getValue().isCacheOnDisk()).isTrue();
    }

    @Test
    public void displayWithPlaceholderShouldLoadImageFromMobileApiAndPlaceholderOptions() throws ExecutionException {
        final String imageUrl = RESOLVER_URL;
        when(placeholderCache.get(anyString(), any(ValueProvider.class))).thenReturn(transitionDrawable);

        imageOperations.displayWithPlaceholder(URN, ApiImageSize.T120, imageView);

        verify(imageLoader).displayImage(eq(imageUrl), imageViewAwareCaptor.capture(),
                                         displayOptionsCaptor.capture(), any(SimpleImageLoadingListener.class));

        assertThat(imageViewAwareCaptor.getValue().getWrappedView()).isEqualTo(imageView);
        verifyFallbackDrawableOptions(RES_ID);
        verifyFullCacheOptions();
    }

    @Test
    public void displayInPlayerShouldLoadImageFromMobileApiAndPlaceholderOptions() throws ExecutionException {
        when(placeholderCache.get(anyString(), any(ValueProvider.class))).thenReturn(transitionDrawable);

        Bitmap bitmap = Bitmap.createBitmap(1, 2, Bitmap.Config.RGB_565);
        imageOperations.displayInPlayer(imageResource, ApiImageSize.T120, imageView, bitmap, true);

        verify(imageLoader).displayImage(eq(CDN_URL), imageViewAwareCaptor.capture(),
                                         displayOptionsCaptor.capture(), any(SimpleImageLoadingListener.class));

        verifyFullCacheOptions();
        assertThat(imageViewAwareCaptor.getValue().getWrappedView()).isEqualTo(imageView);
        assertThat(displayOptionsCaptor.getValue().getDelayBeforeLoading()).isEqualTo(0);
        assertThat(((BitmapDrawable) displayOptionsCaptor.getValue()
                                                         .getImageForEmptyUri(resources)).getBitmap()).isSameAs(bitmap);
    }

    @Test
    public void displayInPlayerShouldDelayLoadingIfHighPriorityFlagIsNotSet() throws ExecutionException {
        when(placeholderCache.get(anyString(), any(ValueProvider.class))).thenReturn(transitionDrawable);

        Bitmap bitmap = Bitmap.createBitmap(1, 2, Bitmap.Config.RGB_565);
        imageOperations.displayInPlayer(imageResource, ApiImageSize.T120, imageView, bitmap, false);

        verify(imageLoader).displayImage(eq(CDN_URL),
                                         imageViewAwareCaptor.capture(),
                                         displayOptionsCaptor.capture(),
                                         any(SimpleImageLoadingListener.class));
        assertThat(displayOptionsCaptor.getValue().getDelayBeforeLoading()).isEqualTo(DELAY_BEFORE_LOADING);
    }

    @Test
    public void precacheTrackArtworkCachesImageOnDisc() {
        imageOperations.precacheArtwork(imageResource, ApiImageSize.T120);

        verify(imageLoader).loadImage(eq(CDN_URL), displayOptionsCaptor.capture(), isNull(ImageLoadingListener.class));
        assertThat(displayOptionsCaptor.getValue().isCacheOnDisk()).isTrue();
        assertThat(displayOptionsCaptor.getValue().isCacheInMemory()).isFalse();
    }

    @Test
    public void displayLeaveBehindDoesNotCacheAndHasNoPlaceholder() {
        imageOperations.displayLeaveBehind(Uri.parse(URL), imageView, imageListener);

        verify(imageLoader).displayImage(eq(URL), imageViewAwareCaptor.capture(),
                                         displayOptionsCaptor.capture(), any(SimpleImageLoadingListener.class));

        assertThat(displayOptionsCaptor.getValue().isCacheOnDisk()).isFalse();
        assertThat(displayOptionsCaptor.getValue().isCacheInMemory()).isTrue();
        assertThat(displayOptionsCaptor.getValue().shouldShowImageOnLoading()).isFalse();
        assertThat(displayOptionsCaptor.getValue().shouldShowImageOnFail()).isFalse();
        assertThat(displayOptionsCaptor.getValue().shouldShowImageForEmptyUri()).isFalse();
        assertThat(imageViewAwareCaptor.getValue().getWrappedView()).isSameAs(imageView);
    }

    @Test
    public void displayImageInAdapterViewShouldUsePlaceholderFromCache() throws ExecutionException {
        when(imageView.getLayoutParams()).thenReturn(new ViewGroup.LayoutParams(100, 100));
        when(placeholderCache.get(eq("soundcloud:tracks:1_100_100"), any(ValueProvider.class))).thenReturn(
                transitionDrawable);
        imageOperations.displayInAdapterView(URN, ApiImageSize.T120, imageView);

        verify(imageLoader).displayImage(eq(RESOLVER_URL), any(ImageAware.class),
                                         displayOptionsCaptor.capture(), any(ImageLoadingListener.class));
        assertThat(displayOptionsCaptor.getValue().getImageOnLoading(resources())).isSameAs(transitionDrawable);
        assertThat(displayOptionsCaptor.getValue().getImageOnFail(resources())).isSameAs(transitionDrawable);
        assertThat(displayOptionsCaptor.getValue().getImageForEmptyUri(resources())).isSameAs(transitionDrawable);
    }

    @Test
    public void displayWithPlaceholderShouldUsePlaceholderFromCache() throws ExecutionException {
        when(imageView.getLayoutParams()).thenReturn(new ViewGroup.LayoutParams(100, 100));
        when(placeholderCache.get(eq("soundcloud:tracks:1_100_100"), any(ValueProvider.class))).thenReturn(
                transitionDrawable);
        imageOperations.displayWithPlaceholder(URN, ApiImageSize.T120, imageView);

        verify(imageLoader).displayImage(eq(RESOLVER_URL),
                                         any(ImageAware.class),
                                         displayOptionsCaptor.capture(),
                                         any(ImageLoadingListener.class));
        assertThat(displayOptionsCaptor.getValue().getImageOnLoading(resources())).isSameAs(transitionDrawable);
        assertThat(displayOptionsCaptor.getValue().getImageOnFail(resources())).isSameAs(transitionDrawable);
        assertThat(displayOptionsCaptor.getValue().getImageForEmptyUri(resources())).isSameAs(transitionDrawable);
    }

    @Test
    public void adImageObservablePassesBitmapFromLoadCompleteToLoadingAdapter() {
        final Bitmap bitmap = Mockito.mock(Bitmap.class);
        ArgumentCaptor<ImageLoadingListener> captor = ArgumentCaptor.forClass(ImageLoadingListener.class);

        Observable<Bitmap> observable = imageOperations.adImage(Uri.parse(URL));
        TestSubscriber<Bitmap> subscriber = new TestSubscriber<>();
        when(bitmapLoadingAdapterFactory.create(any(Subscriber.class))).thenReturn(bitmapLoadingAdapter);
        observable.subscribe(subscriber);

        verify(imageLoader).displayImage(eq(URL),
                                         any(NonViewAware.class),
                                         displayOptionsCaptor.capture(),
                                         captor.capture());
        assertThat(displayOptionsCaptor.getValue().isCacheOnDisk()).isFalse();
        assertThat(displayOptionsCaptor.getValue().isCacheInMemory()).isTrue();
        captor.getValue().onLoadingComplete("ad-image-url", imageView, bitmap);
        verify(bitmapLoadingAdapter).onLoadingComplete(eq("ad-image-url"), any(ImageView.class), eq(bitmap));
    }

    @Test
    public void adImageObservablePassesLoadFailedToLoadingAdapter() {
        ArgumentCaptor<ImageLoadingListener> captor = ArgumentCaptor.forClass(ImageLoadingListener.class);

        Observable<Bitmap> observable = imageOperations.adImage(Uri.parse(URL));
        TestSubscriber<Bitmap> subscriber = new TestSubscriber<>();
        when(bitmapLoadingAdapterFactory.create(any(Subscriber.class))).thenReturn(bitmapLoadingAdapter);
        observable.subscribe(subscriber);

        verify(imageLoader).displayImage(eq(URL),
                                         any(NonViewAware.class),
                                         displayOptionsCaptor.capture(),
                                         captor.capture());
        assertThat(displayOptionsCaptor.getValue().isCacheOnDisk()).isFalse();
        assertThat(displayOptionsCaptor.getValue().isCacheInMemory()).isTrue();
        captor.getValue().onLoadingFailed("ad-image-url", imageView,
                                          new FailReason(FailReason.FailType.DECODING_ERROR,
                                                         new Exception("Decoding error")));
        verify(bitmapLoadingAdapter).onLoadingFailed("ad-image-url", imageView, "Decoding error");
    }

    @Test
    public void artworkObservablePassesBitmapFromLoadCompleteToLoadingAdapter() {
        final Bitmap bitmap = Mockito.mock(Bitmap.class);
        ArgumentCaptor<ImageLoadingListener> captor = ArgumentCaptor.forClass(ImageLoadingListener.class);

        Observable<Bitmap> observable = imageOperations.artwork(imageResource, ApiImageSize.T120);
        TestSubscriber<Bitmap> subscriber = new TestSubscriber<>();
        when(viewlessLoadingAdapterFactory.create(any(Subscriber.class), any(Bitmap.class)))
                .thenReturn(fallbackBitmapLoadingAdapter);
        observable.subscribe(subscriber);

        verify(imageLoader).loadImage(eq(CDN_URL), captor.capture());
        captor.getValue().onLoadingComplete("asdf", imageView, bitmap);
        verify(fallbackBitmapLoadingAdapter).onLoadingComplete("asdf", imageView, bitmap);
    }

    @Test
    public void artworkObservablePassesLoadFailedToLoadingAdapter() {
        ArgumentCaptor<ImageLoadingListener> captor = ArgumentCaptor.forClass(ImageLoadingListener.class);

        Observable<Bitmap> observable = imageOperations.artwork(imageResource, ApiImageSize.T120);
        TestSubscriber<Bitmap> subscriber = new TestSubscriber<>();
        when(viewlessLoadingAdapterFactory.create(any(Subscriber.class), any(Bitmap.class)))
                .thenReturn(fallbackBitmapLoadingAdapter);
        observable.subscribe(subscriber);

        verify(imageLoader).loadImage(eq(CDN_URL), captor.capture());
        captor.getValue().onLoadingFailed("asdf", imageView,
                                          new FailReason(FailReason.FailType.DECODING_ERROR,
                                                         new Exception("Decoding error")));
        verify(fallbackBitmapLoadingAdapter).onLoadingFailed("asdf", imageView, "Decoding error");
    }

    @Test
    public void blurredPlayerArtworkReturnsBlurredImageFromCache() throws Exception {
        final TestSubscriber<Bitmap> subscriber = new TestSubscriber<>();
        Bitmap blurredBitmap = Bitmap.createBitmap(2, 1, Bitmap.Config.RGB_565);

        when(blurCache.get(URN)).thenReturn(blurredBitmap);

        imageOperations.blurredPlayerArtwork(resources(), imageResource,
                                             scheduler, scheduler).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).containsExactly(blurredBitmap);
    }

    @Test
    public void blurredPlayerArtworkCreatesBlurredImageFromCache() throws Exception {
        final TestSubscriber<Bitmap> subscriber = new TestSubscriber<>();
        Bitmap cachedBitmap = Bitmap.createBitmap(1, 2, Bitmap.Config.RGB_565);
        Bitmap blurredBitmap = Bitmap.createBitmap(2, 1, Bitmap.Config.RGB_565);

        when(memoryCache.get(anyString())).thenReturn(cachedBitmap);
        when(imageProcessor.blurBitmap(cachedBitmap)).thenReturn(blurredBitmap);

        imageOperations.blurredPlayerArtwork(resources(), imageResource,
                                             scheduler, scheduler).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).containsExactly(blurredBitmap);
    }

    @Test
    public void blurredPlayerArtworkCachesBlurredImage() throws Exception {
        final TestSubscriber<Bitmap> subscriber = new TestSubscriber<>();
        Bitmap cachedBitmaop = Bitmap.createBitmap(1, 2, Bitmap.Config.RGB_565);
        Bitmap blurredBitmap = Bitmap.createBitmap(2, 1, Bitmap.Config.RGB_565);

        when(memoryCache.get(anyString())).thenReturn(cachedBitmaop);
        when(imageProcessor.blurBitmap(cachedBitmaop)).thenReturn(blurredBitmap);

        imageOperations.blurredPlayerArtwork(resources(), imageResource,
                                             scheduler, scheduler).subscribe(subscriber);

        verify(blurCache).put(URN, blurredBitmap);
    }

    @Test
    public void buildUrlIfNotPreviouslyMissingReturnsFullSizeUrl() throws Exception {
        assertThat(imageOperations.getUrlForLargestImage(resources, URN)).isEqualTo(RESOLVER_URL);
    }

    private void verifyFullCacheOptions() {
        assertThat(displayOptionsCaptor.getValue().isCacheOnDisk()).isTrue();
        assertThat(displayOptionsCaptor.getValue().isCacheInMemory()).isTrue();
    }

    private void verifyFallbackDrawableOptions(int fallbackImageResId) {
        when(resources.getDrawable(fallbackImageResId)).thenReturn(transitionDrawable);
        assertThat(displayOptionsCaptor.getValue().getImageForEmptyUri(resources)).isEqualTo(transitionDrawable);
        assertThat(displayOptionsCaptor.getValue().getImageOnFail(resources)).isEqualTo(transitionDrawable);
        assertThat(displayOptionsCaptor.getValue().getImageOnLoading(resources)).isEqualTo(transitionDrawable);
    }
}
