package com.soundcloud.android.playback.ui;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.image.SimpleImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.SingleSubject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.widget.ImageView;

public class BlurringPlayerArtworkLoaderTest extends AndroidUnitTest {

    @Mock ImageOperations imageOperations;
    @Mock ImageView wrappedImageView;
    @Mock ImageView imageOverlayView;
    @Mock ImageListener listener;
    @Mock ViewVisibilityProvider viewVisibilityProvider;

    private PlayerArtworkLoader playerArtworkLoader;
    private Bitmap cachedBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);
    private Scheduler immediateScheduler = Schedulers.trampoline();
    private Urn urn = Urn.forTrack(1L);
    private ImageResource imageResource = SimpleImageResource.create(urn, Optional.absent());

    @Before
    public void setUp() throws Exception {
        playerArtworkLoader = new BlurringPlayerArtworkLoader(imageOperations,
                                                              resources(),
                                                              Schedulers.trampoline(),
                                                              Schedulers.trampoline());
    }

    @Test
    public void loadArtworkLoadsArtworkThroughImageOperations() {
        when(imageOperations.getCachedListItemBitmap(resources(), urn, Optional.absent())).thenReturn(cachedBitmap);
        when(imageOperations.blurredArtwork(resources(), urn, Optional.absent(), Optional.absent(), immediateScheduler, immediateScheduler)).thenReturn(Single.never());

        playerArtworkLoader.loadArtwork(imageResource,
                                        wrappedImageView,
                                        imageOverlayView,
                                        true,
                                        viewVisibilityProvider);

        verify(imageOperations).displayInPlayer(imageResource.getUrn(), imageResource.getImageUrlTemplate(), ApiImageSize.getFullImageSize(resources()), wrappedImageView, cachedBitmap, true);
    }

    @Test
    public void loadArtworkSetsBlurredArtworkOnImageOverlay() {
        final Bitmap blurredBitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565);
        when(imageOperations.getCachedListItemBitmap(resources(), urn, Optional.absent())).thenReturn(cachedBitmap);
        when(imageOperations.blurredArtwork(resources(), urn, Optional.absent(), Optional.absent(), immediateScheduler, immediateScheduler))
                .thenReturn(Single.just(blurredBitmap));

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
        when(imageOperations.getCachedListItemBitmap(resources(), urn, Optional.absent())).thenReturn(cachedBitmap);
        when(imageOperations.blurredArtwork(resources(), urn, Optional.absent(), Optional.absent(), immediateScheduler, immediateScheduler))
                .thenReturn(Single.just(blurredBitmap));
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
        SingleSubject<Bitmap> firstBlurObservable = SingleSubject.create();
        SingleSubject<Bitmap> secondBlurObservable = SingleSubject.create();

        when(imageOperations.getCachedListItemBitmap(resources(), urn, Optional.absent())).thenReturn(cachedBitmap);
        when(imageOperations.blurredArtwork(resources(), urn, Optional.absent(), Optional.absent(), immediateScheduler, immediateScheduler))
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
