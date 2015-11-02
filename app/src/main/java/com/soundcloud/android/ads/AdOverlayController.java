package com.soundcloud.android.ads;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.view.View;

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

    private Optional<OverlayAdData> overlayData = Optional.absent();

    private AdOverlayPresenter presenter;
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
    public void onAdImageLoaded() {
        onAdVisible();
    }

    @Override
    public void onImageClick() {
        startActivity(overlayData.get().getClickthroughUrl());
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
        final PlayQueueItem playQueueItem = playQueueManager.getCurrentPlayQueueItem();
        final AdOverlayTrackingEvent event = AdOverlayTrackingEvent.forClick(
                overlayData.get(),
                playQueueItem.getUrn(),
                accountOperations.getLoggedInUserUrn(),
                playQueueManager.getCurrentTrackSourceInfo());
        eventBus.publish(EventQueue.TRACKING, event);
    }

    public void initialize(OverlayAdData data) {
        this.overlayData = Optional.of(data);
        presenter = AdOverlayPresenter.create(data, trackView, this, eventBus, context.getResources(), imageOperations);
        setAdNotVisible();
    }

    public void show() {
        show(false);
    }

    public void show(boolean isForeground) {
        if (shouldDisplayAdOverlay(isForeground)) {
            presenter.bind(overlayData.get());
            resetMetaData();
        }
    }

    private void resetMetaData() {
        if (overlayData.isPresent()) {
            overlayData.get().resetMetaAdState();
        }
    }

    private boolean shouldDisplayAdOverlay(boolean isForeground) {
        if (!overlayData.isPresent()) {
            return false;
        }

        final boolean isPortrait = deviceHelper.getCurrentOrientation() == Configuration.ORIENTATION_PORTRAIT;
        return presenter.shouldDisplayOverlay(overlayData.get(), isExpanded, isPortrait, isForeground);
    }

    private void onAdVisible() {
        if (presenter != null) {
            presenter.onAdVisible(playQueueManager.getCurrentPlayQueueItem(), overlayData.get(), playQueueManager.getCurrentTrackSourceInfo());
            listener.onAdOverlayShown(presenter.isFullScreen());
        }
    }

    private void setAdNotVisible() {
        if (presenter != null) {
            presenter.onAdNotVisible();
        }
    }

    public boolean isNotVisible() {
        return presenter == null || presenter.isNotVisible();
    }

    public boolean isVisibleInFullscreen() {
        return !isNotVisible() && presenter.isFullScreen();
    }

    public void clear() {
        resetMetaData();
        if (presenter != null) {
            setOverlayDismissed();
            final boolean fullScreen = presenter.isFullScreen();
            presenter.clear();
            presenter = null;
            overlayData = Optional.absent();
            listener.onAdOverlayHidden(fullScreen);
        }
    }

    private void setOverlayDismissed() {
        if (overlayData.isPresent()) {
            overlayData.get().setMetaAdDismissed();
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
