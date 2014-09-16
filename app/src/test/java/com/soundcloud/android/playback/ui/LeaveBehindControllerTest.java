package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.LeaveBehindProperty;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
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

import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

@RunWith(SoundCloudTestRunner.class)
public class LeaveBehindControllerTest {

    private LeaveBehindController controller;

    private View trackView;
    private PropertySet leaveBehindProperties;
    @Mock private ImageOperations imageOperations;
    @Mock private DeviceHelper deviceHelper;
    @Captor private ArgumentCaptor<ImageListener> imageListenerCaptor;

    @Before
    public void setUp() throws Exception {
        leaveBehindProperties = TestPropertySets.leaveBehindForPlayer();
        trackView = LayoutInflater.from(Robolectric.application).inflate(R.layout.player_track_page, mock(ViewGroup.class));
        LeaveBehindController.Factory factory = new LeaveBehindController.Factory(imageOperations,
                Robolectric.application, deviceHelper);
        controller = factory.create(trackView);
        when(deviceHelper.getCurrentOrientation()).thenReturn(Configuration.ORIENTATION_PORTRAIT);
    }

    @Test
    public void dismissSetsLeaveBehindVisibilityToGone() {
        controller.initialize(leaveBehindProperties);
        controller.show();
        captureImageListener().onLoadingComplete(null, null, null);

        controller.clear();

        View leaveBehind = trackView.findViewById(R.id.leave_behind);
        expect(leaveBehind).toBeGone();
    }

    @Test
    public void leaveBehindGoneOnLeaveBehindCloseClick() {
        controller.initialize(leaveBehindProperties);
        controller.show();
        View close = trackView.findViewById(R.id.leave_behind_close);
        captureImageListener().onLoadingComplete(null, null, null);

        close.performClick();

        View leaveBehind = trackView.findViewById(R.id.leave_behind);
        expect(leaveBehind).toBeGone();
    }

    @Test
    public void leaveBehindIsVisibleAfterSetupWithSuccessfulImageLoad() {
        controller.initialize(leaveBehindProperties);
        controller.show();

        captureImageListener().onLoadingComplete(null, null, null);

        View leaveBehind = trackView.findViewById(R.id.leave_behind);
        expect(leaveBehind).toBeVisible();
    }

    @Test
    public void leaveBehindNeverBecomesVisibleIfDismissedBeforeImageLoads() {
        controller.initialize(leaveBehindProperties);
        controller.show();

        controller.clear();
        captureImageListener().onLoadingComplete(null, null, null);

        View leaveBehind = trackView.findViewById(R.id.leave_behind);
        expect(leaveBehind).toBeGone();
    }

    @Test
    public void leaveBehindIsGoneAfterSetupIfImageNotLoaded() {
        controller.initialize(leaveBehindProperties);
        View leaveBehind = trackView.findViewById(R.id.leave_behind);
        expect(leaveBehind).toBeGone();
    }

    @Test
    public void loadsLeaveBehindImageFromModel() {
        controller.initialize(leaveBehindProperties);
        controller.show();
        verify(imageOperations).displayLeaveBehind(eq(Uri.parse(leaveBehindProperties.get(LeaveBehindProperty.IMAGE_URL))), any(ImageView.class), any(ImageListener.class));
    }

    @Test
    public void setupOnLandscapeOrientationDoesNotDisplayLeaveBehind() {
        when(deviceHelper.getCurrentOrientation()).thenReturn(Configuration.ORIENTATION_LANDSCAPE);
        controller.initialize(leaveBehindProperties);
        verify(imageOperations, never()).displayLeaveBehind(any(Uri.class), any(ImageView.class), any(ImageListener.class));
    }

    @Test
    public void onClickLeaveBehindImageOpensUrl() {
        controller.initialize(leaveBehindProperties);
        controller.show();
        View leaveBehindImage = trackView.findViewById(R.id.leave_behind_image);
        captureImageListener().onLoadingComplete(null, null, null);

        leaveBehindImage.performClick();

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(Intent.ACTION_VIEW);
        expect(intent.getData()).toEqual(leaveBehindProperties.get(LeaveBehindProperty.CLICK_THROUGH_URL));
    }

    @Test
    public void onClickLeaveBehindImageDismissesLeaveBehind() {
        controller.initialize(leaveBehindProperties);
        controller.show();
        View leaveBehindImage = trackView.findViewById(R.id.leave_behind_image);
        captureImageListener().onLoadingComplete(null, null, null);

        leaveBehindImage.performClick();

        View leaveBehind = trackView.findViewById(R.id.leave_behind);
        expect(leaveBehind).toBeGone();
    }

    private ImageListener captureImageListener() {
        verify(imageOperations).displayLeaveBehind(any(Uri.class), any(ImageView.class), imageListenerCaptor.capture());
        return imageListenerCaptor.getValue();
    }

}