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

public class LeaveBehindController implements View.OnClickListener{

    private static final Uri TEST_LEAVE_BEHIND = Uri.parse("https://cloud.githubusercontent.com/assets/283794/4220512/91526122-3901-11e4-8092-20db3b1ff853.jpg");

    private final View trackView;
    private final ImageOperations imageOperations;
    private final Resources resources;

    private final ImageListener imageListener = new ImageListener() {
        @Override
        public void onLoadingStarted(String imageUri, View view) {
        }

        @Override
        public void onLoadingFailed(String imageUri, View view, String failedReason) {
            hide();
        }

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            show();
        }
    };

    public LeaveBehindController(View trackView, ImageOperations imageOperations, Resources resources) {
        this.trackView = trackView;
        this.imageOperations = imageOperations;
        this.resources = resources;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.leave_behind_close:
                hide();
                break;
            case R.id.leave_behind_image:
                break;
            default:
                break;
        }
    }

    public void set() {
        final View leaveBehind = getLeaveBehind();
        leaveBehind.findViewById(R.id.leave_behind_close).setOnClickListener(this);
        leaveBehind.findViewById(R.id.leave_behind_image).setOnClickListener(this);

        ImageView leaveBehindImage = (ImageView) leaveBehind.findViewById(R.id.leave_behind_image);
        imageOperations.displayLeaveBehind(TEST_LEAVE_BEHIND, leaveBehindImage, imageListener, resources.getDrawable(R.drawable.placeholder));
    }

    public void show() {
        final View leaveBehind = trackView.findViewById(R.id.leave_behind);
        leaveBehind.setVisibility(View.VISIBLE);
    }

    public void hide() {
        final View leaveBehind = trackView.findViewById(R.id.leave_behind);
        leaveBehind.setVisibility(View.GONE);
    }

    private View getLeaveBehind() {
        View leaveBehind = trackView.findViewById(R.id.leave_behind);
        if (leaveBehind == null) {
            ViewStub stub = (ViewStub) trackView.findViewById(R.id.leave_behind_stub);
            leaveBehind = stub.inflate();
            leaveBehind.setVisibility(View.GONE);
        }
        return leaveBehind;
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

        LeaveBehindController create(View trackView) {
            return new LeaveBehindController(trackView, imageOperations, resources);
        }
    }

}
