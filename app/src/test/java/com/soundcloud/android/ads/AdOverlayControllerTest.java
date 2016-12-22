package com.soundcloud.android.ads;

import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.TrackQueueItem;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.robolectric.shadows.RoboLayoutInflater;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ImageView;

public class AdOverlayControllerTest extends AndroidUnitTest {

    private final TrackQueueItem trackQueueItem = TestPlayQueueItem.createTrack(Urn.forTrack(123L),
                                                                                AdFixtures.getLeaveBehindAd(Urn.forTrack(
                                                                                        123L)));
    private final TrackSourceInfo trackSourceInfo = new TrackSourceInfo("origin_screen", true);
    private AdOverlayController controller;

    private View trackView;
    private TestEventBus eventBus;

    @Mock private DeviceHelper deviceHelper;
    @Mock private AdOverlayController.AdOverlayListener listener;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private AccountOperations accountOperations;
    @Mock private Context context;
    @Mock private ImageView imageView;
    @Mock private Resources resources;
    @Mock private InterstitialPresenterFactory interstitialPresenterFactory;
    @Mock private LeaveBehindPresenterFactory leaveBehindPresenterFactory;
    @Mock private InterstitialPresenter interstitialPresenter;
    @Mock private LeaveBehindPresenter leaveBehindPresenter;
    @Mock private AdViewabilityController adViewabilityController;

    @Captor private ArgumentCaptor<ImageListener> imageListenerCaptor;
    @Captor private ArgumentCaptor<AdOverlayPresenter.Listener> adOverlayListenerCaptor;

    private InterstitialAd interstitialData;
    private LeaveBehindAd leaveBehindData;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        FragmentActivity context = activity();
        trackView = RoboLayoutInflater.from(context).inflate(
                R.layout.player_track_page,
                null);

