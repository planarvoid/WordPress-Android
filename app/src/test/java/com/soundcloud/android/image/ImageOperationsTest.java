package com.soundcloud.android.image;

import static com.soundcloud.android.image.ImageOptionsFactory.DELAY_BEFORE_LOADING;
import static com.soundcloud.android.image.ImageOptionsFactory.PlaceholderTransitionDisplayer;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.nostra13.universalimageloader.cache.disc.DiskCache;
import com.nostra13.universalimageloader.cache.memory.MemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.nostra13.universalimageloader.core.imageaware.NonViewAware;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiUrlBuilder;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.android.utils.DisplayMetricsStub;
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
import rx.functions.Action1;
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

    @Mock private Action1<Bitmap> bitmapAction1;

    @Mock ImageLoader imageLoader;
    @Mock ApiUrlBuilder apiUrlBuilder;
    @Mock DiskCache diskCache;
    @Mock MemoryCache memoryCache;
    @Mock ImageListener imageListener;
    @Mock ImageView imageView;
    @Mock Resources resources;
    @Mock TransitionDrawable cachedTransitionDrawable;
    @Mock TransitionDrawable generatedTransitionDrawable;
    @Mock GradientDrawable gradientDrawable;
    @Mock View parentView;
    @Mock FailReason failReason;
    @Mock FallbackBitmapLoadingAdapter.Factory viewlessLoadingAdapterFactory;
    @Mock FallbackBitmapLoadingAdapter fallbackBitmapLoadingAdapter;
    @Mock BitmapLoadingAdapter.Factory bitmapLoadingAdapterFactory;
    @Mock BitmapLoadingAdapter bitmapLoadingAdapter;
    @Mock ImageProcessor imageProcessor;
    @Mock Configuration configuration;
    @Mock DeviceHelper deviceHelper;
    @Mock ImageCache imageCache;
    @Mock PlaceholderGenerator placeholderGenerator;
    @Mock CircularPlaceholderGenerator circularPlaceholderGenerator;

    @Captor ArgumentCaptor<ImageViewAware> imageViewAwareCaptor;
    @Captor ArgumentCaptor<DisplayImageOptions> displayOptionsCaptor;
    @Captor ArgumentCaptor<ImageLoadingListener> imageLoadingListenerCaptor;
    @Captor ArgumentCaptor<ValueProvider<String, TransitionDrawable>> cacheValueProviderCaptor;

    @Before
    public void setUp() throws Exception {
        imageOperations = new ImageOperations(
                imageLoader,
                new ImageUrlBuilder(apiUrlBuilder),
                viewlessLoadingAdapterFactory,
                bitmapLoadingAdapterFactory,
                imageProcessor,
                deviceHelper,
                placeholderGenerator,
                circularPlaceholderGenerator,
                imageCache);
        scheduler = Schedulers.immediate();

        when(imageLoader.getDiskCache()).thenReturn(diskCache);
        when(imageLoader.getMemoryCache()).thenReturn(memoryCache);
        when(placeholderGenerator.generateDrawable(any())).thenReturn(gradientDrawable);
        when(circularPlaceholderGenerator.generateDrawable(any())).thenReturn(gradientDrawable);
        when(apiUrlBuilder.from(eq(ApiEndpoints.IMAGES), eq(URN), anyString())).thenReturn(apiUrlBuilder);
        when(apiUrlBuilder.build()).thenReturn(RESOLVER_URL);
        when(resources.getDisplayMetrics()).thenReturn(displayMetrics);

        mock(ImageLoader.class, withSettings().verboseLogging());
    }

    @Test
    public void NotFoundExceptionDuringAdapterViewLoadMakesNextLoadToPassNullPath() throws ExecutionException {
        when(failReason.getCause()).thenReturn(new FileNotFoundException());

        // 1st load
        imageOperations.displayInAdapterView(URN, Optional.absent(), ApiImageSize.T120, imageView, ImageOperations.DisplayType.DEFAULT);
        InOrder inOrder = Mockito.inOrder(imageLoader);
        inOrder.verify(imageLoader)
               .displayImage(eq(RESOLVER_URL), any(ImageViewAware.class), any(DisplayImageOptions.class),
                             imageLoadingListenerCaptor.capture());

        // trigger loading failed
        imageLoadingListenerCaptor.getValue().onLoadingFailed(RESOLVER_URL, imageView, failReason);

        // 2nd load
        imageOperations.displayInAdapterView(URN, Optional.absent(), ApiImageSize.T120, imageView, ImageOperations.DisplayType.DEFAULT);
        inOrder.verify(imageLoader).displayImage(isNull(), any(ImageViewAware.class),
                                                 any(DisplayImageOptions.class), any(FallbackImageListener.class));
    }

    @Test
    public void NotFoundExceptionDuringPlaceholderLoadMakesNextLoadToPassNullPath() {
        when(failReason.getCause()).thenReturn(new FileNotFoundException());

        // 1st load
        imageOperations.displayWithPlaceholder(URN, Optional.absent(), ApiImageSize.T120, imageView);
        InOrder inOrder = Mockito.inOrder(imageLoader);
        inOrder.verify(imageLoader).displayImage(eq(RESOLVER_URL), any(ImageViewAware.class),
                                                 any(DisplayImageOptions.class), imageLoadingListenerCaptor.capture());

        // trigger loading failed
        imageLoadingListenerCaptor.getValue().onLoadingFailed(RESOLVER_URL, imageView, failReason);

        // 2nd load
        imageOperations.displayWithPlaceholder(URN, Optional.absent(), ApiImageSize.T120, imageView);
        inOrder.verify(imageLoader).displayImage(isNull(), any(ImageViewAware.class),
                                                 any(DisplayImageOptions.class), any(FallbackImageListener.class));
    }

    @Test
    public void IOExceptionDuringAdapterViewLoadAllowsRetryWithUrl() {
        when(failReason.getCause()).thenReturn(new IOException());

        // 1st load
        imageOperations.displayInAdapterView(URN, Optional.absent(), ApiImageSize.T120, imageView, ImageOperations.DisplayType.DEFAULT);
        InOrder inOrder = Mockito.inOrder(imageLoader);
        inOrder.verify(imageLoader).displayImage(eq(RESOLVER_URL), any(ImageViewAware.class),
                                                 any(DisplayImageOptions.class), imageLoadingListenerCaptor.capture());

        // trigger loading failed
        imageLoadingListenerCaptor.getValue().onLoadingFailed(RESOLVER_URL, imageView, failReason);

        // 2nd load
        imageOperations.displayInAdapterView(URN, Optional.absent(), ApiImageSize.T120, imageView, ImageOperations.DisplayType.DEFAULT);
        inOrder.verify(imageLoader).displayImage(eq(RESOLVER_URL), any(ImageViewAware.class),
                                                 any(DisplayImageOptions.class), any(FallbackImageListener.class));
    }

    @Test
    public void displayImageInAdapterViewShouldRequestImagesThroughMobileImageResolver() {
        imageOperations.displayInAdapterView(URN, Optional.absent(), ApiImageSize.T120, imageView, ImageOperations.DisplayType.DEFAULT);
        verify(imageLoader).displayImage(
                eq(RESOLVER_URL), any(ImageAware.class),
                any(DisplayImageOptions.class), any(FallbackImageListener.class));
    }

    @Test
    public void displayImageInAdapterViewShouldWrapAndForwardTheGivenImageView() {
        imageOperations.displayInAdapterView(URN, Optional.absent(), ApiImageSize.T120, imageView, ImageOperations.DisplayType.DEFAULT);

        verify(imageLoader).displayImage(
                anyString(), imageViewAwareCaptor.capture(),
                any(DisplayImageOptions.class), any(FallbackImageListener.class));
        assertThat(imageViewAwareCaptor.getValue().getWrappedView()).isEqualTo(imageView);
    }

    @Test
    public void displayImageInAdapterViewShouldUseCorrectDisplayOptions() {
        imageOperations.displayInAdapterView(URN, Optional.absent(), ApiImageSize.T120, imageView, ImageOperations.DisplayType.DEFAULT);

        verify(imageLoader).displayImage(anyString(), any(ImageAware.class),
                                         displayOptionsCaptor.capture(), any(FallbackImageListener.class));

        assertThat(displayOptionsCaptor.getValue().isResetViewBeforeLoading()).isTrue();
        assertThat(displayOptionsCaptor.getValue().getDisplayer()).isInstanceOf(PlaceholderTransitionDisplayer.class);
        assertThat(displayOptionsCaptor.getValue().isCacheOnDisk()).isTrue();
        assertThat(displayOptionsCaptor.getValue().isCacheInMemory()).isTrue();
    }

    @Test
    public void displayInFullDialogViewShouldLoadImageFromMobileImageResolver() {
        imageOperations.displayInFullDialogView(SimpleImageResource.create(URN, Optional.absent()), ApiImageSize.T500, imageView, imageListener);

        verify(imageLoader).displayImage(
                eq(RESOLVER_URL), any(ImageAware.class),
                any(DisplayImageOptions.class), any(ImageListenerUILAdapter.class));
    }

    @Test
    public void displayInFullDialogViewShouldWrapAndForwardTheGivenImageView() {
        imageOperations.displayInFullDialogView(imageResource, ApiImageSize.T500, imageView, imageListener);

        verify(imageLoader).displayImage(
                anyString(), imageViewAwareCaptor.capture(),
                any(DisplayImageOptions.class), any(ImageListenerUILAdapter.class));
        assertThat(imageViewAwareCaptor.getValue().getWrappedView()).isEqualTo(imageView);
    }

    @Test
    public void displayInFullDialogViewShouldUseCorrectDisplayOptions() {
        imageOperations.displayInFullDialogView(imageResource, ApiImageSize.T500, imageView, imageListener);

        verify(imageLoader).displayImage(
                anyString(), any(ImageAware.class), displayOptionsCaptor.capture(), any(ImageListenerUILAdapter.class));
        assertThat(displayOptionsCaptor.getValue().getDelayBeforeLoading()).isEqualTo(DELAY_BEFORE_LOADING);
        assertThat(displayOptionsCaptor.getValue().getDisplayer()).isInstanceOf(FadeInBitmapDisplayer.class);
        assertThat(displayOptionsCaptor.getValue().isCacheOnDisk()).isTrue();
    }

    @Test
    public void displayWithPlaceholderShouldLoadImageWithMobileApiUrl() throws Exception {
        when(imageView.getLayoutParams()).thenReturn(new ViewGroup.LayoutParams(100, 100));
        when(imageCache.getPlaceholderDrawable(eq(RESOLVER_URL), anyInt(), anyInt(), any())).thenReturn(cachedTransitionDrawable);

        imageOperations.displayWithPlaceholder(URN, Optional.absent(), ApiImageSize.T120, imageView);

        verify(imageLoader).displayImage(eq(RESOLVER_URL),
                                         imageViewAwareCaptor.capture(),
                                         displayOptionsCaptor.capture(),
                                         any(FallbackImageListener.class));

        assertThat(imageViewAwareCaptor.getValue().getWrappedView()).isEqualTo(imageView);
        assertThat(displayOptionsCaptor.getValue().getImageOnLoading(resources())).isSameAs(cachedTransitionDrawable);
        assertThat(displayOptionsCaptor.getValue().getImageOnFail(resources())).isSameAs(cachedTransitionDrawable);
        assertThat(displayOptionsCaptor.getValue().getImageForEmptyUri(resources())).isSameAs(cachedTransitionDrawable);
        assertThat(displayOptionsCaptor.getValue().isCacheOnDisk()).isTrue();
        assertThat(displayOptionsCaptor.getValue().isCacheInMemory()).isTrue();
    }

    @Test
    public void displayInPlayerShouldLoadImageFromMobileApiAndPlaceholderOptions() throws ExecutionException {
        when(imageCache.getPlaceholderDrawable(anyString(), anyInt(), anyInt(), any(PlaceholderGenerator.class))).thenReturn(cachedTransitionDrawable);

        Bitmap bitmap = Bitmap.createBitmap(1, 2, Bitmap.Config.RGB_565);
        imageOperations.displayInPlayer(imageResource, ApiImageSize.T120, imageView, bitmap, true);

        verify(imageLoader).displayImage(eq(CDN_URL), imageViewAwareCaptor.capture(),
                                         displayOptionsCaptor.capture(), any(FallbackImageListener.class));

        assertThat(displayOptionsCaptor.getValue().isCacheOnDisk()).isTrue();
        assertThat(displayOptionsCaptor.getValue().isCacheInMemory()).isTrue();
        assertThat(imageViewAwareCaptor.getValue().getWrappedView()).isEqualTo(imageView);
        assertThat(displayOptionsCaptor.getValue().getDelayBeforeLoading()).isEqualTo(0);
        assertThat(((BitmapDrawable) displayOptionsCaptor.getValue()
                                                         .getImageForEmptyUri(resources)).getBitmap()).isSameAs(bitmap);
    }

    @Test
    public void displayInPlayerShouldDelayLoadingIfHighPriorityFlagIsNotSet() throws ExecutionException {
        when(imageCache.getPlaceholderDrawable(anyString(), anyInt(), anyInt(), any(PlaceholderGenerator.class))).thenReturn(cachedTransitionDrawable);

        Bitmap bitmap = Bitmap.createBitmap(1, 2, Bitmap.Config.RGB_565);
        imageOperations.displayInPlayer(imageResource, ApiImageSize.T120, imageView, bitmap, false);

        verify(imageLoader).displayImage(eq(CDN_URL),
                                         imageViewAwareCaptor.capture(),
                                         displayOptionsCaptor.capture(),
                                         any(FallbackImageListener.class));
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
                                         displayOptionsCaptor.capture(), any(ImageListenerUILAdapter.class));

        assertThat(displayOptionsCaptor.getValue().isCacheOnDisk()).isFalse();
        assertThat(displayOptionsCaptor.getValue().isCacheInMemory()).isTrue();
        assertThat(displayOptionsCaptor.getValue().shouldShowImageOnLoading()).isFalse();
        assertThat(displayOptionsCaptor.getValue().shouldShowImageOnFail()).isFalse();
        assertThat(displayOptionsCaptor.getValue().shouldShowImageForEmptyUri()).isFalse();
        assertThat(imageViewAwareCaptor.getValue().getWrappedView()).isSameAs(imageView);
    }

    @Test
    public void displayAppInstallDoesNotCacheAndHasPlaceholder() {
        imageOperations.displayAdImage(Urn.forAd("dfp", "123"), URL, imageView, mock(ImageListener.class));

        verify(imageLoader).displayImage(eq(URL), imageViewAwareCaptor.capture(),
                                         displayOptionsCaptor.capture(), any(ImageListenerUILAdapter.class));

        assertThat(displayOptionsCaptor.getValue().isCacheOnDisk()).isFalse();
        assertThat(displayOptionsCaptor.getValue().isCacheInMemory()).isTrue();
        assertThat(displayOptionsCaptor.getValue().getDisplayer()).isInstanceOf(PlaceholderTransitionDisplayer.class);
        assertThat(imageViewAwareCaptor.getValue().getWrappedView()).isSameAs(imageView);
    }

    @Test
    public void displayImageInAdapterViewShouldUsePlaceholderFromCache() throws ExecutionException {
        when(imageView.getLayoutParams()).thenReturn(new ViewGroup.LayoutParams(100, 100));
        when(imageCache.getPlaceholderDrawable(eq(RESOLVER_URL), anyInt(), anyInt(), any())).thenReturn(
                cachedTransitionDrawable);
        imageOperations.displayInAdapterView(URN, Optional.absent(), ApiImageSize.T120, imageView, ImageOperations.DisplayType.DEFAULT);

        verify(imageLoader).displayImage(eq(RESOLVER_URL), any(ImageAware.class),
                                         displayOptionsCaptor.capture(), any(ImageLoadingListener.class));
        assertThat(displayOptionsCaptor.getValue().getImageOnLoading(resources())).isSameAs(cachedTransitionDrawable);
        assertThat(displayOptionsCaptor.getValue().getImageOnFail(resources())).isSameAs(cachedTransitionDrawable);
        assertThat(displayOptionsCaptor.getValue().getImageForEmptyUri(resources())).isSameAs(cachedTransitionDrawable);
    }

    @Test
    public void displayWithPlaceholderObservablePassesBitmapFromLoadCompleteToAdapter() {
        final Bitmap bitmap = Mockito.mock(Bitmap.class);
        ArgumentCaptor<ImageLoadingListener> listenerCaptor = ArgumentCaptor.forClass(ImageLoadingListener.class);
        TestSubscriber<Bitmap> subscriber = new TestSubscriber<>();
        when(bitmapLoadingAdapterFactory.create(any(Subscriber.class))).thenReturn(bitmapLoadingAdapter);

        imageOperations.displayWithPlaceholderObservable(imageResource,
                                                         ApiImageSize.T500,
                                                         imageView).subscribe(subscriber);

        verify(imageLoader).displayImage(any(String.class),
                                         any(ImageViewAware.class),
                                         any(DisplayImageOptions.class),
                                         listenerCaptor.capture());

        listenerCaptor.getValue().onLoadingComplete("ad-image-url", imageView, bitmap);
        verify(bitmapLoadingAdapter).onLoadingComplete(eq("ad-image-url"), any(ImageView.class), eq(bitmap));
    }

    @Test
    public void adImageObservablePassesBitmapFromLoadCompleteToLoadingAdapter() {
        final Bitmap bitmap = Mockito.mock(Bitmap.class);
        ArgumentCaptor<ImageLoadingListener> captor = ArgumentCaptor.forClass(ImageLoadingListener.class);

        Observable<Bitmap> observable = imageOperations.bitmap(Uri.parse(URL));
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

        Observable<Bitmap> observable = imageOperations.bitmap(Uri.parse(URL));
        TestSubscriber<Bitmap> subscriber = new TestSubscriber<>();
        when(bitmapLoadingAdapterFactory.create(any(Subscriber.class))).thenReturn(bitmapLoadingAdapter);
        observable.subscribe(subscriber);

        verify(imageLoader).displayImage(eq(URL),
                                         any(NonViewAware.class),
                                         displayOptionsCaptor.capture(),
                                         captor.capture());
        assertThat(displayOptionsCaptor.getValue().isCacheOnDisk()).isFalse();
        assertThat(displayOptionsCaptor.getValue().isCacheInMemory()).isTrue();
        Exception cause = new Exception("Decoding error");
        captor.getValue().onLoadingFailed("ad-image-url", imageView,
                                          new FailReason(FailReason.FailType.DECODING_ERROR,
                                                         cause));
        verify(bitmapLoadingAdapter).onLoadingFailed("ad-image-url", imageView, cause);
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
        Exception cause = new Exception("Decoding error");
        captor.getValue().onLoadingFailed("asdf", imageView,
                                          new FailReason(FailReason.FailType.DECODING_ERROR,
                                                         cause));
        verify(fallbackBitmapLoadingAdapter).onLoadingFailed("asdf", imageView, cause);
    }

    @Test
    public void blurredPlayerArtworkReturnsBlurredImageFromCache() throws Exception {
        final TestSubscriber<Bitmap> subscriber = new TestSubscriber<>();
        Bitmap blurredBitmap = Bitmap.createBitmap(2, 1, Bitmap.Config.RGB_565);

        when(imageCache.getBlurredImage(URN)).thenReturn(blurredBitmap);

        imageOperations.blurredArtwork(resources(), imageResource, Optional.absent(), scheduler, scheduler).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).containsExactly(blurredBitmap);
    }

    @Test
    public void blurredPlayerArtworkCreatesBlurredImageFromCache() throws Exception {
        final TestSubscriber<Bitmap> subscriber = new TestSubscriber<>();
        Bitmap cachedBitmap = Bitmap.createBitmap(1, 2, Bitmap.Config.RGB_565);
        Bitmap blurredBitmap = Bitmap.createBitmap(2, 1, Bitmap.Config.RGB_565);

        when(imageCache.cacheBlurredBitmap(any())).thenReturn(bitmapAction1);
        when(memoryCache.get(anyString())).thenReturn(cachedBitmap);
        when(imageProcessor.blurBitmap(cachedBitmap, Optional.absent())).thenReturn(blurredBitmap);

        imageOperations.blurredArtwork(resources(), imageResource, Optional.absent(), scheduler, scheduler).subscribe(subscriber);

        assertThat(subscriber.getOnNextEvents()).containsExactly(blurredBitmap);
    }

    @Test
    public void blurredPlayerArtworkCachesBlurredImage() throws Exception {
        final TestSubscriber<Bitmap> subscriber = new TestSubscriber<>();
        Bitmap cachedBitmaop = Bitmap.createBitmap(1, 2, Bitmap.Config.RGB_565);
        Bitmap blurredBitmap = Bitmap.createBitmap(2, 1, Bitmap.Config.RGB_565);

        when(memoryCache.get(anyString())).thenReturn(cachedBitmaop);
        when(imageProcessor.blurBitmap(cachedBitmaop, Optional.absent())).thenReturn(blurredBitmap);

        imageOperations.blurredArtwork(resources(), imageResource, Optional.absent(), scheduler, scheduler).subscribe(subscriber);

        verify(imageCache).cacheBlurredBitmap(URN);
    }

    @Test
    public void buildUrlIfNotPreviouslyMissingReturnsFullSizeUrl() throws Exception {
        assertThat(imageOperations.getImageUrl(Optional.absent(), URN, ApiImageSize.getFullImageSize(resources))).isEqualTo(RESOLVER_URL);
    }

    @Test
    public void displayCircularInAdapterViewShouldRequestImagesThroughMobileImageResolver() {
        imageOperations.displayInAdapterView(URN, Optional.absent(), ApiImageSize.T120, imageView, ImageOperations.DisplayType.CIRCULAR);
        verify(imageLoader)
               .displayImage(eq(RESOLVER_URL), any(ImageViewAware.class), any(DisplayImageOptions.class),
                             any(ImageLoadingListener.class));
    }

    @Test
    public void displayCircularInAdapterViewCalledWithoutUrnShouldUseUrl() {
        imageOperations.displayInAdapterView(Urn.NOT_SET, ARTWORK_TEMPLATE_URL, ApiImageSize.T120, imageView, ImageOperations.DisplayType.CIRCULAR);
        verify(imageLoader)
                .displayImage(eq(CDN_URL), any(ImageViewAware.class), any(DisplayImageOptions.class),
                              any(ImageLoadingListener.class));
    }

    @Test
    public void displayCircularWithPlaceholderShouldRequestImagesThroughMobileImageResolver() {
        imageOperations.displayCircularWithPlaceholder(URN, Optional.absent(), ApiImageSize.T120, imageView);
        verify(imageLoader)
                .displayImage(eq(RESOLVER_URL), any(ImageViewAware.class), any(DisplayImageOptions.class),
                              any(ImageLoadingListener.class));
    }

    @Test
    public void displayCircularWithPlaceholderCalledWithoutUrnShouldUseUrl() {
        imageOperations.displayCircularWithPlaceholder(Urn.NOT_SET, ARTWORK_TEMPLATE_URL, ApiImageSize.T120, imageView);
        verify(imageLoader)
                .displayImage(eq(CDN_URL), any(ImageViewAware.class), any(DisplayImageOptions.class),
                              any(ImageLoadingListener.class));
    }

    @Test
    public void displayCircularWithPlaceholderWithAbsentImageUrlAndAbsentUrnResolvesInPlaceholder() {
        when(imageView.getLayoutParams()).thenReturn(new ViewGroup.LayoutParams(100, 100));
        when(imageCache.getPlaceholderDrawable(isNull(), anyInt(), anyInt(), any(PlaceholderGenerator.class))).thenReturn(cachedTransitionDrawable);

        imageOperations.displayCircularWithPlaceholder(Urn.NOT_SET, Optional.absent(), ApiImageSize.T120, imageView);

        verify(imageLoader).displayImage(eq(null),
                                         imageViewAwareCaptor.capture(),
                                         displayOptionsCaptor.capture(),
                                         any(FallbackImageListener.class));

        assertThat(imageViewAwareCaptor.getValue().getWrappedView()).isEqualTo(imageView);
        assertThat(displayOptionsCaptor.getValue().getImageOnLoading(resources())).isSameAs(cachedTransitionDrawable);
        assertThat(displayOptionsCaptor.getValue().getImageOnFail(resources())).isSameAs(cachedTransitionDrawable);
        assertThat(displayOptionsCaptor.getValue().getImageForEmptyUri(resources())).isSameAs(cachedTransitionDrawable);
        assertThat(displayOptionsCaptor.getValue().isCacheOnDisk()).isTrue();
        assertThat(displayOptionsCaptor.getValue().isCacheInMemory()).isTrue();
    }

}
