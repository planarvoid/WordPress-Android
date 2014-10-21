package com.soundcloud.android.ads;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LeaveBehindTrackingEvent;
import com.soundcloud.android.image.ImageListener;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.rx.eventbus.EventBus;
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
    private final EventBus eventBus;
    private final PlayQueueManager playQueueManager;
    private final AccountOperations accountOperations;
    private final AdOverlayListener listener;

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

    public interface AdOverlayListener {
        void onAdOverlayShown(boolean fullscreen);
        void onAdOverlayHidden(boolean fullscreen);
    }

    AdOverlayController(View trackView, AdOverlayListener listener, ImageOperations imageOperations, Context context, DeviceHelper deviceHelper, EventBus eventBus, PlayQueueManager playQueueManager, AccountOperations accountOperations) {
        this.trackView = trackView;
        this.listener = listener;
        this.imageOperations = imageOperations;
        this.context = context;
        this.deviceHelper = deviceHelper;
        this.eventBus = eventBus;
        this.playQueueManager = playQueueManager;
        this.accountOperations = accountOperations;
    }

    @Override
    public void onImageClick() {
        startActivity(data.get(LeaveBehindProperty.CLICK_THROUGH_URL));
        sendTrackingEvent();
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

    private void sendTrackingEvent() {
        final LeaveBehindTrackingEvent event = LeaveBehindTrackingEvent.forClick(
                playQueueManager.getCurrentMetaData(),
                playQueueManager.getCurrentTrackUrn(),
                accountOperations.getLoggedInUserUrn(),
                playQueueManager.getCurrentTrackSourceInfo());
        eventBus.publish(EventQueue.TRACKING, event);
    }

    public void initialize(PropertySet data) {
        this.data = data;
        presenter = AdOverlayPresenter.create(data, trackView, this, eventBus);
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
            listener.onAdOverlayShown(presenter.isFullScreen());
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
            final boolean fullScreen = presenter.isFullScreen();
            presenter.clear();
            presenter = null;
            data = null;
            listener.onAdOverlayHidden(fullScreen);
        }
    }

    public static class Factory {
        private final ImageOperations imageOperations;
        private final Context context;
        private final DeviceHelper deviceHelper;
        private final EventBus eventBus;
        private final PlayQueueManager playQueueManager;
        private final AccountOperations accountOperations;

        @Inject
        Factory(ImageOperations imageOperations, Context context, DeviceHelper deviceHelper, EventBus eventBus, PlayQueueManager playQueueManager, AccountOperations accountOperations) {
            this.imageOperations = imageOperations;
            this.context = context;
            this.deviceHelper = deviceHelper;
            this.eventBus = eventBus;
            this.playQueueManager = playQueueManager;
            this.accountOperations = accountOperations;
        }

        public AdOverlayController create(View trackView, AdOverlayListener listener) {
            return new AdOverlayController(trackView, listener, imageOperations, context, deviceHelper, eventBus, playQueueManager, accountOperations);
        }
    }

}
