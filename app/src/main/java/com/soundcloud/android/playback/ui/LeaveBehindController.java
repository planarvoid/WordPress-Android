package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.image.ImageOperations;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;

import javax.inject.Inject;

public class LeaveBehindController {

    private static final Uri TEST_LEAVE_BEHIND = Uri.parse("https://cloud.githubusercontent.com/assets/283794/4220512/91526122-3901-11e4-8092-20db3b1ff853.jpg");

    private final ImageOperations imageOperations;
    private final Resources resources;

    public LeaveBehindController(ImageOperations imageOperations, Resources resources) {
        this.imageOperations = imageOperations;
        this.resources = resources;
    }

    public void show(final View trackView) {
        View leaveBehind = showLeaveBehind(trackView);
        View close = leaveBehind.findViewById(R.id.leave_behind_close);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hide(trackView);
            }
        });
        ImageView leaveBehindImage = (ImageView) leaveBehind.findViewById(R.id.leave_behind_image);

        ImageListener failureListener = new ImageListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {}

            @Override
            public void onLoadingFailed(String imageUri, View view, String failedReason) {
                hide(trackView);
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {

            }
        };

        imageOperations.displayLeaveBehind(TEST_LEAVE_BEHIND, leaveBehindImage, failureListener, resources.getDrawable(R.drawable.placeholder));

    }

    private View showLeaveBehind(View trackView) {
        final View leaveBehind = trackView.findViewById(R.id.leave_behind);
        if (leaveBehind == null) {
            ViewStub stub = (ViewStub) trackView.findViewById(R.id.leave_behind_stub);
            return stub.inflate();
        } else {
            leaveBehind.setVisibility(View.VISIBLE);
            return leaveBehind;
        }
    }

    public void hide(View trackView) {
        final View leaveBehind = trackView.findViewById(R.id.leave_behind);
        leaveBehind.setVisibility(View.GONE);
    }

    static class Factory {
        private final ImageOperations imageOperations;
        private final Resources resources;

        @Inject
        Factory(ImageOperations imageOperations,
                Resources resources) {
            this.imageOperations = imageOperations;
            this.resources = resources;
        }

        LeaveBehindController create() {
            return new LeaveBehindController(imageOperations, resources);
        }
    }

}
