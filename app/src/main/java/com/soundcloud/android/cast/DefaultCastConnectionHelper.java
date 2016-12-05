package com.soundcloud.android.cast;

import static com.soundcloud.android.cast.CastProtocol.TAG;

import com.appboy.support.StringUtils;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.MediaRouteButton;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashSet;
import java.util.Set;

@Singleton
class DefaultCastConnectionHelper extends DefaultActivityLightCycle<AppCompatActivity>
        implements CastConnectionHelper {

    private static final int EXPECTED_MEDIA_BUTTON_CAPACITY = 6;

    private final Set<MediaRouteButton> mediaRouteButtons;
    private final Set<MenuItem> mediaRouteMenuItems;

    private final Set<OnConnectionChangeListener> connectionChangeListeners;
    private final CastContextWrapper castContextWrapper;

    private boolean isCastableDeviceAvailable;
    private String deviceName;

    @Inject
    DefaultCastConnectionHelper(CastContextWrapper castContextWrapper) {
        this.castContextWrapper = castContextWrapper;
        this.mediaRouteMenuItems = new HashSet<>();
        this.mediaRouteButtons = new HashSet<>(EXPECTED_MEDIA_BUTTON_CAPACITY);
        this.connectionChangeListeners = new HashSet<>();
    }

    public void addOnConnectionChangeListener(OnConnectionChangeListener listener) {
        connectionChangeListeners.add(listener);
        notifyListeners(isCastableDeviceAvailable);
    }

    public void removeOnConnectionChangeListener(OnConnectionChangeListener listener) {
        connectionChangeListeners.remove(listener);
    }

    public void notifyConnectionChange(boolean castAvailable, Optional<String> optionalDeviceName) {
        this.deviceName = optionalDeviceName.or(StringUtils.EMPTY_STRING);
        this.isCastableDeviceAvailable = castAvailable;
        notifyListeners(castAvailable);
        updateMediaRouteButtons();
    }

    private void notifyListeners(boolean castAvailable) {
        for (OnConnectionChangeListener listener : connectionChangeListeners) {
            if (castAvailable) {
                listener.onCastAvailable();
            } else {
                listener.onCastUnavailable();
            }
        }
    }

    @Override
    public MenuItem addMediaRouterButton(Context context, Menu menu, int itemId) {
        final MenuItem menuItem = CastButtonFactory.setUpMediaRouteButton(context, menu, itemId);
        Log.d(TAG, "AddMediaRouterButton called for " + menuItem + " vis : " + isCastableDeviceAvailable);

        mediaRouteMenuItems.add(menuItem);
        menuItem.setVisible(isCastableDeviceAvailable);
        return menuItem;
    }

    @Override
    public void removeMediaRouterButton(MenuItem castMenu) {
        mediaRouteMenuItems.remove(castMenu);
    }

    @Override
    public void addMediaRouterButton(MediaRouteButton mediaRouteButton) {
        Log.d(TAG, "AddMediaRouterButton called for " + mediaRouteButton);
        CastButtonFactory.setUpMediaRouteButton(mediaRouteButton.getContext(), mediaRouteButton);
        mediaRouteButtons.add(mediaRouteButton);
        mediaRouteButton.setVisibility(isCastableDeviceAvailable ? View.VISIBLE : View.GONE);
    }

    @Override
    public void removeMediaRouterButton(MediaRouteButton mediaRouteButton) {
        mediaRouteButtons.remove(mediaRouteButton);
    }

    @Override
    public boolean onDispatchVolumeEvent(KeyEvent event) {
        return castContextWrapper.onDispatchVolumeKeyEventBeforeJellyBean(event);
    }

    @Override
    public boolean isCasting() {
        return isCastableDeviceAvailable && !deviceName.isEmpty();
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

        for (MenuItem mediaRouteButton : mediaRouteMenuItems) {
            mediaRouteButton.setVisible(isCastableDeviceAvailable);
        }
    }

}

