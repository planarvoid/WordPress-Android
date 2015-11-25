package com.soundcloud.android.ads;

import static org.assertj.android.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.DeviceHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class AdOverlayControllerTest extends AndroidUnitTest {

    private AdOverlayController controller;

    private View trackView;
    private TestEventBus eventBus;
    @Mock private ImageOperations imageOperations;
    @Mock private DeviceHelper deviceHelper;
    @Mock private AdOverlayController.AdOverlayListener listener;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private AccountOperations accountOperations;
    @Mock private Context context;
    @Mock private Resources resources;
    @Captor private ArgumentCaptor<ImageListener> imageListenerCaptor;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        trackView = LayoutInflater.from(context()).inflate(R.layout.player_track_page, new FrameLayout(context()), false);
        AdOverlayController.Factory factory = new AdOverlayController.Factory(imageOperations,
                context, deviceHelper, eventBus, playQueueManager, accountOperations);
        controller = factory.create(trackView, listener);

        when(deviceHelper.getCurrentOrientation()).thenReturn(Configuration.ORIENTATION_PORTRAIT);
        when(playQueueManager.getCurrentPlayQueueItem())
                .thenReturn(TestPlayQueueItem.createTrack(Urn.forTrack(123L), AdFixtures.getLeaveBehindAd(Urn.forTrack(123L))));
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo("origin_screen", true));
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(456L));
        when(context.getResources()).thenReturn(resources);
    }

    @Test
    public void dismissSetsLeaveBehindVisibilityToGone() {
        initializeAndShow(AdFixtures.getLeaveBehindAdWithDisplayMetaData(Urn.forTrack(123L)));
        captureImageListener().onLoadingComplete(null, null, null);

        controller.clear();

        expectLeaveBehindToBeGone();
    }

    @Test
    public void leaveBehindGoneOnLeaveBehindCloseClick() {
        initializeAndShow(AdFixtures.getLeaveBehindAdWithDisplayMetaData(Urn.forTrack(123L)));
        captureImageListener().onLoadingComplete(null, null, null);

        controller.onCloseButtonClick();

        expectLeaveBehindToBeGone();
    }

    @Test
    public void leaveBehindIsVisibleAfterSetupWithSuccessfulImageLoad() {
        initializeAndShow(AdFixtures.getLeaveBehindAdWithDisplayMetaData(Urn.forTrack(123L)));

        captureImageListener().onLoadingComplete(null, null, null);

        expectLeaveBehindToVisible();
    }

    @Test
    public void leaveBehindNeverBecomesVisibleIfDismissedBeforeImageLoads() {
        initializeAndShow(AdFixtures.getLeaveBehindAdWithDisplayMetaData(Urn.forTrack(123L)));

        controller.clear();
        captureImageListener().onLoadingComplete(null, null, null);

        expectLeaveBehindToBeGone();
    }

    @Test
    public void leaveBehindIsGoneAfterSetupIfImageNotLoaded() {
        initializeAndShow(AdFixtures.getLeaveBehindAdWithDisplayMetaData(Urn.forTrack(123L)));

        expectLeaveBehindToBeGone();
    }

    @Test
    public void leaveBehindIsGoneIfImageLoadingFails() {
        initializeAndShow(AdFixtures.getLeaveBehindAdWithDisplayMetaData(Urn.forTrack(123L)));

        captureImageListener().onLoadingFailed(null, null, null);

        expectLeaveBehindToBeGone();
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
    public void loadsLeaveBehindImageFromModel() {
        final LeaveBehindAd leaveBehind = AdFixtures.getLeaveBehindAdWithDisplayMetaData(Urn.forTrack(123L));
        controller.initialize(leaveBehind);
        controller.show();
        verify(imageOperations).displayLeaveBehind(Matchers.eq(Uri.parse(leaveBehind.getImageUrl())), Matchers.any(ImageView.class), Matchers.any(ImageListener.class));
    }

    @Test
    public void setupOnLandscapeOrientationDoesNotDisplayLeaveBehind() {
        when(deviceHelper.getCurrentOrientation()).thenReturn(Configuration.ORIENTATION_LANDSCAPE);
        controller.initialize(AdFixtures.getLeaveBehindAdWithDisplayMetaData(Urn.forTrack(123L)));
        verify(imageOperations, Mockito.never()).displayLeaveBehind(Matchers.any(Uri.class), Matchers.any(ImageView.class), Matchers.any(ImageListener.class));
    }

    @Test
    public void onClickLeaveBehindImageOpensUrl() {
        final LeaveBehindAd leaveBehind = AdFixtures.getLeaveBehindAdWithDisplayMetaData(Urn.forTrack(123L));
        controller.initialize(leaveBehind);
        controller.show();
        captureImageListener().onLoadingComplete(null, null, null);

        controller.onImageClick();

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);

        verify(context).startActivity(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        assertThat(intent).isNotNull();
        assertThat(intent.getAction()).isEqualTo(Intent.ACTION_VIEW);
        assertThat(intent.getData()).isEqualTo(leaveBehind.getClickthroughUrl());
    }

    @Test
    public void onClickLeaveBehindImageDismissesLeaveBehind() {
        initializeAndShow(AdFixtures.getLeaveBehindAdWithDisplayMetaData(Urn.forTrack(123L)));
        captureImageListener().onLoadingComplete(null, null, null);

        controller.onImageClick();

        expectLeaveBehindToBeGone();
    }

    @Test
    public void onClickLeaveBehindImageSendTrackingEvent() {
        initializeAndShow(AdFixtures.getLeaveBehindAdWithDisplayMetaData(Urn.forTrack(123L)));
        controller.show();

        controller.onImageClick();

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).isSameAs(AdOverlayTrackingEvent.KIND_CLICK);
    }

    @Test
    public void isNotVisibleReturnsTrueIfPresenterIsNull() throws Exception {
        assertThat(controller.isNotVisible()).isTrue();
    }

    @Test
    public void isNotVisibleReturnsTrueIfLeaveBehindImageHasNotLoaded() throws Exception {
        controller.initialize(AdFixtures.getLeaveBehindAdWithDisplayMetaData(Urn.forTrack(123L)));
        controller.show();

        assertThat(controller.isNotVisible()).isTrue();
    }

    @Test
    public void isNotVisibleReturnsFalseIfLeaveBehindImageHasLoaded() throws Exception {
        controller.initialize(AdFixtures.getLeaveBehindAdWithDisplayMetaData(Urn.forTrack(123L)));
        controller.show();

        captureImageListener().onLoadingComplete(null, null, null);

        assertThat(controller.isNotVisible()).isFalse();
    }

    @Test
    public void isNotVisibleReturnsTrueIfInterstitialImageHasNotLoaded() throws Exception {
        controller.initialize(AdFixtures.getInterstitialAd(Urn.forTrack(123L)));
        controller.show();

        assertThat(controller.isNotVisible()).isTrue();
    }

    @Test
    public void isNotVisibleReturnsFalseIfInterstitialImageHasLoaded() throws Exception {
        when(resources.getBoolean(R.bool.allow_interstitials)).thenReturn(true);
        controller.initialize(AdFixtures.getInterstitialAd(Urn.forTrack(123L)));
        controller.setExpanded();
        controller.show(true);

        captureImageListener().onLoadingComplete(null, null, null);

        assertThat(controller.isNotVisible()).isFalse();
    }

    @Test
    public void isVisibleInFullscreenReturnsFalseIfLeaveBehindImageHasLoaded() throws Exception {
        controller.initialize(AdFixtures.getLeaveBehindAdWithDisplayMetaData(Urn.forTrack(123L)));
        controller.show();

        captureImageListener().onLoadingComplete(null, null, null);

        assertThat(controller.isVisibleInFullscreen()).isFalse();
    }

    @Test
    public void isVisibleInFullscreenReturnsTrueIfInterstitialImageHasLoaded() throws Exception {
        when(resources.getBoolean(R.bool.allow_interstitials)).thenReturn(true);
        controller.initialize(AdFixtures.getInterstitialAd(Urn.forTrack(123L)));
        controller.setExpanded();
        controller.show(true);

        captureImageListener().onLoadingComplete(null, null, null);

        assertThat(controller.isVisibleInFullscreen()).isTrue();
    }

    private ImageListener captureImageListener() {
        verify(imageOperations).displayLeaveBehind(Matchers.any(Uri.class), Matchers.any(ImageView.class), imageListenerCaptor.capture());
        return imageListenerCaptor.getValue();
    }

    private View getLeaveBehind() {
        return trackView.findViewById(R.id.leave_behind);
    }

    private View getLeaveBehindImage() {
        return trackView.findViewById(R.id.leave_behind_image);
    }

    private View getLeaveBehindHeader() {
        return trackView.findViewById(R.id.leave_behind_header);
    }

    private void initializeAndShow(LeaveBehindAd leaveBehind) {
        controller.initialize(leaveBehind);
        controller.show();
    }

    private void expectLeaveBehindToBeGone() {
        assertThat(getLeaveBehind()).isNotClickable();
        assertThat(getLeaveBehindImage()).isGone();
        assertThat(getLeaveBehindHeader()).isGone();
    }

    private void expectLeaveBehindToVisible() {
        assertThat(getLeaveBehind()).isClickable();
        assertThat(getLeaveBehindImage()).isVisible();
        assertThat(getLeaveBehindHeader()).isVisible();
    }
}
