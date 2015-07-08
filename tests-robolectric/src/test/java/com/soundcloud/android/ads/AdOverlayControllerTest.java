package com.soundcloud.android.ads;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.interstitialForPlayer;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.leaveBehindForPlayerWithDisplayMetaData;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

@RunWith(SoundCloudTestRunner.class)
public class AdOverlayControllerTest {

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
        trackView = LayoutInflater.from(Robolectric.application).inflate(R.layout.player_track_page, mock(ViewGroup.class));
        AdOverlayController.Factory factory = new AdOverlayController.Factory(imageOperations,
                context, deviceHelper, eventBus, playQueueManager, accountOperations);
        controller = factory.create(trackView, listener);
        when(deviceHelper.getCurrentOrientation()).thenReturn(Configuration.ORIENTATION_PORTRAIT);
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(Urn.forTrack(123L));
        when(playQueueManager.getCurrentMetaData()).thenReturn(TestPropertySets.leaveBehindForPlayer());
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo("origin_screen", true));
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(456L));
        when(context.getResources()).thenReturn(resources);
    }

    @Test
    public void dismissSetsLeaveBehindVisibilityToGone() {
        initializeAndShow(leaveBehindForPlayerWithDisplayMetaData());
        captureImageListener().onLoadingComplete(null, null, null);

        controller.clear();

        expectLeaveBehindToBeGone();
    }

    @Test
    public void leaveBehindGoneOnLeaveBehindCloseClick() {
        initializeAndShow(leaveBehindForPlayerWithDisplayMetaData());
        captureImageListener().onLoadingComplete(null, null, null);

        controller.onCloseButtonClick();

        expectLeaveBehindToBeGone();
    }

    @Test
    public void leaveBehindIsVisibleAfterSetupWithSuccessfulImageLoad() {
        initializeAndShow(leaveBehindForPlayerWithDisplayMetaData());

        captureImageListener().onLoadingComplete(null, null, null);

        expectLeaveBehindToVisible();
    }

    @Test
    public void leaveBehindNeverBecomesVisibleIfDismissedBeforeImageLoads() {
        initializeAndShow(leaveBehindForPlayerWithDisplayMetaData());

        controller.clear();
        captureImageListener().onLoadingComplete(null, null, null);

        expectLeaveBehindToBeGone();
    }

    @Test
    public void leaveBehindIsGoneAfterSetupIfImageNotLoaded() {
        initializeAndShow(leaveBehindForPlayerWithDisplayMetaData());

        expectLeaveBehindToBeGone();
    }

    @Test
    public void leaveBehindIsGoneIfImageLoadingFails() {
        initializeAndShow(leaveBehindForPlayerWithDisplayMetaData());

        captureImageListener().onLoadingFailed(null, null, null);

        expectLeaveBehindToBeGone();
    }

    @Test
    public void recordsOverlayDismissal() {
        final PropertySet properties = leaveBehindForPlayerWithDisplayMetaData();
        controller.initialize(properties);
        controller.show();

        controller.onCloseButtonClick();

        expect(properties.getOrElse(AdOverlayProperty.META_AD_DISMISSED, false)).toBeTrue();
    }

    @Test
    public void loadsLeaveBehindImageFromModel() {
        final PropertySet properties = leaveBehindForPlayerWithDisplayMetaData();
        controller.initialize(properties);
        controller.show();
        verify(imageOperations).displayLeaveBehind(eq(Uri.parse(properties.get(LeaveBehindProperty.IMAGE_URL))), any(ImageView.class), any(ImageListener.class));
    }

    @Test
    public void setupOnLandscapeOrientationDoesNotDisplayLeaveBehind() {
        when(deviceHelper.getCurrentOrientation()).thenReturn(Configuration.ORIENTATION_LANDSCAPE);
        controller.initialize(leaveBehindForPlayerWithDisplayMetaData());
        verify(imageOperations, never()).displayLeaveBehind(any(Uri.class), any(ImageView.class), any(ImageListener.class));
    }

    @Test
    public void onClickLeaveBehindImageOpensUrl() {
        final PropertySet properties = leaveBehindForPlayerWithDisplayMetaData();
        controller.initialize(properties);
        controller.show();
        captureImageListener().onLoadingComplete(null, null, null);

        controller.onImageClick();

        ArgumentCaptor<Intent> intentArgumentCaptor = ArgumentCaptor.forClass(Intent.class);

        verify(context).startActivity(intentArgumentCaptor.capture());

        Intent intent = intentArgumentCaptor.getValue();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(Intent.ACTION_VIEW);
        expect(intent.getData()).toEqual(properties.get(LeaveBehindProperty.CLICK_THROUGH_URL));
    }

    @Test
    public void onClickLeaveBehindImageDismissesLeaveBehind() {
        initializeAndShow(leaveBehindForPlayerWithDisplayMetaData());
        captureImageListener().onLoadingComplete(null, null, null);

        controller.onImageClick();

        expectLeaveBehindToBeGone();
    }

    @Test
    public void onClickLeaveBehindImageSendTrackingEvent() {
        initializeAndShow(leaveBehindForPlayerWithDisplayMetaData());
        controller.show();

        controller.onImageClick();

        expect(eventBus.eventsOn(EventQueue.TRACKING)).toNumber(1);
        expect(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).toBe(AdOverlayTrackingEvent.KIND_CLICK);
    }

    @Test
    public void isNotVisibleReturnsTrueIfPresenterIsNull() throws Exception {
        expect(controller.isNotVisible()).toBeTrue();
    }

    @Test
    public void isNotVisibleReturnsTrueIfLeaveBehindImageHasNotLoaded() throws Exception {
        controller.initialize(leaveBehindForPlayerWithDisplayMetaData());
        controller.show();

        expect(controller.isNotVisible()).toBeTrue();
    }

    @Test
    public void isNotVisibleReturnsFalseIfLeaveBehindImageHasLoaded() throws Exception {
        controller.initialize(leaveBehindForPlayerWithDisplayMetaData());
        controller.show();

        captureImageListener().onLoadingComplete(null, null, null);

        expect(controller.isNotVisible()).toBeFalse();
    }

    @Test
    public void isNotVisibleReturnsTrueIfInterstitialImageHasNotLoaded() throws Exception {
        controller.initialize(interstitialForPlayer());
        controller.show();

        expect(controller.isNotVisible()).toBeTrue();
    }

    @Test
    public void isNotVisibleReturnsFalseIfInterstitialImageHasLoaded() throws Exception {
        when(resources.getBoolean(R.bool.allow_interstitials)).thenReturn(true);
        controller.initialize(interstitialForPlayer());
        controller.setExpanded();
        controller.show(true);

        captureImageListener().onLoadingComplete(null, null, null);

        expect(controller.isNotVisible()).toBeFalse();
    }

    @Test
    public void isVisibleInFullscreenReturnsFalseIfLeaveBehindImageHasLoaded() throws Exception {
        controller.initialize(leaveBehindForPlayerWithDisplayMetaData());
        controller.show();

        captureImageListener().onLoadingComplete(null, null, null);

        expect(controller.isVisibleInFullscreen()).toBeFalse();
    }

    @Test
    public void isVisibleInFullscreenReturnsTrueIfInterstitialImageHasLoaded() throws Exception {
        when(resources.getBoolean(R.bool.allow_interstitials)).thenReturn(true);
        controller.initialize(interstitialForPlayer());
        controller.setExpanded();
        controller.show(true);

        captureImageListener().onLoadingComplete(null, null, null);

        expect(controller.isVisibleInFullscreen()).toBeTrue();
    }

    private ImageListener captureImageListener() {
        verify(imageOperations).displayLeaveBehind(any(Uri.class), any(ImageView.class), imageListenerCaptor.capture());
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

    private void initializeAndShow(PropertySet properties) {
        controller.initialize(properties);
        controller.show();
    }

    private void expectLeaveBehindToBeGone() {
        expect(getLeaveBehind()).not.toBeClickable();
        expect(getLeaveBehindImage()).toBeGone();
        expect(getLeaveBehindHeader()).toBeGone();
    }

    private void expectLeaveBehindToVisible() {
        expect(getLeaveBehind()).toBeClickable();
        expect(getLeaveBehindImage()).toBeVisible();
        expect(getLeaveBehindHeader()).toBeVisible();
    }
}
