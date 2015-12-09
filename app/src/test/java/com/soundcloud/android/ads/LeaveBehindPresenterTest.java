package com.soundcloud.android.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.events.AdOverlayEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;

public class LeaveBehindPresenterTest extends AndroidUnitTest {

    private LeaveBehindAd leaveBehind;
    private LeaveBehindPresenter presenter;

    @Mock AdOverlayPresenter.Listener listener;
    @Mock View trackView;
    @Mock private View overlay;
    @Mock private View headerView;
    @Mock private View imageViewHolder;
    @Mock private ImageView imageView;
    @Mock private ViewStub overlayStub;
    @Mock private ImageOperations imageOperations;
    private TestEventBus eventBus;

    @Before
    public void setUp() {
        eventBus = new TestEventBus();
        when(trackView.findViewById(R.id.leave_behind_stub)).thenReturn(overlayStub);
        when(overlayStub.inflate()).thenReturn(overlay);
        when(overlay.findViewById(R.id.leave_behind_header)).thenReturn(headerView);
        when(overlay.findViewById(R.id.leave_behind_image)).thenReturn(imageView);
        when(overlay.findViewById(R.id.leave_behind_image_holder)).thenReturn(imageViewHolder);

        presenter = new LeaveBehindPresenter(trackView, listener, eventBus, imageOperations);
    }

    @Test
    public void shouldNotShowLeaveBehindWhenAdWasClicked() {
        leaveBehind = AdFixtures.getLeaveBehindAd(Urn.forTrack(123L));
        leaveBehind.setMetaAdClicked();

        assertThat(presenter.shouldDisplayOverlay(leaveBehind, true, true, true)).isFalse();
    }

    @Test
    public void shouldNotShowLeaveBehindWhenAdWasNotCompleted() {
        leaveBehind = AdFixtures.getLeaveBehindAd(Urn.forTrack(123L));

        assertThat(presenter.shouldDisplayOverlay(leaveBehind, true, true, true)).isFalse();
    }

    @Test
    public void shouldNotShowTheLeaveBehindIfAlreadyShown() {
        leaveBehind = AdFixtures.getLeaveBehindAd(Urn.forTrack(123L));

        assertThat(presenter.shouldDisplayOverlay(leaveBehind, true, true, true)).isFalse();
    }

    @Test
    public void showsLeaveBehindWhenAdWasCompletedAndNotClicked() {
        leaveBehind = AdFixtures.getLeaveBehindAdWithDisplayMetaData(Urn.forTrack(123L));

        assertThat(presenter.shouldDisplayOverlay(leaveBehind, true, true, true)).isTrue();
    }

    @Test
    public void sendAnEventWhenVisible() {
        final TrackSourceInfo trackSourceInfo = new TrackSourceInfo("screen", false);
        final LeaveBehindAd leaveBehind = AdFixtures.getLeaveBehindAd(Urn.forTrack(123L));
        presenter.onAdVisible(TestPlayQueueItem.createTrack(Urn.forTrack(123L)), leaveBehind, trackSourceInfo);

        assertThat(eventBus.eventsOn(EventQueue.AD_OVERLAY)).hasSize(1);
        final AdOverlayEvent adOverlayEvent = eventBus.lastEventOn(EventQueue.AD_OVERLAY);
        assertThat(adOverlayEvent.getKind()).isEqualTo(AdOverlayEvent.SHOWN);
        assertThat(adOverlayEvent.getAdData()).isEqualTo(leaveBehind);
        assertThat(adOverlayEvent.getTrackSourceInfo()).isEqualTo(trackSourceInfo);
    }

    @Test
    public void sendAnEventWhenInVisible() {
        presenter.onAdNotVisible();

        assertThat(eventBus.eventsOn(EventQueue.AD_OVERLAY)).hasSize(1);
        assertThat(eventBus.lastEventOn(EventQueue.AD_OVERLAY).getKind()).isEqualTo(AdOverlayEvent.HIDDEN);
    }

    @Test
    public void isNotFullScreen() throws Exception {
        assertThat(presenter.isFullScreen()).isFalse();
    }
}
