package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.LeaveBehind;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.utils.DeviceHelper;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import android.view.ViewStub;
import android.widget.ImageView;

import javax.annotation.Nullable;
import javax.inject.Inject;

class LeaveBehindController implements View.OnClickListener{

    private final View trackView;
    private final ImageOperations imageOperations;
    private final Context context;
    private final DeviceHelper deviceHelper;

    private @Nullable LeaveBehind data;

    private View leaveBehind;
    private ImageView adImage;

    private final ImageListener imageListener = new ImageListener() {
        @Override
        public void onLoadingStarted(String imageUri, View view) {}

        @Override
        public void onLoadingFailed(String imageUri, View view, String failedReason) {}

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            show();
        }
    };

    private LeaveBehindController(View trackView, ImageOperations imageOperations, Context context, DeviceHelper deviceHelper) {
        this.trackView = trackView;
        this.imageOperations = imageOperations;
        this.context = context;
        this.deviceHelper = deviceHelper;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.leave_behind_close:
                dismiss();
                break;
            case R.id.leave_behind_image:
                startActivity(Uri.parse(data.getLinkUrl()));
                dismiss();
                break;
            default:
                throw new IllegalArgumentException("Unexpected view ID: "
                        + view.getContext().getResources().getResourceName(view.getId()));
        }
    }

    private void startActivity(Uri uri) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    void setup(LeaveBehind data) {
        if (deviceHelper.getCurrentOrientation() == Configuration.ORIENTATION_PORTRAIT) {
            this.data = data;
            leaveBehind = getLeaveBehindView();

            adImage = (ImageView) leaveBehind.findViewById(R.id.leave_behind_image);
            imageOperations.displayLeaveBehind(Uri.parse(data.getImageUrl()), adImage, imageListener);

            adImage.setOnClickListener(this);
            leaveBehind.findViewById(R.id.leave_behind_close).setOnClickListener(this);
        }
    }

    private void show() {
        if (data != null) {
            leaveBehind.setVisibility(View.VISIBLE);
        }
    }

    void dismiss() {
        if (data != null) {
            leaveBehind.setVisibility(View.GONE);
            clear();
        }
    }

    private void clear() {
        data = null;
        adImage.setImageDrawable(null);
    }

    private View getLeaveBehindView() {
        View leaveBehind = trackView.findViewById(R.id.leave_behind);
        if (leaveBehind == null) {
            ViewStub stub = (ViewStub) trackView.findViewById(R.id.leave_behind_stub);
            leaveBehind = stub.inflate();
        }
        return leaveBehind;
    }

    static class Factory {
        private final ImageOperations imageOperations;
        private final Context context;
        private final DeviceHelper deviceHelper;

        @Inject
        Factory(ImageOperations imageOperations, Context context, DeviceHelper deviceHelper) {
            this.imageOperations = imageOperations;
            this.context = context;
            this.deviceHelper = deviceHelper;
        }

        LeaveBehindController create(View trackView) {
            return new LeaveBehindController(trackView, imageOperations, context, deviceHelper);
        }
    }

}
