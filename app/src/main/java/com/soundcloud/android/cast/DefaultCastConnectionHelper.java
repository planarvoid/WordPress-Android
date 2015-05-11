package com.soundcloud.android.cast;

import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.callbacks.VideoCastConsumerImpl;
import org.jetbrains.annotations.Nullable;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteButton;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

@Singleton
public class DefaultCastConnectionHelper extends VideoCastConsumerImpl implements CastConnectionHelper {

    private static final int EXPECTED_MEDIA_BUTTON_CAPACITY = 6;

    private final VideoCastManager videoCastManager;
    private final Set<MediaRouteButton> mediaRouteButtons;
    private final Set<OnConnectionChangeListener> connectionChangeListeners;

    private boolean isCastableDeviceAvailable;


    @Inject
    public DefaultCastConnectionHelper(VideoCastManager videoCastManager) {
        this.videoCastManager = videoCastManager;
        mediaRouteButtons = new HashSet<>(EXPECTED_MEDIA_BUTTON_CAPACITY);
        connectionChangeListeners = new HashSet<>();
        videoCastManager.addVideoCastConsumer(this);
    }

    public void addOnConnectionChangeListener(OnConnectionChangeListener listener) {
        connectionChangeListeners.add(listener);
    }

    public void removeOnConnectionChangeListener(OnConnectionChangeListener listener) {
        connectionChangeListeners.remove(listener);
    }

    @Override
    public void onConnected() {
        notifyConnectionChange();
    }

    @Override
    public void onDisconnected() {
        notifyConnectionChange();
    }

    private void notifyConnectionChange() {
        for (CastConnectionHelper.OnConnectionChangeListener listener : connectionChangeListeners) {
            listener.onCastConnectionChange();
        }
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
    public void onResume(AppCompatActivity activity) {
        videoCastManager.startCastDiscovery();
        videoCastManager.incrementUiCounter();
    }

    @Override
    public boolean onOptionsItemSelected(AppCompatActivity activity, MenuItem item) {
        return false;
    }

    @Override
    public void onPause(AppCompatActivity activity) {
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
    public boolean isConnected() {
        return videoCastManager.isConnected();
    }

    @Override
    public String getDeviceName() {
        return videoCastManager.getDeviceName();
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
    public void onCreate(AppCompatActivity activity, @Nullable Bundle bundle) {
        /* no-op */
    }

    @Override
    public void onNewIntent(AppCompatActivity activity, Intent intent) {
        /* no-op */
    }

    @Override
    public void onStart(AppCompatActivity activity) {
        /* no-op */
    }

    @Override
    public void onStop(AppCompatActivity activity) {
        /* no-op */
    }

    @Override
    public void onSaveInstanceState(AppCompatActivity activity, Bundle bundle) {
        /* no-op */
    }

    @Override
    public void onRestoreInstanceState(AppCompatActivity activity, Bundle bundle) {
        /* no-op */
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        /* no-op */
    }
}

