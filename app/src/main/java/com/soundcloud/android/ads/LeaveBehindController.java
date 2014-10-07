package com.soundcloud.android.ads;

import com.soundcloud.android.R;
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

@SuppressWarnings("PMD.AccessorClassGeneration")
public class LeaveBehindController implements View.OnClickListener{

    private final View trackView;
    private final ImageOperations imageOperations;
    private final Context context;
    private final DeviceHelper deviceHelper;
    private final LeaveBehindListener listener;

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
    private View leaveBehindClose;

    public interface LeaveBehindListener {
        void onLeaveBehindShown();
        void onLeaveBehindHidden();
    }

    LeaveBehindController(View trackView, LeaveBehindListener listener, ImageOperations imageOperations, Context context, DeviceHelper deviceHelper) {
        this.trackView = trackView;
        this.listener = listener;
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

    public void initialize(PropertySet data) {
        this.data = data;
        leaveBehind = getLeaveBehindView();
        adImage = (ImageView) leaveBehind.findViewById(R.id.leave_behind_image);
        adImage.setOnClickListener(this);
        leaveBehindClose = leaveBehind.findViewById(R.id.leave_behind_close);
        leaveBehindClose.setOnClickListener(this);
        setInvisible();
    }

    public void show() {
        if (shouldDisplayLeaveBehind()) {
            imageOperations.displayLeaveBehind(Uri.parse(data.get(LeaveBehindProperty.IMAGE_URL)), adImage, imageListener);
            resetMetaData();
        }
    }

    private void resetMetaData() {
        if (data != null) {
            data.put(LeaveBehindProperty.META_AD_COMPLETED, false);
            data.put(LeaveBehindProperty.META_AD_CLICKED, false);
        }
    }

    private boolean shouldDisplayLeaveBehind() {
        final boolean isPortrait = deviceHelper.getCurrentOrientation() == Configuration.ORIENTATION_PORTRAIT;
        return isPortrait
                && data != null
                && data.getOrElse(LeaveBehindProperty.META_AD_COMPLETED, false)
                && !data.getOrElse(LeaveBehindProperty.META_AD_CLICKED, false);
    }

    private void setVisible() {
        if (leaveBehind != null) {
            leaveBehind.setClickable(true);
            adImage.setVisibility(View.VISIBLE);
            leaveBehindClose.setVisibility(View.VISIBLE);
            listener.onLeaveBehindShown();
        }
    }

    private void setInvisible() {
        leaveBehind.setClickable(false);
        adImage.setVisibility(View.GONE);
        leaveBehindClose.setVisibility(View.GONE);
    }

    public boolean isDisabled() {
        return adImage == null || adImage.getVisibility() == View.GONE;
    }

    public void clear() {
        resetMetaData();
        if (leaveBehind != null) {
            adImage.setImageDrawable(null);
            setInvisible();
            leaveBehind = null;
            data = null;
            listener.onLeaveBehindHidden();
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

    public static class Factory {
        private final ImageOperations imageOperations;
        private final Context context;
        private final DeviceHelper deviceHelper;

        @Inject
        Factory(ImageOperations imageOperations, Context context, DeviceHelper deviceHelper) {
            this.imageOperations = imageOperations;
            this.context = context;
            this.deviceHelper = deviceHelper;
        }

        public LeaveBehindController create(View trackView, LeaveBehindListener listener) {
            return new LeaveBehindController(trackView, listener, imageOperations, context, deviceHelper);
        }
    }

}
