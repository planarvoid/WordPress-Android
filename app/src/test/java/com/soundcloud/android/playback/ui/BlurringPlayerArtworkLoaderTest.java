package com.soundcloud.android.playback.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.widget.ImageView;

public class BlurringPlayerArtworkLoaderTest extends AndroidUnitTest {

    @Mock ImageOperations imageOperations;
    @Mock ImageView wrappedImageView;
    @Mock ImageView imageOverlayView;
    @Mock ImageListener listener;
    @Mock Subscription subscription;
    @Mock ViewVisibilityProvider viewVisibilityProvider;
    @Mock ImageResource imageResource;

    private PlayerArtworkLoader playerArtworkLoader;
    private Bitmap cachedBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);
    private Scheduler immediateScheduler = Schedulers.immediate();

    @Before
    public void setUp() throws Exception {
        playerArtworkLoader = new BlurringPlayerArtworkLoader(imageOperations,
                                                              resources(),
                                                              Schedulers.immediate(),
                                                              Schedulers.immediate());
    }

    @Test
    public void loadArtworkLoadsArtworkThroughImageOperations() {
        when(imageOperations.getCachedListItemBitmap(resources(), imageResource)).thenReturn(cachedBitmap);
        when(imageOperations.blurredPlayerArtwork(resources(),
                                                  imageResource,
                                                  immediateScheduler,
                                                  immediateScheduler)).thenReturn(Observable.empty());

        playerArtworkLoader.loadArtwork(imageResource,
                                        wrappedImageView,
                                        imageOverlayView,
                                        true,
                                        viewVisibilityProvider);

        verify(imageOperations).displayInPlayer(imageResource, ApiImageSize.getFullImageSize(resources()),
                                                wrappedImageView, cachedBitmap, true);
    }

    @Test
    public void loadArtworkSetsBlurredArtworkOnImageOverlay() {
        final Bitmap blurredBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);
        when(imageOperations.getCachedListItemBitmap(resources(), imageResource)).thenReturn(cachedBitmap);
        when(imageOperations.blurredPlayerArtwork(resources(), imageResource, immediateScheduler, immediateScheduler))
                .thenReturn(Observable.just(blurredBitmap));

        playerArtworkLoader.loadArtwork(imageResource,
                                        wrappedImageView,
                                        imageOverlayView,
                                        true,
                                        viewVisibilityProvider);

        verify(imageOverlayView).setImageBitmap(blurredBitmap);
    }

    @Test
    public void loadArtworkSetsBlurredArtworkOnImageOverlayWithTransitionDrawableWhenOnScreen() {
        final Bitmap blurredBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);
        when(imageOperations.getCachedListItemBitmap(resources(), imageResource)).thenReturn(cachedBitmap);
        when(imageOperations.blurredPlayerArtwork(resources(), imageResource, immediateScheduler, immediateScheduler))
                .thenReturn(Observable.just(blurredBitmap));
        when(viewVisibilityProvider.isCurrentlyVisible(imageOverlayView)).thenReturn(true);

        playerArtworkLoader.loadArtwork(imageResource,
                                        wrappedImageView,
                                        imageOverlayView,
                                        true,
                                        viewVisibilityProvider);

        ArgumentCaptor<TransitionDrawable> captor = ArgumentCaptor.forClass(TransitionDrawable.class);
        verify(imageOverlayView).setImageDrawable(captor.capture());
        assertThat(((BitmapDrawable) captor.getValue().getDrawable(1)).getBitmap()).isEqualTo(blurredBitmap);
    }

    @Test
    public void loadArtworkUnsubscribesFromOldBlurringOperations() {
        PublishSubject<Bitmap> firstBlurObservable = PublishSubject.create();
        PublishSubject<Bitmap> secondBlurObservable = PublishSubject.create();

        when(imageOperations.getCachedListItemBitmap(resources(), imageResource)).thenReturn(cachedBitmap);
        when(imageOperations.blurredPlayerArtwork(resources(), imageResource, immediateScheduler, immediateScheduler))
                .thenReturn(firstBlurObservable, secondBlurObservable);

        playerArtworkLoader.loadArtwork(imageResource,
                                        wrappedImageView,
                                        imageOverlayView,
                                        true,
                                        viewVisibilityProvider);
        playerArtworkLoader.loadArtwork(imageResource,
                                        wrappedImageView,
                                        imageOverlayView,
                                        true,
                                        viewVisibilityProvider);

        assertThat(firstBlurObservable.hasObservers()).isFalse();
        assertThat(secondBlurObservable.hasObservers()).isTrue();
    }
}
