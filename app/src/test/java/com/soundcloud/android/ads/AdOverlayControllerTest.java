package com.soundcloud.android.ads;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.fixtures.TestPropertySets.leaveBehindForPlayerWithDisplayMetaData;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.propeller.PropertySet;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

@RunWith(SoundCloudTestRunner.class)
public class AdOverlayControllerTest {

    private AdOverlayController controller;

    private View trackView;
    @Mock private ImageOperations imageOperations;
    @Mock private DeviceHelper deviceHelper;
    @Mock private AdOverlayController.AdOverlayListener listener;
    @Captor private ArgumentCaptor<ImageListener> imageListenerCaptor;

    @Before
    public void setUp() throws Exception {
        trackView = LayoutInflater.from(Robolectric.application).inflate(R.layout.player_track_page, mock(ViewGroup.class));
        AdOverlayController.Factory factory = new AdOverlayController.Factory(imageOperations,
                Robolectric.application, deviceHelper);
        controller = factory.create(trackView, listener);
        when(deviceHelper.getCurrentOrientation()).thenReturn(Configuration.ORIENTATION_PORTRAIT);
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

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
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

    private View getLeaveBehindClose() {
        return trackView.findViewById(R.id.leave_behind_close);
    }

    private void initializeAndShow(PropertySet properties) {
        controller.initialize(properties);
        controller.show();
    }

    private void expectLeaveBehindToBeGone() {
        expect(getLeaveBehind()).not.toBeClickable();
        expect(getLeaveBehindImage()).toBeGone();
        expect(getLeaveBehindClose()).toBeGone();
    }

    private void expectLeaveBehindToVisible() {
        expect(getLeaveBehind()).toBeClickable();
        expect(getLeaveBehindImage()).toBeVisible();
        expect(getLeaveBehindClose()).toBeVisible();
    }
}
