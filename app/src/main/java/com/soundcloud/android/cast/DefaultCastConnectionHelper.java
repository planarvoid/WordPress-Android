package com.soundcloud.android.cast;

import com.appboy.support.StringUtils;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteButton;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

@Singleton
public class DefaultCastConnectionHelper implements CastConnectionHelper {

    private static final int EXPECTED_MEDIA_BUTTON_CAPACITY = 6;

    private final Set<MediaRouteButton> mediaRouteButtons;
    private final Set<OnConnectionChangeListener> connectionChangeListeners;
    private final CastSessionController castSessionController;

    private boolean isCastableDeviceAvailable;
    private String deviceName;

    @Inject
    public DefaultCastConnectionHelper(CastSessionController controller) {
        this.castSessionController = controller;
        this.mediaRouteButtons = new HashSet<>(EXPECTED_MEDIA_BUTTON_CAPACITY);
        this.connectionChangeListeners = new HashSet<>();
        this.castSessionController.setCastConnectionListener(this);
    }

    public void addOnConnectionChangeListener(OnConnectionChangeListener listener) {
        connectionChangeListeners.add(listener);
    }

    public void removeOnConnectionChangeListener(OnConnectionChangeListener listener) {
        connectionChangeListeners.remove(listener);
    }

    public void notifyConnectionChange(boolean castAvailable, Optional<String> optionalDeviceName) {
        this.deviceName = optionalDeviceName.or(StringUtils.EMPTY_STRING);
        this.isCastableDeviceAvailable = castAvailable;

        for (CastConnectionHelper.OnConnectionChangeListener listener : connectionChangeListeners) {
            listener.onCastConnectionChange(deviceName);
        }
        updateMediaRouteButtons();
    }

    @Override
    public void addMediaRouterButton(Context context, Menu menu, int itemId) {
        CastButtonFactory.setUpMediaRouteButton(context, menu, itemId);
    }

    @Override
    public void addMediaRouterButton(MediaRouteButton mediaRouteButton) {
        Log.d("Cast", "AddMediaRouterButton called for " + mediaRouteButton);
        CastButtonFactory.setUpMediaRouteButton(mediaRouteButton.getContext(), mediaRouteButton);
        mediaRouteButtons.add(mediaRouteButton);
        mediaRouteButton.setVisibility(isCastableDeviceAvailable ? View.VISIBLE : View.GONE);
    }

    @Override
    public void removeMediaRouterButton(MediaRouteButton mediaRouteButton) {
        mediaRouteButtons.remove(mediaRouteButton);
    }

    @Override
    public boolean onOptionsItemSelected(AppCompatActivity activity, MenuItem item) {
        return false;
    }

    @Override
    public String getDeviceName() {
        return deviceName;
    }

    private void updateMediaRouteButtons() {
        for (MediaRouteButton mediaRouteButton : mediaRouteButtons) {
            mediaRouteButton.setVisibility(isCastableDeviceAvailable ? View.VISIBLE : View.GONE);
        }
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
    public void onResume(AppCompatActivity activity) {
        castSessionController.onResume(activity);
    }

    @Override
    public void onPause(AppCompatActivity activity) {
        castSessionController.onPause(activity);
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

