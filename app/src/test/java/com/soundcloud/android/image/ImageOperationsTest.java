package com.soundcloud.android.image;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.image.ImageOptionsFactory.PlaceholderTransitionDisplayer;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nostra13.universalimageloader.cache.disc.DiscCacheAware;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.imageaware.ImageAware;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
import com.soundcloud.android.R;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;


@RunWith(SoundCloudTestRunner.class)
public class ImageOperationsTest {


    private ImageOperations imageOperations;

    private static final int RES_ID = 123;

    @Mock
    ImageLoader imageLoader;
    @Mock
    ImageEndpointBuilder imageEndpointBuilder;
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

    @Captor
    ArgumentCaptor<ImageListenerUILAdapter> imageListenerUILAdapterCaptor;
    @Captor
    ArgumentCaptor<ImageViewAware> imageViewAwareCaptor;
    @Captor
    ArgumentCaptor<DisplayImageOptions> displayOptionsCaptor;

    final private String URL = "https://i1.sndcdn.com/artworks-000058493054-vcrifw-t500x500.jpg?b09b136";
    final private String ADJUSTED_URL = "http://i1.sndcdn.com/artworks-000058493054-vcrifw-t500x500.jpg?b09b136";
    final private String URL_WITH_PARAMS = "https://api.soundcloud.com/resolve/image?url=soundcloud%3Ausers%3A1818488&client_id=40ccfee680a844780a41fbe23ea89934&size=t500x500";
    final private String ADJUSTED_URL_WITH_PARAMS = "http://api.soundcloud.com/resolve/image?url=soundcloud%3Ausers%3A1818488&client_id=40ccfee680a844780a41fbe23ea89934&size=t500x500";

    @Before
    public void setUp() throws Exception {
        imageOperations = new ImageOperations(imageLoader, imageEndpointBuilder);
        when(imageLoader.getDiscCache()).thenReturn(diskCache);
    }