        controller = new AdOverlayController(trackView,
                                             listener,
                                             this.context,
                                             deviceHelper,
                                             eventBus,
                                             playQueueManager,
                                             accountOperations,
                                             interstitialPresenterFactory,
                                             leaveBehindPresenterFactory,
                                             adViewabilityController);
        when(deviceHelper.getCurrentOrientation()).thenReturn(Configuration.ORIENTATION_PORTRAIT);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(trackQueueItem);
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(trackSourceInfo);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(456L));
        when(this.context.getResources()).thenReturn(resources);

        interstitialData = AdFixtures.getInterstitialAd(Urn.forTrack(123L));
        leaveBehindData = AdFixtures.getLeaveBehindAdWithDisplayMetaData(Urn.forTrack(123L));

        when(interstitialPresenterFactory.create(same(trackView), any(AdOverlayPresenter.Listener.class)))
                .thenReturn(interstitialPresenter);
        when(leaveBehindPresenterFactory.create(same(trackView), any(AdOverlayPresenter.Listener.class)))
                .thenReturn(leaveBehindPresenter);
        when(interstitialPresenter.getImageView()).thenReturn(imageView);
    }

    @Test
    public void dismissSetsLeaveBehindVisibilityToGone() {
        initializeLeaveBehindAndShow();
        captureLeaveBehindListener().onCloseButtonClick();
        verify(leaveBehindPresenter).clear();
    }

    @Test
    public void leaveBehindOnAdVisibleCalledAfterSetupWithSuccessfulImageLoad() {
        initializeLeaveBehindAndShow();

        captureLeaveBehindListener().onAdImageLoaded();

        verify(leaveBehindPresenter).onAdVisible(trackQueueItem, leaveBehindData, trackSourceInfo);
    }

    @Test
    public void leaveBehindOnAdVisibleNotCalledIfDismissedBeforeImageLoads() {
        initializeLeaveBehindAndShow();

        controller.clear();

        captureLeaveBehindListener().onAdImageLoaded();

        verify(leaveBehindPresenter, never()).onAdVisible(any(TrackQueueItem.class),
                                                          any(OverlayAdData.class),
                                                          any(TrackSourceInfo.class));
    }

    @Test
    public void recordsOverlayDismissal() {
        final LeaveBehindAd leaveBehind = AdFixtures.getLeaveBehindAdWithDisplayMetaData(Urn.forTrack(123L));
        controller.initialize(leaveBehind);
        controller.show();

        controller.onCloseButtonClick();

        assertThat(leaveBehind.isMetaAdDismissed()).isTrue();
    }

    @Test
    public void setupOnLandscapeOrientationDoesNotDisplayLeaveBehind() {
        when(deviceHelper.getCurrentOrientation()).thenReturn(Configuration.ORIENTATION_LANDSCAPE);

        initializeLeaveBehindAndShow();

        verify(leaveBehindPresenter, Mockito.never()).bind(any(OverlayAdData.class));
    }

    @Test
    public void onClickLeaveBehindImageOpensUrl() {
        initializeLeaveBehindAndShow();
        captureLeaveBehindListener().onImageClick();

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);

        verify(context).startActivity(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertThat(intent).isNotNull();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(intent.getData()).isEqualTo(leaveBehindData.getClickthroughUrl());
    }

    @Test
    public void onClickLeaveBehindImageClearsLeaveBehind() {
        initializeLeaveBehindAndShow();

        captureLeaveBehindListener().onImageClick();

        verify(leaveBehindPresenter).clear();
    }

    @Test
    public void onClickLeaveBehindImageSendTrackingEvent() {
        initializeLeaveBehindAndShow();
        captureLeaveBehindListener().onImageClick();

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).isSameAs(AdOverlayTrackingEvent.KIND_CLICK);
    }

    @Test
    public void isNotVisibleReturnsTrueIfPresenterIsNull() throws Exception {
        assertThat(controller.isNotVisible()).isTrue();
    }

    @Test
    public void isNotVisibleReturnsValueFromInterstitialPresenter() throws Exception {
        initializeLeaveBehindAndShow();

        when(leaveBehindPresenter.isNotVisible()).thenReturn(true);

        assertThat(controller.isNotVisible()).isTrue();
    }

    @Test
    public void isVisibleInFullscreenReturnsFalseIfPresenterNotVisibleAndFullscreen() throws Exception {
        initializeLeaveBehindAndShow();

        when(leaveBehindPresenter.isFullScreen()).thenReturn(true);
        when(leaveBehindPresenter.isNotVisible()).thenReturn(true);

        assertThat(controller.isVisibleInFullscreen()).isFalse();
    }

    @Test
    public void isVisibleInFullscreenReturnsFalseIfPresenterVisibleAndNotFullscreen() throws Exception {
        initializeLeaveBehindAndShow();

        assertThat(controller.isVisibleInFullscreen()).isFalse();
    }

    @Test
    public void isVisibleInFullscreenReturnsTrueIfPresenterIsVisibleAndFullscreen() throws Exception {
        initializeLeaveBehindAndShow();

        when(leaveBehindPresenter.isFullScreen()).thenReturn(true);

        assertThat(controller.isVisibleInFullscreen()).isTrue();
    }

    @Test
    public void onAdVisibleCalledOnAdVisibleOnInterstitialPresenter() {
        controller.initialize(interstitialData);
        controller.onAdImageLoaded();

        verify(interstitialPresenter).onAdVisible(trackQueueItem, interstitialData, trackSourceInfo);
    }

    @Test
    public void onAdVisibleCalledOnAdVisibleOnLeaveBehindPresenter() {
        controller.initialize(leaveBehindData);
        controller.onAdImageLoaded();

        verify(leaveBehindPresenter).onAdVisible(trackQueueItem, leaveBehindData, trackSourceInfo);
    }

    @Test
    public void onAdVisibleNotCalledOnInterstitialPresenterItCurrentItemNotTrack() {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PlayQueueItem.EMPTY);
        controller.initialize(interstitialData);
        controller.onAdImageLoaded();

        verify(interstitialPresenter, never()).onAdVisible(any(PlayQueueItem.class),
                                                           any(OverlayAdData.class),
                                                           any(TrackSourceInfo.class));
    }

    @Test
    public void onAdVisibleNotCalledOnLeaveBehindPresenterIfCurrentItemNotTrack() {
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PlayQueueItem.EMPTY);
        controller.initialize(leaveBehindData);
        controller.onAdImageLoaded();

        verify(interstitialPresenter, never()).onAdVisible(any(PlayQueueItem.class),
                                                           any(OverlayAdData.class),
                                                           any(TrackSourceInfo.class));
    }

    @Test
    public void adViewabilityControllerTracksOverlayImpressionWhenShouldDisplayOverlayIsTrue() {
        when(interstitialPresenter.shouldDisplayOverlay(interstitialData, true, true, true)).thenReturn(true);

        controller.initialize(interstitialData);
        controller.setExpanded();
        controller.show(true);

        verify(adViewabilityController).startOverlayTracking(imageView, interstitialData);
    }

    @Test
    public void adViewabilityControllerDoesntTrackOverlayImpressionWhenShouldDisplayOverlayIsFalse() {
        when(interstitialPresenter.shouldDisplayOverlay(interstitialData, true, true, true)).thenReturn(false);

        controller.initialize(interstitialData);
        controller.setExpanded();
        controller.show(true);

        verify(adViewabilityController, never()).startOverlayTracking(any(ImageView.class), eq(interstitialData));
    }

    @Test
    public void adViewabilityControllerStopsTrackingOnClear() {
        when(interstitialPresenter.shouldDisplayOverlay(interstitialData, true, true, true)).thenReturn(true);

        controller.initialize(interstitialData);
        controller.clear();

        verify(adViewabilityController).stopOverlayTracking();
    }

    private AdOverlayPresenter.Listener captureLeaveBehindListener() {
        verify(leaveBehindPresenterFactory).create(same(trackView), adOverlayListenerCaptor.capture());
        return adOverlayListenerCaptor.getValue();
    }

    private void initializeLeaveBehindAndShow() {
        controller.initialize(leaveBehindData);
        controller.show();
    }
}
