package com.soundcloud.android.ads;

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

import javax.annotation.Nullable;
import javax.inject.Inject;

@SuppressWarnings("PMD.AccessorClassGeneration")
public class AdOverlayController implements AdOverlayPresenter.Listener {

    private final View trackView;
    private final ImageOperations imageOperations;
    private final Context context;
    private final DeviceHelper deviceHelper;
    private final LeaveBehindListener listener;

    private @Nullable PropertySet data;

    private AdOverlayPresenter presenter;
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
    private boolean isExpanded;

    public void setCollapsed() {
        isExpanded = false;
    }

    public void setExpanded() {
        isExpanded = true;
    }

    public interface LeaveBehindListener {
        void onLeaveBehindShown();
        void onLeaveBehindHidden();
    }

    AdOverlayController(View trackView, LeaveBehindListener listener, ImageOperations imageOperations, Context context, DeviceHelper deviceHelper) {
        this.trackView = trackView;
        this.listener = listener;
        this.imageOperations = imageOperations;
        this.context = context;
        this.deviceHelper = deviceHelper;
    }

    @Override
    public void onImageClick() {
        startActivity(data.get(LeaveBehindProperty.CLICK_THROUGH_URL));
        clear();
    }

    @Override
    public void onCloseButtonClick() {
        clear();
    }


    private void startActivity(Uri uri) {
        final Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public void initialize(PropertySet data) {
        this.data = data;
        presenter = AdOverlayPresenter.create(data, trackView, this);
        setInvisible();
    }

    public void show() {
        show(false);
    }

    public void show(boolean isForeground) {
        if (shouldDisplayLeaveBehind(isForeground)) {
            imageOperations.displayLeaveBehind(Uri.parse(data.get(LeaveBehindProperty.IMAGE_URL)), presenter.getImageView(), imageListener);
            resetMetaData();
        }
    }

    private void resetMetaData() {
        if (data != null) {
            data.put(LeaveBehindProperty.META_AD_COMPLETED, false);
            data.put(LeaveBehindProperty.META_AD_CLICKED, false);
        }
    }

    private boolean shouldDisplayLeaveBehind(boolean isForeground) {
        if (data == null) {
            return false;
        }

        final boolean isPortrait = deviceHelper.getCurrentOrientation() == Configuration.ORIENTATION_PORTRAIT;

        return presenter.shouldDisplayOverlay(data, isExpanded, isPortrait, isForeground);
    }



    private void setVisible() {
        if (presenter != null) {
            presenter.setVisible();
            listener.onLeaveBehindShown();
        }
    }

    private void setInvisible() {
        if (presenter != null) {
            presenter.setInvisible();
        }
    }

    public boolean isNotVisible() {
        return presenter == null || presenter.isNotVisible();
    }

    public void clear() {
        resetMetaData();
        if (presenter != null) {
            presenter.clear();
            presenter = null;
            data = null;
            listener.onLeaveBehindHidden();
        }
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

        public AdOverlayController create(View trackView, LeaveBehindListener listener) {
            return new AdOverlayController(trackView, listener, imageOperations, context, deviceHelper);
        }
    }

}