    @Test
    public void shouldLoadImageByUrnWithHeadlessListener() throws Exception {
        final String imageUrl = "http://api.soundcloud.com/app/mobileapps/images/soundcloud:tracks:1/large";
        when(imageEndpointBuilder.imageUrl("soundcloud:tracks:1", ImageSize.LARGE)).thenReturn(imageUrl);

        imageOperations.load("soundcloud:tracks:1", ImageSize.LARGE, imageListener);

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
    public void displayImageInGridViewShouldRequestImagesThroughMobileImageResolver() {
        final String imageUrl = "http://api.soundcloud.com/app/mobileapps/images/soundcloud:tracks:1/large";
        when(imageEndpointBuilder.imageUrl("soundcloud:tracks:1", ImageSize.LARGE)).thenReturn(imageUrl);

        imageOperations.displayInGridView("soundcloud:tracks:1", ImageSize.LARGE, imageView);

        verify(imageLoader).displayImage(eq(imageUrl), any(ImageAware.class), any(DisplayImageOptions.class));
    }

    @Test
    public void displayImageInGridViewShouldWrapAndForwardTheGivenImageView() {
        imageOperations.displayInGridView("soundcloud:tracks:1", ImageSize.LARGE, imageView);

        verify(imageLoader).displayImage(anyString(), imageViewAwareCaptor.capture(), any(DisplayImageOptions.class));
        expect(imageViewAwareCaptor.getValue().getWrappedView()).toBe(imageView);
    }

    @Test
    public void displayImageInGridViewShouldShouldUseCorrectDisplayOptions() {
        imageOperations.displayInGridView("soundcloud:tracks:1", ImageSize.LARGE, imageView);

        verify(imageLoader).displayImage(anyString(), any(ImageAware.class), displayOptionsCaptor.capture());
        expect(displayOptionsCaptor.getValue().isResetViewBeforeLoading()).toBeTrue();
        verifyFullCacheOptions();
    }

    @Test
    public void displayInListViewShouldRequestImagesFromMobileImageResolver() {
        final String imageUrl = "http://api.soundcloud.com/app/mobileapps/images/soundcloud:tracks:1/large";
        when(imageEndpointBuilder.imageUrl("soundcloud:tracks:1", ImageSize.LARGE)).thenReturn(imageUrl);

        imageOperations.displayInListView("soundcloud:tracks:1", ImageSize.LARGE, imageView);

        verify(imageLoader).displayImage(eq(imageUrl), any(ImageAware.class), any(DisplayImageOptions.class));
    }

    @Test
    public void displayInListViewShouldWrapAndForwardTheGivenImageView() {
        imageOperations.displayInListView("soundcloud:tracks:1", ImageSize.LARGE, imageView);

        verify(imageLoader).displayImage(anyString(), imageViewAwareCaptor.capture(), any(DisplayImageOptions.class));
        expect(imageViewAwareCaptor.getValue().getWrappedView()).toBe(imageView);
    }

    @Test
    public void displayInListViewShouldUseCorrectDisplayOptions() {
        imageOperations.displayInListView("soundcloud:tracks:1", ImageSize.LARGE, imageView);

        verify(imageLoader).displayImage(anyString(), any(ImageAware.class), displayOptionsCaptor.capture());
        expect(displayOptionsCaptor.getValue().isResetViewBeforeLoading()).toBeTrue();
        expect(displayOptionsCaptor.getValue().getDisplayer()).toBeInstanceOf(PlaceholderTransitionDisplayer.class);
        verifyFallbackDrawableOptions(R.drawable.placeholder_cells);
    }

    @Test
    public void displayInPlayerViewShouldCallDisplayWithAdjustedUrlImageViewAwarePlayerOptionsAndListener() throws Exception {
        final String imageUrl = "http://api.soundcloud.com/app/mobileapps/images/soundcloud:tracks:1/t500x500";
        when(imageEndpointBuilder.imageUrl("soundcloud:tracks:1", ImageSize.T500)).thenReturn(imageUrl);

        imageOperations.displayInPlayerView("soundcloud:tracks:1", ImageSize.T500, imageView, parentView, false, imageListener);

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
        when(imageEndpointBuilder.imageUrl("soundcloud:tracks:1", ImageSize.T500)).thenReturn(imageUrl);

        imageOperations.displayInFullDialogView("soundcloud:tracks:1", ImageSize.T500, imageView, imageListener);

        verify(imageLoader).displayImage(
                eq(imageUrl), any(ImageAware.class), any(DisplayImageOptions.class), any(ImageListenerUILAdapter.class));
    }

    @Test
    public void displayInFullDialogViewShouldWrapAndForwardTheGivenImageView() throws Exception {
        imageOperations.displayInFullDialogView("soundcloud:tracks:1", ImageSize.T500, imageView, imageListener);

        verify(imageLoader).displayImage(
                anyString(), imageViewAwareCaptor.capture(), any(DisplayImageOptions.class), any(ImageListenerUILAdapter.class));
        expect(imageViewAwareCaptor.getValue().getWrappedView()).toBe(imageView);
    }

    @Test
    public void displayInFullDialogViewShouldUseCorrectImageListener() throws Exception {
        imageOperations.displayInFullDialogView("soundcloud:tracks:1", ImageSize.T500, imageView, imageListener);

        verify(imageLoader).displayImage(
                anyString(), any(ImageAware.class), any(DisplayImageOptions.class), imageListenerUILAdapterCaptor.capture());
        verifyCapturedListener();
    }

    @Test
    public void displayInFullDialogViewShouldUseCorrectDisplayOptions() throws Exception {
        imageOperations.displayInFullDialogView("soundcloud:tracks:1", ImageSize.T500, imageView, imageListener);

        verify(imageLoader).displayImage(
                anyString(), any(ImageAware.class), displayOptionsCaptor.capture(), any(ImageListenerUILAdapter.class));
        expect(displayOptionsCaptor.getValue().getDelayBeforeLoading()).toEqual(ImageOptionsFactory.DELAY_BEFORE_LOADING_LOW_PRIORITY);
        expect(displayOptionsCaptor.getValue().getDisplayer()).toBeInstanceOf(FadeInBitmapDisplayer.class);
        expect(displayOptionsCaptor.getValue().isCacheOnDisc()).toBeTrue();
    }

    @Test
    public void displayWithPlaceholderShouldCallDisplayWithAdjustedUrlImageViewAwareAndPlaceholderOptions() throws Exception {
        imageOperations.displayWithPlaceholder(URL, imageView, RES_ID);

        verify(imageLoader).displayImage(eq(ADJUSTED_URL), imageViewAwareCaptor.capture(), displayOptionsCaptor.capture());
        expect(imageViewAwareCaptor.getValue().getWrappedView()).toBe(imageView);
        verifyFallbackDrawableOptions(RES_ID);
        verifyFullCacheOptions();
    }

    @Test
    public void displayWithPlaceholderShouldLoadImageFromMobileApiAndPlaceholderOptions() throws Exception {
        final String imageUrl = "http://api.soundcloud.com/app/mobileapps/images/soundcloud:tracks:1/large";
        when(imageEndpointBuilder.imageUrl("soundcloud:tracks:1", ImageSize.LARGE)).thenReturn(imageUrl);

        imageOperations.displayWithPlaceholder("soundcloud:tracks:1", ImageSize.LARGE, imageView, RES_ID);

        verify(imageLoader).displayImage(eq(imageUrl), imageViewAwareCaptor.capture(), displayOptionsCaptor.capture());
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

    private void verifyCapturedListener(){
        ImageListenerUILAdapter imageListenerUILAdapter = imageListenerUILAdapterCaptor.getValue();
        View view = Mockito.mock(View.class);
        Bitmap bitmap = Mockito.mock(Bitmap.class);

        FailReason failReason = Mockito.mock(FailReason.class);
        Throwable cause = Mockito.mock(Throwable.class);

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
