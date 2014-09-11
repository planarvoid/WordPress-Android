package com.soundcloud.android.playback.ui;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.LeaveBehind;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

@RunWith(SoundCloudTestRunner.class)
public class LeaveBehindControllerTest {

    private LeaveBehindController controller;

    private View trackView;
    private Resources resources = Robolectric.application.getResources();
    private LeaveBehind data = new LeaveBehind("http://image.url/image.png", "http://link.url");

    @Mock private ImageOperations imageOperations;

    @Captor private ArgumentCaptor<ImageListener> imageListenerCaptor;

    @Before
    public void setUp() throws Exception {
        trackView = LayoutInflater.from(Robolectric.application).inflate(R.layout.player_track_page, mock(ViewGroup.class));
        LeaveBehindController.Factory factory = new LeaveBehindController.Factory(imageOperations, Robolectric.application, resources);
        controller = factory.create(trackView);
    }

    @Test
    public void dismissSetsLeaveBehindVisibilityToGone() {
        controller.setup(data);
        captureImageListener().onLoadingComplete(null, null, null);

        controller.dismiss();

        View leaveBehind = trackView.findViewById(R.id.leave_behind);
        expect(leaveBehind).toBeGone();
    }

    @Test
    public void leaveBehindGoneOnLeaveBehindCloseClick() {
        controller.setup(data);
        View close = trackView.findViewById(R.id.leave_behind_close);
        captureImageListener().onLoadingComplete(null, null, null);

        close.performClick();

        View leaveBehind = trackView.findViewById(R.id.leave_behind);
        expect(leaveBehind).toBeGone();
    }

    @Test
    public void leaveBehindIsVisibleAfterSetupWithSuccessfulImageLoad() {
        controller.setup(data);

        captureImageListener().onLoadingComplete(null, null, null);

        View leaveBehind = trackView.findViewById(R.id.leave_behind);
        expect(leaveBehind).toBeVisible();
    }

    @Test
    public void leaveBehindIsGoneAfterSetupIfImageNotLoaded() {
        controller.setup(data);
        View leaveBehind = trackView.findViewById(R.id.leave_behind);
        expect(leaveBehind).toBeGone();
    }

    @Test
    public void loadsLeaveBehindImageFromModel() {
        controller.setup(data);
        verify(imageOperations).displayLeaveBehind(eq(Uri.parse(data.getImageUrl())), any(ImageView.class), any(ImageListener.class));
    }

    @Test
    public void onClickLeaveBehindImageOpensUrl() {
        controller.setup(data);
        View leaveBehindImage = trackView.findViewById(R.id.leave_behind_image);
        captureImageListener().onLoadingComplete(null, null, null);

        leaveBehindImage.performClick();

        Intent intent = Robolectric.getShadowApplication().getNextStartedActivity();
        expect(intent).not.toBeNull();
        expect(intent.getAction()).toEqual(Intent.ACTION_VIEW);
        expect(intent.getData()).toEqual(Uri.parse(data.getLinkUrl()));
    }

    @Test
    public void onClickLeaveBehindImageDismissesLeaveBehind() {
        controller.setup(data);
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