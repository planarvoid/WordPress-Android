package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.LeaveBehindProperty;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.propeller.PropertySet;

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

    private @Nullable PropertySet data;

    private View leaveBehind;
    private ImageView adImage;

    private final ImageListener imageListener = new ImageListener() {
        @Override
        public void onLoadingStarted(String imageUri, View view) {}

        @Override
        public void onLoadingFailed(String imageUri, View view, String failedReason) {}

        @Override
        public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
            setVisible();
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
                clear();
                break;
            case R.id.leave_behind_image:
                startActivity(data.get(LeaveBehindProperty.CLICK_THROUGH_URL));
                clear();
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

    void initialize(PropertySet data) {
        this.data = data;
        leaveBehind = getLeaveBehindView();
        adImage = (ImageView) leaveBehind.findViewById(R.id.leave_behind_image);
        adImage.setOnClickListener(this);
        leaveBehind.findViewById(R.id.leave_behind_close).setOnClickListener(this);
    }

    public void show() {
        final boolean isPortrait = deviceHelper.getCurrentOrientation() == Configuration.ORIENTATION_PORTRAIT;
        final boolean isEnabled = data != null && data.getOrElse(LeaveBehindProperty.ENABLED, false);
        if (isEnabled && isPortrait) {
            data.put(LeaveBehindProperty.ENABLED, false);
            imageOperations.displayLeaveBehind(Uri.parse(data.get(LeaveBehindProperty.IMAGE_URL)), adImage, imageListener);
        }
    }
    private void setVisible() {
        if (data != null) {
            leaveBehind.setVisibility(View.VISIBLE);
        }
    }

    void clear() {
        if (data != null) {
            data = null;
            adImage.setImageDrawable(null);
            leaveBehind.setVisibility(View.GONE);
        }
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
