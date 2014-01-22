package com.soundcloud.android.image;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.nostra13.universalimageloader.core.display.FadeInBitmapDisplayer;
import com.nostra13.universalimageloader.core.imageaware.ImageViewAware;
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
    ArgumentCaptor<ImageViewAware> imageViewAwareArgumentCaptor;
    @Captor
    ArgumentCaptor<DisplayImageOptions> displayImageOptionsArgumentCaptor;

    final private String URL = "https://i1.sndcdn.com/artworks-000058493054-vcrifw-t500x500.jpg?b09b136";
    final private String ADJUSTED_URL = "http://i1.sndcdn.com/artworks-000058493054-vcrifw-t500x500.jpg";
    final private String URL2 = "https://api.soundcloud.com/resolve/image?url=soundcloud%3Ausers%3A1818488&client_id=40ccfee680a844780a41fbe23ea89934&size=t500x500";
    final private String ADJUSTED_URL2 = "http://api.soundcloud.com/resolve/image?url=soundcloud%3Ausers%3A1818488&client_id=40ccfee680a844780a41fbe23ea89934&size=t500x500";

    @Before
    public void setUp() throws Exception {
        imageOperations = new ImageOperations(imageLoader);
        when(resources.getDrawable(RES_ID)).thenReturn(drawable);
    }

    @Test
    public void shouldLoadImageWithListener() throws Exception {
        imageOperations.load(URL, imageListener);
        verify(imageLoader).loadImage(eq(ADJUSTED_URL), imageListenerUILAdapterCaptor.capture());
        verifyCapturedListener();

        imageOperations.load(URL2, imageListener);
        verify(imageLoader).loadImage(eq(ADJUSTED_URL2), imageListenerUILAdapterCaptor.capture());
        verifyCapturedListener();
    }

    @Test
    public void displayShouldCallDisplayWithAdjustedUrlAndImageViewAware() throws Exception {
        imageOperations.display(URL, imageView);
        verify(imageLoader).displayImage(eq(ADJUSTED_URL), imageViewAwareArgumentCaptor.capture());
        expect(imageViewAwareArgumentCaptor.getValue().getWrappedView()).toBe(imageView);
        expect(imageViewAwareArgumentCaptor.getValue().getWidth()).toBe(0);
    }

    @Test
    public void displayInGridViewShouldCallDisplayWithAdjustedUrlAndImageViewAware() throws Exception {
        imageOperations.displayInGridView(URL, imageView);
        verify(imageLoader).displayImage(eq(ADJUSTED_URL), imageViewAwareArgumentCaptor.capture(), displayImageOptionsArgumentCaptor.capture());
        expect(imageViewAwareArgumentCaptor.getValue().getWrappedView()).toBe(imageView);
    }

    @Test
    public void displayInAdapterViewShouldCallDisplayWithAdjustedUrlImageViewAwareAndDefaultIcon() throws Exception {
        imageOperations.displayInAdapterView(URL, imageView, RES_ID);
        verify(imageLoader).displayImage(eq(ADJUSTED_URL), imageViewAwareArgumentCaptor.capture(), displayImageOptionsArgumentCaptor.capture());
        expect(imageViewAwareArgumentCaptor.getValue().getWrappedView()).toBe(imageView);
        expect(displayImageOptionsArgumentCaptor.getValue().getImageForEmptyUri(resources)).toBe(drawable);
        expect(displayImageOptionsArgumentCaptor.getValue().getImageOnFail(resources)).toBe(drawable);
        expect(displayImageOptionsArgumentCaptor.getValue().getImageOnLoading(resources)).toBe(drawable);
        expect(displayImageOptionsArgumentCaptor.getValue().isCacheOnDisc()).toBeTrue();
        expect(displayImageOptionsArgumentCaptor.getValue().isCacheInMemory()).toBeTrue();
    }

    @Test
    public void displayInPlayerViewShouldCallDisplayWithAdjustedUrlImageViewAwarePlayerOptionsAndListener() throws Exception {
        imageOperations.displayInPlayerView(URL, imageView, parentView, false, imageListener);

        verify(imageLoader).displayImage(eq(ADJUSTED_URL), imageViewAwareArgumentCaptor.capture(), displayImageOptionsArgumentCaptor.capture(), imageListenerUILAdapterCaptor.capture());
        expect(imageViewAwareArgumentCaptor.getValue().getWrappedView()).toBe(imageView);
        expect(displayImageOptionsArgumentCaptor.getValue().getDelayBeforeLoading()).toEqual(ImageOptionsFactory.DELAY_BEFORE_LOADING_LOW_PRIORITY);
        expect(displayImageOptionsArgumentCaptor.getValue().getDisplayer()).toBeInstanceOf(ImageOptionsFactory.PlayerBitmapDisplayer.class);
        expect(displayImageOptionsArgumentCaptor.getValue().isCacheOnDisc()).toBeTrue();
        expect(displayImageOptionsArgumentCaptor.getValue().isCacheInMemory()).toBeTrue();
        verifyCapturedListener();
    }

    @Test
    public void displayInFullDialogViewShouldCallDisplayWithAdjustedUrlImageViewAwareDialogOptionsAndListener() throws Exception {
        imageOperations.displayInFullDialogView(URL, imageView, imageListener);

        verify(imageLoader).displayImage(eq(ADJUSTED_URL), imageViewAwareArgumentCaptor.capture(), displayImageOptionsArgumentCaptor.capture(), imageListenerUILAdapterCaptor.capture());
        expect(imageViewAwareArgumentCaptor.getValue().getWrappedView()).toBe(imageView);
        expect(displayImageOptionsArgumentCaptor.getValue().getDelayBeforeLoading()).toEqual(ImageOptionsFactory.DELAY_BEFORE_LOADING_LOW_PRIORITY);
        expect(displayImageOptionsArgumentCaptor.getValue().getDisplayer()).toBeInstanceOf(FadeInBitmapDisplayer.class);
        expect(displayImageOptionsArgumentCaptor.getValue().isCacheOnDisc()).toBeTrue();
        verifyCapturedListener();
    }

    @Test
    public void displayWithPlaceholderShouldCallDisplayWithAdjustedUrlImageViewAwareAndPlaceholderOptions() throws Exception {
        imageOperations.displayPlaceholder(URL, imageView, RES_ID);

        verify(imageLoader).displayImage(eq(ADJUSTED_URL), imageViewAwareArgumentCaptor.capture(), displayImageOptionsArgumentCaptor.capture());
        expect(imageViewAwareArgumentCaptor.getValue().getWrappedView()).toBe(imageView);
        expect(displayImageOptionsArgumentCaptor.getValue().getImageForEmptyUri(resources)).toBe(drawable);
        expect(displayImageOptionsArgumentCaptor.getValue().getImageOnFail(resources)).toBe(drawable);
        expect(displayImageOptionsArgumentCaptor.getValue().getImageOnLoading(resources)).toBe(drawable);
        expect(displayImageOptionsArgumentCaptor.getValue().isCacheOnDisc()).toBeTrue();
        expect(displayImageOptionsArgumentCaptor.getValue().isCacheInMemory()).toBeTrue();
    }

    @Test
    public void prefetchShouldCallDisplayWithAdjustedUrlImageViewAwareAndPlaceholderOptions() throws Exception {
        imageOperations.prefetch(URL);
        verify(imageLoader).loadImage(eq(ADJUSTED_URL), displayImageOptionsArgumentCaptor.capture(), isNull(ImageLoadingListener.class));
        expect(displayImageOptionsArgumentCaptor.getValue().isCacheInMemory()).toBeFalse();
        expect(displayImageOptionsArgumentCaptor.getValue().isCacheOnDisc()).toBeTrue();
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

}
