package com.soundcloud.android.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.res.Resources;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;

public class InterstitialPresenterTest extends AndroidUnitTest {

    private InterstitialAd interstitialData;
    private InterstitialPresenter presenter;

    @Mock AdOverlayPresenter.Listener listener;
    @Mock View trackView;
    @Mock private View overlay;
    @Mock private View closeView;
    @Mock private View imageHolder;
    @Mock private View trackImageHolder;
    @Mock private View previewContainer;
    @Mock private View header;
    @Mock private ImageView imageView;
    @Mock private ViewStub overlayStub;
    @Mock private ImageOperations imageOperations;
    @Mock private Resources resources;

    private TestEventBus eventBus;

    @Before
    public void setUp() {
        when(trackView.findViewById(R.id.interstitial_stub)).thenReturn(overlayStub);
        when(overlayStub.inflate()).thenReturn(overlay);
        when(overlay.findViewById(R.id.interstitial_close)).thenReturn(closeView);
        when(overlay.findViewById(R.id.interstitial_image)).thenReturn(imageView);
        when(overlay.findViewById(R.id.interstitial_image_holder)).thenReturn(imageHolder);
        when(trackView.findViewById(R.id.interstitial_image_holder)).thenReturn(trackImageHolder);
        when(trackView.findViewById(R.id.interstitial_preview_container)).thenReturn(previewContainer);
        when(overlay.findViewById(R.id.interstitial_header)).thenReturn(header);
        when(resources.getBoolean(R.bool.allow_interstitials)).thenReturn(true);

        eventBus = new TestEventBus();

        interstitialData = AdFixtures.getInterstitialAd(Urn.forTrack(123L));
        presenter = new InterstitialPresenter(trackView, listener, eventBus, imageOperations, resources);
    }

    @Test
    public void doesNotShowInterstitialIfInBackground() {
        final boolean shouldDisplay = presenter.shouldDisplayOverlay(interstitialData, true, true, false);
        assertThat(shouldDisplay).isFalse();
    }

    @Test
    public void doesNotShowInterstitialIfCollapsed() {
        final boolean shouldDisplay = presenter.shouldDisplayOverlay(interstitialData, false, true, true);
        assertThat(shouldDisplay).isFalse();
    }

    @Test
    public void doesNotShowInterstitialIfAlreadyDismissed() {
        interstitialData.setMetaAdDismissed();

        final boolean shouldDisplay = presenter.shouldDisplayOverlay(interstitialData, true, true, true);
        assertThat(shouldDisplay).isFalse();
    }

    @Test
    public void doesNotShowInterstitialIfResourcesDoesNotAllow() {
        when(resources.getBoolean(R.bool.allow_interstitials)).thenReturn(false);

        final boolean shouldDisplay = presenter.shouldDisplayOverlay(interstitialData, true, true, true);
        assertThat(shouldDisplay).isFalse();
    }

    @Test
    public void showsInterstitials() {
        final boolean shouldDisplay = presenter.shouldDisplayOverlay(interstitialData, true, true, true);
        assertThat(shouldDisplay).isTrue();
    }

    @Test
    public void isFullScreen() throws Exception {
        assertThat(presenter.isFullScreen()).isTrue();
    }

    @Test
    public void hidesPlayQueueWhenVisible() {
        final TrackQueueItem track = TestPlayQueueItem.createTrack(Urn.forTrack(123L));
        final TrackSourceInfo trackSourceInfo = new TrackSourceInfo("origin_screen", true);

        presenter.onAdVisible(track, interstitialData, trackSourceInfo);

        assertThat(eventBus.lastEventOn(EventQueue.PLAY_QUEUE_UI).isHideEvent()).isTrue();
    }
}
