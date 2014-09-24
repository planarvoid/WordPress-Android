package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observable;
import rx.Scheduler;
import rx.Subscription;
import rx.schedulers.Schedulers;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.widget.ImageView;

@RunWith(SoundCloudTestRunner.class)
public class BlurringPlayerArtworkLoaderTest {

    @Mock ImageOperations imageOperations;
    @Mock ImageView wrappedImageView;
    @Mock ImageView imageOverlayView;
    @Mock ImageListener listener;
    @Mock Subscription subscription;
    @Mock ViewVisibilityProvider viewVisibilityProvider;

    private PlayerArtworkLoader playerArtworkLoader;
    private Resources resources = Robolectric.application.getResources();
    private Urn urn = Urn.forTrack(123L);
    private Bitmap cachedBitmap = Bitmap.createBitmap(0, 0, Bitmap.Config.RGB_565);
    private Scheduler immediateScheduler = Schedulers.immediate();

    @Before
    public void setUp() throws Exception {
        playerArtworkLoader = new BlurringPlayerArtworkLoader(imageOperations, Robolectric.application.getResources(), Schedulers.immediate(), Schedulers.immediate());
    }

    @Test
    public void loadArtworkLoadsArtworkThroughImageOperations() throws Exception {
        when(imageOperations.getCachedListItemBitmap(resources, urn)).thenReturn(cachedBitmap);
        when(imageOperations.blurredPlayerArtwork(resources, urn, immediateScheduler, immediateScheduler)).thenReturn(Observable.<Bitmap>empty());

        playerArtworkLoader.loadArtwork(urn, wrappedImageView, imageOverlayView, listener, true, viewVisibilityProvider);

        verify(imageOperations).displayInPlayer(urn, ApiImageSize.getFullImageSize(Robolectric.application.getResources()),
                wrappedImageView, listener, cachedBitmap, true);
    }

    @Test
    public void loadArtworkSetsBlurredArtworkOnImageOverlay() throws Exception {
        final Bitmap blurredBitmap = Bitmap.createBitmap(0, 0, Bitmap.Config.RGB_565);
        when(imageOperations.getCachedListItemBitmap(resources, urn)).thenReturn(cachedBitmap);
        when(imageOperations.blurredPlayerArtwork(resources, urn, immediateScheduler, immediateScheduler)).thenReturn(Observable.just(blurredBitmap));

        playerArtworkLoader.loadArtwork(urn, wrappedImageView, imageOverlayView, listener, true, viewVisibilityProvider);

        verify(imageOverlayView).setImageBitmap(blurredBitmap);
    }

    @Test
    public void loadArtworkSetsBlurredArtworkOnImageOverlayWithTransitionDrawableWhenOnScreen() throws Exception {
        final Bitmap blurredBitmap = Bitmap.createBitmap(0, 0, Bitmap.Config.RGB_565);
        when(imageOperations.getCachedListItemBitmap(resources, urn)).thenReturn(cachedBitmap);
        when(imageOperations.blurredPlayerArtwork(resources, urn, immediateScheduler, immediateScheduler)).thenReturn(Observable.just(blurredBitmap));
        when(viewVisibilityProvider.isCurrentlyVisible(imageOverlayView)).thenReturn(true);

        playerArtworkLoader.loadArtwork(urn, wrappedImageView, imageOverlayView, listener, true, viewVisibilityProvider);

        ArgumentCaptor<TransitionDrawable> captor = ArgumentCaptor.forClass(TransitionDrawable.class);
        verify(imageOverlayView).setImageDrawable(captor.capture());
        expect(((BitmapDrawable)captor.getValue().getDrawable(1)).getBitmap()).toBe(blurredBitmap);
    }

    @Test
    public void loadArtworkUnsubscribesFromOldBlurringOperations() throws Exception {
        when(imageOperations.getCachedListItemBitmap(resources, urn)).thenReturn(cachedBitmap);
        when(imageOperations.blurredPlayerArtwork(resources, urn, immediateScheduler, immediateScheduler))
                .thenReturn(TestObservables.<Bitmap>endlessObservablefromSubscription(subscription));

        playerArtworkLoader.loadArtwork(urn, wrappedImageView, imageOverlayView, listener, true, viewVisibilityProvider);
        playerArtworkLoader.loadArtwork(urn, wrappedImageView, imageOverlayView, listener, true, viewVisibilityProvider);

        verify(subscription).unsubscribe();

    }
}