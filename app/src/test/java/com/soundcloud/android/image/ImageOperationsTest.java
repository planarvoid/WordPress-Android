package com.soundcloud.android.image;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.image.ImageOptionsFactory.PlaceholderTransitionDisplayer;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.cache.Cache;
import com.nostra13.universalimageloader.cache.disc.DiscCacheAware;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.assist.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Subscriber;
import rx.observers.TestSubscriber;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
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

    @Mock
    ImageLoader imageLoader;
    @Mock
    ImageEndpointBuilder imageEndpointBuilder;
    @Mock
    PlaceholderGenerator placeholderGenerator;
    @Mock
    DiscCacheAware diskCache;
    @Mock
    ImageListener imageListener;
    @Mock
    ImageView imageView;
    @Mock
    Resources resources;
    @Mock
    Drawable drawable;
    @Mock
    View parentView;
    @Mock
    FailReason failReason;
    @Mock
    Cache cache;
    @Mock
    ViewlessLoadingAdapter.Factory viewlessLoadingAdapterFactory;
    @Mock
    ViewlessLoadingAdapter viewlessLoadingAdapter;

    @Captor
    ArgumentCaptor<ImageListenerUILAdapter> imageListenerUILAdapterCaptor;
    @Captor
    ArgumentCaptor<ImageViewAware> imageViewAwareCaptor;
    @Captor
    ArgumentCaptor<DisplayImageOptions> displayOptionsCaptor;
    @Captor
    ArgumentCaptor<SimpleImageLoadingListener> simpleImageLoadingListenerCaptor;

    final private String URL = "https://i1.sndcdn.com/artworks-000058493054-vcrifw-t500x500.jpg?b09b136";
    final private String ADJUSTED_URL = "http://i1.sndcdn.com/artworks-000058493054-vcrifw-t500x500.jpg?b09b136";
    final private String URL_WITH_PARAMS = "https://api.soundcloud.com/resolve/image?url=soundcloud%3Ausers%3A1818488&client_id=40ccfee680a844780a41fbe23ea89934&size=t500x500";
    final private String ADJUSTED_URL_WITH_PARAMS = "http://api.soundcloud.com/resolve/image?url=soundcloud%3Ausers%3A1818488&client_id=40ccfee680a844780a41fbe23ea89934&size=t500x500";

    final private static String RESOLVER_URL_LARGE = "http://api.soundcloud.com/app/mobileapps/images/soundcloud:tracks:1/large";
    final private static Urn URN = Urn.parse("soundcloud:tracks:1");

    @Before
    public void setUp() throws Exception {
        imageOperations = new ImageOperations(imageLoader, imageEndpointBuilder, placeholderGenerator, cache, viewlessLoadingAdapterFactory);
        when(imageLoader.getDiscCache()).thenReturn(diskCache);
        when(imageEndpointBuilder.imageUrl(URN, ImageSize.LARGE)).thenReturn(RESOLVER_URL_LARGE);
        when(placeholderGenerator.generate(any(String.class))).thenReturn(drawable);
    }

    @Test
    public void NotFoundExceptionDuringAdapterViewLoadMakesNextLoadToPassNullPath() throws Exception {
        when(failReason.getCause()).thenReturn(new FileNotFoundException());

        // 1st load
        imageOperations.displayInAdapterView(URN, ImageSize.LARGE, imageView);
        InOrder inOrder = Mockito.inOrder(imageLoader);
        inOrder.verify(imageLoader).displayImage(eq(RESOLVER_URL_LARGE), any(ImageViewAware.class), any(DisplayImageOptions.class), simpleImageLoadingListenerCaptor.capture());
        simpleImageLoadingListenerCaptor.getValue().onLoadingFailed(RESOLVER_URL_LARGE, imageView, failReason);

        // 2nd load
        imageOperations.displayInAdapterView(URN, ImageSize.LARGE, imageView);
        inOrder.verify(imageLoader).displayImage((String) isNull(), any(ImageViewAware.class), any(DisplayImageOptions.class), any(SimpleImageLoadingListener.class));
        when(cache.get(anyString(), any(Callable.class))).thenReturn(drawable);
    }

    @Test
    public void IOExceptionDuringAdapterViewLoadAllowsRetryWithUrl() throws Exception {
        when(failReason.getCause()).thenReturn(new IOException());

        // 1st load
        imageOperations.displayInAdapterView(URN, ImageSize.LARGE, imageView);
        InOrder inOrder = Mockito.inOrder(imageLoader);
        inOrder.verify(imageLoader).displayImage(eq(RESOLVER_URL_LARGE), any(ImageViewAware.class), any(DisplayImageOptions.class), simpleImageLoadingListenerCaptor.capture());
        simpleImageLoadingListenerCaptor.getValue().onLoadingFailed(RESOLVER_URL_LARGE, imageView, failReason);

        // 2nd load
        imageOperations.displayInAdapterView(URN, ImageSize.LARGE, imageView);
        inOrder.verify(imageLoader).displayImage(eq(RESOLVER_URL_LARGE), any(ImageViewAware.class), any(DisplayImageOptions.class), any(SimpleImageLoadingListener.class));
    }

    @Test
    public void NotFoundExceptionDuringPlaceholderLoadMakesNextLoadToPassNullPath() throws Exception {
        when(failReason.getCause()).thenReturn(new FileNotFoundException());

        // 1st load
        imageOperations.displayWithPlaceholder(URN, ImageSize.LARGE, imageView);
        InOrder inOrder = Mockito.inOrder(imageLoader);
        inOrder.verify(imageLoader).displayImage(eq(RESOLVER_URL_LARGE), any(ImageViewAware.class), any(DisplayImageOptions.class), simpleImageLoadingListenerCaptor.capture());
        simpleImageLoadingListenerCaptor.getValue().onLoadingFailed(RESOLVER_URL_LARGE, imageView, failReason);

        // 2nd load
        imageOperations.displayWithPlaceholder(URN, ImageSize.LARGE, imageView);
        inOrder.verify(imageLoader).displayImage((String) isNull(), any(ImageViewAware.class), any(DisplayImageOptions.class), any(SimpleImageLoadingListener.class));
    }

    @Test
    public void shouldLoadImageByUrnWithHeadlessListener() throws Exception {
        final String imageUrl = RESOLVER_URL_LARGE;
        when(imageEndpointBuilder.imageUrl(URN, ImageSize.LARGE)).thenReturn(imageUrl);

        imageOperations.load(URN, ImageSize.LARGE, imageListener);

        verify(imageLoader).loadImage(eq(imageUrl), imageListenerUILAdapterCaptor.capture());
        verifyCapturedListener();
    }

    @Test
    public void shouldLoadImageByURLWithHeadlessListener() throws Exception {
        imageOperations.load(URL, imageListener);
        verify(imageLoader).loadImage(eq(ADJUSTED_URL), imageListenerUILAdapterCaptor.capture());
        verifyCapturedListener();
    }

    @Test
    public void shouldLoadImageByURLWithListenerAndParameterizedUrl() throws Exception {
        imageOperations.load(URL_WITH_PARAMS, imageListener);
        verify(imageLoader).loadImage(eq(ADJUSTED_URL_WITH_PARAMS), imageListenerUILAdapterCaptor.capture());
        verifyCapturedListener();
    }

    @Test
    public void shouldAcceptNullImageUrl() throws Exception {
        imageOperations.load(null, imageListener);
        verify(imageLoader).loadImage(isNull(String.class), imageListenerUILAdapterCaptor.capture());
        verifyCapturedListener();
    }

    @Test
    public void shouldNotAdjustUrlIfDoesNotMatch() throws Exception {
        imageOperations.load("does_not_match_url", imageListener);
        verify(imageLoader).loadImage(eq("does_not_match_url"), imageListenerUILAdapterCaptor.capture());
        verifyCapturedListener();
    }

    @Test
    public void displayShouldCallDisplayWithAdjustedUrlAndImageViewAware() throws Exception {
        imageOperations.display(URL, imageView);
        verify(imageLoader).displayImage(eq(ADJUSTED_URL), imageViewAwareCaptor.capture());
        expect(imageViewAwareCaptor.getValue().getWrappedView()).toBe(imageView);
        expect(imageViewAwareCaptor.getValue().getWidth()).toBe(0);
    }

    @Test
    public void displayImageInAdapterViewShouldRequestImagesThroughMobileImageResolver() {
        final String imageUrl = "http://api.soundcloud.com/app/mobileapps/images/soundcloud:tracks:1/large";
        when(imageEndpointBuilder.imageUrl(URN, ImageSize.LARGE)).thenReturn(imageUrl);

        imageOperations.displayInAdapterView(URN, ImageSize.LARGE, imageView);
        verify(imageLoader).displayImage(
                eq(imageUrl), any(ImageAware.class), any(DisplayImageOptions.class), any(SimpleImageLoadingListener.class));
    }

    @Test
    public void displayImageInAdapterViewShouldWrapAndForwardTheGivenImageView() {
        imageOperations.displayInAdapterView(URN, ImageSize.LARGE, imageView);

        verify(imageLoader).displayImage(
                anyString(), imageViewAwareCaptor.capture(), any(DisplayImageOptions.class), any(SimpleImageLoadingListener.class));
        expect(imageViewAwareCaptor.getValue().getWrappedView()).toBe(imageView);
    }

    @Test
    public void displayImageInAdapterViewShouldShouldUseCorrectDisplayOptions() {
        imageOperations.displayInAdapterView(URN, ImageSize.LARGE, imageView);

        verify(imageLoader).displayImage(anyString(), any(ImageAware.class), displayOptionsCaptor.capture(), any(SimpleImageLoadingListener.class));
        expect(displayOptionsCaptor.getValue().isResetViewBeforeLoading()).toBeTrue();
        expect(displayOptionsCaptor.getValue().getDisplayer()).toBeInstanceOf(PlaceholderTransitionDisplayer.class);
        verifyFullCacheOptions();
    }

    @Test
    public void displayInPlayerViewShouldCallDisplayWithAdjustedUrlImageViewAwarePlayerOptionsAndListener() throws Exception {
        final String imageUrl = "http://api.soundcloud.com/app/mobileapps/images/soundcloud:tracks:1/t500x500";
        when(imageEndpointBuilder.imageUrl(URN, ImageSize.T500)).thenReturn(imageUrl);

        imageOperations.displayInPlayerView(URN, ImageSize.T500, imageView, parentView, false, imageListener);

        verify(imageLoader).displayImage(eq(imageUrl), imageViewAwareCaptor.capture(),
                displayOptionsCaptor.capture(), imageListenerUILAdapterCaptor.capture());
        expect(imageViewAwareCaptor.getValue().getWrappedView()).toBe(imageView);
        expect(displayOptionsCaptor.getValue().getDelayBeforeLoading()).toEqual(ImageOptionsFactory.DELAY_BEFORE_LOADING_LOW_PRIORITY);
        expect(displayOptionsCaptor.getValue().getDisplayer()).toBeInstanceOf(ImageOptionsFactory.PlayerBitmapDisplayer.class);
        verifyFullCacheOptions();
        verifyCapturedListener();
    }

    @Test
    public void displayInFullDialogViewShouldLoadImageFromMobileImageResolver() throws Exception {
        final String imageUrl = "http://api.soundcloud.com/app/mobileapps/images/soundcloud:tracks:1/t500x500";
        when(imageEndpointBuilder.imageUrl(URN, ImageSize.T500)).thenReturn(imageUrl);

        imageOperations.displayInFullDialogView(URN, ImageSize.T500, imageView, imageListener);

        verify(imageLoader).displayImage(
                eq(imageUrl), any(ImageAware.class), any(DisplayImageOptions.class), any(ImageListenerUILAdapter.class));
    }

    @Test
    public void displayInFullDialogViewShouldWrapAndForwardTheGivenImageView() throws Exception {
        imageOperations.displayInFullDialogView(URN, ImageSize.T500, imageView, imageListener);

        verify(imageLoader).displayImage(
                anyString(), imageViewAwareCaptor.capture(), any(DisplayImageOptions.class), any(ImageListenerUILAdapter.class));
        expect(imageViewAwareCaptor.getValue().getWrappedView()).toBe(imageView);
    }

    @Test
    public void displayInFullDialogViewShouldUseCorrectImageListener() throws Exception {
        imageOperations.displayInFullDialogView(URN, ImageSize.T500, imageView, imageListener);

        verify(imageLoader).displayImage(
                anyString(), any(ImageAware.class), any(DisplayImageOptions.class), imageListenerUILAdapterCaptor.capture());
        verifyCapturedListener();
    }

    @Test
    public void displayInFullDialogViewShouldUseCorrectDisplayOptions() throws Exception {
        imageOperations.displayInFullDialogView(URN, ImageSize.T500, imageView, imageListener);

        verify(imageLoader).displayImage(
                anyString(), any(ImageAware.class), displayOptionsCaptor.capture(), any(ImageListenerUILAdapter.class));
        expect(displayOptionsCaptor.getValue().getDelayBeforeLoading()).toEqual(ImageOptionsFactory.DELAY_BEFORE_LOADING_LOW_PRIORITY);
        expect(displayOptionsCaptor.getValue().getDisplayer()).toBeInstanceOf(FadeInBitmapDisplayer.class);
        expect(displayOptionsCaptor.getValue().isCacheOnDisc()).toBeTrue();
    }

    @Test
    public void displayWithPlaceholderShouldLoadImageFromMobileApiAndPlaceholderOptions() throws Exception {
        final String imageUrl = RESOLVER_URL_LARGE;
        when(imageEndpointBuilder.imageUrl(URN, ImageSize.LARGE)).thenReturn(imageUrl);
        when(cache.get(anyString(), any(Callable.class))).thenReturn(drawable);

        imageOperations.displayWithPlaceholder(URN, ImageSize.LARGE, imageView);

        verify(imageLoader).displayImage(eq(imageUrl), imageViewAwareCaptor.capture(), displayOptionsCaptor.capture(), any(SimpleImageLoadingListener.class));
        expect(imageViewAwareCaptor.getValue().getWrappedView()).toBe(imageView);
        verifyFallbackDrawableOptions(RES_ID);
        verifyFullCacheOptions();
    }

    @Test
    public void prefetchShouldCallDisplayWithAdjustedUrlImageViewAwareAndPlaceholderOptions() throws Exception {
        imageOperations.prefetch(URL);
        verify(imageLoader).loadImage(eq(ADJUSTED_URL), displayOptionsCaptor.capture(), isNull(ImageLoadingListener.class));
        expect(displayOptionsCaptor.getValue().isCacheInMemory()).toBeFalse();
        expect(displayOptionsCaptor.getValue().isCacheOnDisc()).toBeTrue();
    }

    @Test
    public void displayImageInAdapterViewShouldUsePlaceholderFromCache() throws ExecutionException {
        when(imageView.getLayoutParams()).thenReturn(new ViewGroup.LayoutParams(100, 100));
        when(cache.get(eq("soundcloud:sounds:1_100_100"), any(Callable.class))).thenReturn(drawable);
        imageOperations.displayInAdapterView(URN, ImageSize.LARGE, imageView);

        verify(imageLoader).displayImage(eq(RESOLVER_URL_LARGE), any(ImageAware.class), displayOptionsCaptor.capture(), any(ImageLoadingListener.class));
        expect(displayOptionsCaptor.getValue().getImageOnLoading(Robolectric.application.getResources())).toBe(drawable);
        expect(displayOptionsCaptor.getValue().getImageOnFail(Robolectric.application.getResources())).toBe(drawable);
        expect(displayOptionsCaptor.getValue().getImageForEmptyUri(Robolectric.application.getResources())).toBe(drawable);
    }

    @Test
    public void displayWithPlaceholderShouldUsePlaceholderFromCache() throws ExecutionException {
        when(imageView.getLayoutParams()).thenReturn(new ViewGroup.LayoutParams(100, 100));
        when(cache.get(eq("soundcloud:sounds:1_100_100"), any(Callable.class))).thenReturn(drawable);
        imageOperations.displayWithPlaceholder(URN, ImageSize.LARGE, imageView);

        verify(imageLoader).displayImage(eq(RESOLVER_URL_LARGE), any(ImageAware.class), displayOptionsCaptor.capture(), any(ImageLoadingListener.class));
        expect(displayOptionsCaptor.getValue().getImageOnLoading(Robolectric.application.getResources())).toBe(drawable);
        expect(displayOptionsCaptor.getValue().getImageOnFail(Robolectric.application.getResources())).toBe(drawable);
        expect(displayOptionsCaptor.getValue().getImageForEmptyUri(Robolectric.application.getResources())).toBe(drawable);
    }

    @Test
    public void copiedImagePassesBitmapFromLoadCompleteToLoadingAdapter() throws Exception {
        final Bitmap bitmap = Mockito.mock(Bitmap.class);
        ArgumentCaptor<ImageLoadingListener> captor = ArgumentCaptor.forClass(ImageLoadingListener.class);

        Observable<Bitmap> observable = imageOperations.copiedImage(URN, ImageSize.LARGE);
        TestSubscriber<Bitmap> subscriber = new TestSubscriber<Bitmap>();
        when(viewlessLoadingAdapterFactory.create(any(Subscriber.class), eq(true))).thenReturn(viewlessLoadingAdapter);
        observable.subscribe(subscriber);

        verify(imageLoader).loadImage(eq(RESOLVER_URL_LARGE), captor.capture());
        captor.getValue().onLoadingComplete("asdf", imageView, bitmap);
        verify(viewlessLoadingAdapter).onLoadingComplete("asdf", imageView, bitmap);
    }

    @Test
    public void copiedImagePassesLoadFailedToLoadingAdapter() throws Exception {
        ArgumentCaptor<ImageLoadingListener> captor = ArgumentCaptor.forClass(ImageLoadingListener.class);

        Observable<Bitmap> observable = imageOperations.copiedImage(URN, ImageSize.LARGE);
        TestSubscriber<Bitmap> subscriber = new TestSubscriber<Bitmap>();
        when(viewlessLoadingAdapterFactory.create(any(Subscriber.class), eq(true))).thenReturn(viewlessLoadingAdapter);
        observable.subscribe(subscriber);

        verify(imageLoader).loadImage(eq(RESOLVER_URL_LARGE), captor.capture());
        captor.getValue().onLoadingFailed("asdf", imageView, new FailReason(FailReason.FailType.DECODING_ERROR, new Exception("Decoding error")));
        verify(viewlessLoadingAdapter).onLoadingFailed("asdf", imageView, "Decoding error");
    }

    private void verifyCapturedListener() {
        ImageListenerUILAdapter imageListenerUILAdapter = imageListenerUILAdapterCaptor.getValue();
        View view = mock(View.class);
        Bitmap bitmap = mock(Bitmap.class);

        FailReason failReason = mock(FailReason.class);
        Throwable cause = mock(Throwable.class);

        when(failReason.getCause()).thenReturn(cause);
        final String message = "Cause message";
        when(cause.getMessage()).thenReturn(message);

        imageListenerUILAdapter.onLoadingStarted(ADJUSTED_URL, view);
        imageListenerUILAdapter.onLoadingFailed(ADJUSTED_URL, view, failReason);
        imageListenerUILAdapter.onLoadingComplete(ADJUSTED_URL, view, bitmap);

        verify(imageListener).onLoadingStarted(ADJUSTED_URL, view);
        verify(imageListener).onLoadingFailed(ADJUSTED_URL, view, message);
        verify(imageListener).onLoadingComplete(ADJUSTED_URL, view, bitmap);
    }

    private void verifyFullCacheOptions() {
        expect(displayOptionsCaptor.getValue().isCacheOnDisc()).toBeTrue();
        expect(displayOptionsCaptor.getValue().isCacheInMemory()).toBeTrue();
    }

    private void verifyFallbackDrawableOptions(int fallbackImageResId) {
        when(resources.getDrawable(fallbackImageResId)).thenReturn(drawable);
        expect(displayOptionsCaptor.getValue().getImageForEmptyUri(resources)).toBe(drawable);
        expect(displayOptionsCaptor.getValue().getImageOnFail(resources)).toBe(drawable);
        expect(displayOptionsCaptor.getValue().getImageOnLoading(resources)).toBe(drawable);
    }
}
