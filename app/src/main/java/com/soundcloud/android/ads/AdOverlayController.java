package com.soundcloud.android.ads;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;

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

    @Nullable private PropertySet data;

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
        startActivity(data.get(AdOverlayProperty.CLICK_THROUGH_URL));
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
        final AdOverlayTrackingEvent event = AdOverlayTrackingEvent.forClick(
                playQueueManager.getCurrentMetaData(),
                playQueueManager.getCurrentTrackUrn(),
                accountOperations.getLoggedInUserUrn(),
                playQueueManager.getCurrentTrackSourceInfo());
        eventBus.publish(EventQueue.TRACKING, event);
    }

    public void initialize(PropertySet data) {
        this.data = data;
        presenter = AdOverlayPresenter.create(data, trackView, this, eventBus, context.getResources(), imageOperations);
        setAdNotVisible();
    }

    public void show() {
        show(false);
    }

    public void show(boolean isForeground) {
        if (shouldDisplayAdOverlay(isForeground)) {
            presenter.bind(data);
            resetMetaData();
        }
    }

    private void resetMetaData() {
        if (data != null) {
            data.put(AdOverlayProperty.META_AD_COMPLETED, false);
            data.put(AdOverlayProperty.META_AD_CLICKED, false);
        }
    }

    private boolean shouldDisplayAdOverlay(boolean isForeground) {
        if (data == null) {
            return false;
        }

        final boolean isPortrait = deviceHelper.getCurrentOrientation() == Configuration.ORIENTATION_PORTRAIT;

        return presenter.shouldDisplayOverlay(data, isExpanded, isPortrait, isForeground);
    }

    private void onAdVisible() {
        if (presenter != null) {
            presenter.onAdVisible(playQueueManager.getCurrentTrackUrn(), data, playQueueManager.getCurrentTrackSourceInfo());
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
            data = null;
            listener.onAdOverlayHidden(fullScreen);
        }
    }

    private void setOverlayDismissed() {
        if (data != null) {
            data.put(AdOverlayProperty.META_AD_DISMISSED, true);
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
