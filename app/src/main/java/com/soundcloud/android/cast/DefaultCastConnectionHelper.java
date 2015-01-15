package com.soundcloud.android.cast;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.sample.castcompanionlibrary.cast.VideoCastManager;
import com.google.sample.castcompanionlibrary.cast.callbacks.VideoCastConsumerImpl;
import com.google.sample.castcompanionlibrary.cast.exceptions.NoConnectionException;
import com.google.sample.castcompanionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.Log;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.MediaRouteButton;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

@Singleton
public class DefaultCastConnectionHelper extends VideoCastConsumerImpl implements CastConnectionHelper {

    private static final int EXPECTED_CAST_LISTENER_CAPACITY = 5;
    private static final String TAG = "CastConnectionHelper";

    private final Context context;
    private final VideoCastManager videoCastManager;
    private final Set<CastConnectionListener> castConnectionListeners;
    private final Set<MediaRouteButton> mediaRouteButtons;

    private boolean isCastableDeviceAvailable;

    @Inject
    public DefaultCastConnectionHelper(Context context, VideoCastManager videoCastManager) {
        this.context = context;
        this.videoCastManager = videoCastManager;
        castConnectionListeners = new HashSet<>(EXPECTED_CAST_LISTENER_CAPACITY);
        mediaRouteButtons = new HashSet<>(EXPECTED_CAST_LISTENER_CAPACITY);
        videoCastManager.addVideoCastConsumer(this);
    }

    @Override
    public void addMediaRouterButton(Menu menu, int itemId){
        videoCastManager.addMediaRouterButton(menu, itemId);
    }

    @Override
    public void addMediaRouterButton(MediaRouteButton mediaRouteButton) {
        videoCastManager.addMediaRouterButton(mediaRouteButton);
        mediaRouteButtons.add(mediaRouteButton);
        updateMediaRouteButtonVisibility(mediaRouteButton);
    }

    @Override
    public void removeMediaRouterButton(MediaRouteButton mediaRouteButton) {
        mediaRouteButtons.remove(mediaRouteButton);
    }

    @Override
    public void onResume(FragmentActivity activity) {
        videoCastManager.startCastDiscovery();
        videoCastManager.incrementUiCounter();
    }

    @Override
    public void onPause(FragmentActivity activity) {
        videoCastManager.stopCastDiscovery();
        videoCastManager.decrementUiCounter();
    }

    @Override
    public void reconnectSessionIfPossible(){
        videoCastManager.reconnectSessionIfPossible();
    }

    @Override
    public boolean onDispatchVolumeEvent(KeyEvent event) {
        return videoCastManager.onDispatchVolumeKeyEvent(event, .1);
    }

    @Override
    public void addConnectionListener(final CastConnectionListener listener) {
        castConnectionListeners.add(listener);
    }

    @Override
    public void removeConnectionListener(final CastConnectionListener listener) {
        castConnectionListeners.remove(listener);
    }

    @Override
    public boolean isConnected() {
        return videoCastManager.isConnected();
    }

    @Override
    public void onRemoteMediaPlayerMetadataUpdated() {
        try {
            final Urn urnFromMediaMetadata = CastPlayer.getUrnFromMediaMetadata(videoCastManager.getRemoteMediaInformation());
            for (CastConnectionListener listener : castConnectionListeners){
                listener.onMetaDataUpdated(urnFromMediaMetadata);
            }
        } catch (TransientNetworkDisconnectionException | NoConnectionException e) {
            Log.e(TAG, "Unable to get remote media information", e);
        }
    }

    @Override
    public void onApplicationConnected(ApplicationMetadata appMetadata, String sessionId, boolean wasLaunched) {
        for (CastConnectionListener listener : castConnectionListeners){
            listener.onConnectedToReceiverApp();
        }
    }

    @Override
    public void onCastAvailabilityChanged(boolean castPresent) {
        isCastableDeviceAvailable = castPresent;
        updateMediaRouteButtons();
    }

    private void updateMediaRouteButtons() {
        for (MediaRouteButton mediaRouteButton : mediaRouteButtons){
            updateMediaRouteButtonVisibility(mediaRouteButton);
        }
    }

    private void updateMediaRouteButtonVisibility(MediaRouteButton mediaRouteButton) {
        mediaRouteButton.setVisibility(isCastableDeviceAvailable ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onCreate(FragmentActivity activity, @Nullable Bundle bundle) {
        /* no-op */
    }

    @Override
    public void onNewIntent(FragmentActivity activity, Intent intent) {
        /* no-op */
    }

    @Override
    public void onStart(FragmentActivity activity) {
        /* no-op */
    }

    @Override
    public void onStop(FragmentActivity activity) {
        /* no-op */
    }

    @Override
    public void onSaveInstanceState(FragmentActivity activity, Bundle bundle) {
        /* no-op */
    }

    @Override
    public void onRestoreInstanceState(FragmentActivity activity, Bundle bundle) {
        /* no-op */
    }

    @Override
    public void onDestroy(FragmentActivity activity) {
        /* no-op */
    }
}

