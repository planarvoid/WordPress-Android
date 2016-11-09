package com.soundcloud.android.ads;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.AdOverlayTrackingEvent;
import com.soundcloud.android.events.EventQueue;
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

@SuppressWarnings("PMD.AccessorClassGeneration")
@AutoFactory(allowSubclasses = true)
public class AdOverlayController implements AdOverlayPresenter.Listener {

    private final View trackView;
    private final Context context;
    private final DeviceHelper deviceHelper;
    private final EventBus eventBus;
    private final PlayQueueManager playQueueManager;
    private final AccountOperations accountOperations;
    private final InterstitialPresenterFactory interstitialPresenterFactory;
    private final LeaveBehindPresenterFactory leaveBehindPresenterFactory;
    private final AdOverlayListener listener;
    private final AdViewabilityController adViewabilityController;

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

    AdOverlayController(View trackView,
                        AdOverlayListener listener,
                        @Provided Context context,
                        @Provided DeviceHelper deviceHelper,
                        @Provided EventBus eventBus,
                        @Provided PlayQueueManager playQueueManager,
                        @Provided AccountOperations accountOperations,
                        @Provided InterstitialPresenterFactory interstitialPresenterFactory,
                        @Provided LeaveBehindPresenterFactory leaveBehindPresenterFactory,
                        @Provided AdViewabilityController adViewabilityController) {
        this.trackView = trackView;
        this.listener = listener;
        this.context = context;
        this.deviceHelper = deviceHelper;
        this.eventBus = eventBus;
        this.playQueueManager = playQueueManager;
        this.accountOperations = accountOperations;
        this.interstitialPresenterFactory = interstitialPresenterFactory;
        this.leaveBehindPresenterFactory = leaveBehindPresenterFactory;
        this.adViewabilityController = adViewabilityController;
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

        if (isInterstitial(data)) {
            presenter = interstitialPresenterFactory.create(trackView, this);
        } else {
            presenter = leaveBehindPresenterFactory.create(trackView, this);
        }
        setAdNotVisible();
    }

    private static boolean isInterstitial(OverlayAdData data) {
        return data instanceof InterstitialAd;
    }

    public void show() {
        show(false);
    }

    public void show(boolean isForeground) {
        if (overlayData.isPresent() && shouldDisplayAdOverlay(isForeground)) {
            OverlayAdData adData = overlayData.get();
            adViewabilityController.startOverlayTracking(presenter.getImageView(), adData);
            presenter.bind(adData);
            resetMetaData();
        }
    }

    private void resetMetaData() {
        if (overlayData.isPresent()) {
            overlayData.get().resetMetaAdState();
        }
    }

    private boolean shouldDisplayAdOverlay(boolean isForeground) {
        final boolean isPortrait = deviceHelper.getCurrentOrientation() == Configuration.ORIENTATION_PORTRAIT;
        return presenter.shouldDisplayOverlay(overlayData.get(), isExpanded, isPortrait, isForeground);
    }

    private void onAdVisible() {
        if (presenter != null && playQueueManager.getCurrentPlayQueueItem().isTrack()) {
            presenter.onAdVisible(playQueueManager.getCurrentPlayQueueItem(),
                                  overlayData.get(),
                                  playQueueManager.getCurrentTrackSourceInfo());
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
            final boolean fullScreen = presenter.isFullScreen();
            setOverlayDismissed();
            presenter.clear();
            presenter = null;
            overlayData = Optional.absent();
            listener.onAdOverlayHidden(fullScreen);
        }
        adViewabilityController.stopOverlayTracking();
    }

    private void setOverlayDismissed() {
        if (overlayData.isPresent()) {
            overlayData.get().setMetaAdDismissed();
        }
    }
}
