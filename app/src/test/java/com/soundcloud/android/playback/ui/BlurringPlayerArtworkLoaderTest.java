package com.soundcloud.android.playback.ui;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.TestObservables;
import com.soundcloud.android.tracks.TrackUrn;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.widget.ImageView;

@RunWith(SoundCloudTestRunner.class)
public class BlurringPlayerArtworkLoaderTest {

    @Mock ImageOperations imageOperations;
    @Mock ImageView wrappedImageView;
    @Mock ImageView imageOverlayView;
    @Mock ImageListener listener;
    @Mock Subscription subscription;

    private PlayerArtworkLoader playerArtworkLoader;
    private Resources resources = Robolectric.application.getResources();
    private TrackUrn urn = Urn.forTrack(123L);
    private Bitmap cachedBitmap = Bitmap.createBitmap(0, 0, Bitmap.Config.RGB_565);

    @Before
    public void setUp() throws Exception {
        playerArtworkLoader = new BlurringPlayerArtworkLoader(imageOperations, Robolectric.application.getResources(), Schedulers.immediate());
    }

    @Test
    public void loadArtworkLoadsArtworkThroughImageOperations() throws Exception {
        when(imageOperations.getCachedListItemBitmap(resources, urn)).thenReturn(cachedBitmap);
        when(imageOperations.blurredPlayerArtwork(resources, urn)).thenReturn(Observable.<Bitmap>empty());

        playerArtworkLoader.loadArtwork(urn, wrappedImageView, imageOverlayView, listener, true);

        verify(imageOperations).displayInPlayer(urn, ApiImageSize.getFullImageSize(Robolectric.application.getResources()),
                wrappedImageView, listener, cachedBitmap, true);
    }

    @Test
    public void loadArtworkSetsBlurredArtworkOnImageOverlay() throws Exception {
        final Bitmap blurredBitmap = Bitmap.createBitmap(0, 0, Bitmap.Config.RGB_565);
        when(imageOperations.getCachedListItemBitmap(resources, urn)).thenReturn(cachedBitmap);
        when(imageOperations.blurredPlayerArtwork(resources, urn)).thenReturn(Observable.just(blurredBitmap));

        playerArtworkLoader.loadArtwork(urn, wrappedImageView, imageOverlayView, listener, true);

        verify(imageOverlayView).setImageBitmap(blurredBitmap);

    }

    @Test
    public void loadArtworkUnsubscribesFromOldBlurringOperations() throws Exception {
        when(imageOperations.getCachedListItemBitmap(resources, urn)).thenReturn(cachedBitmap);
        when(imageOperations.blurredPlayerArtwork(resources, urn)).thenReturn(TestObservables.<Bitmap>endlessObservablefromSubscription(subscription));

        playerArtworkLoader.loadArtwork(urn, wrappedImageView, imageOverlayView, listener, true);
        playerArtworkLoader.loadArtwork(urn, wrappedImageView, imageOverlayView, listener, true);

        verify(subscription).unsubscribe();

    }
}