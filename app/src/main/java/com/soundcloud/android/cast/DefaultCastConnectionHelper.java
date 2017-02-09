package com.soundcloud.android.cast;

import static com.soundcloud.android.cast.CastProtocol.TAG;

import com.appboy.support.StringUtils;
import com.google.android.gms.cast.framework.CastButtonFactory;
import com.google.android.gms.cast.framework.CastSession;
import com.soundcloud.android.utils.ErrorUtils;
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
import java.util.WeakHashMap;

@Singleton
class DefaultCastConnectionHelper extends DefaultActivityLightCycle<AppCompatActivity> implements CastConnectionHelper {

    private static final int EXPECTED_MEDIA_BUTTON_CAPACITY = 6;

    private final Set<MediaRouteButton> mediaRouteButtons;
    private final WeakHashMap<Context, MenuItem> mediaRouteMenuItems;

    private final Set<OnConnectionChangeListener> connectionChangeListeners;
    private final CastContextWrapper castContextWrapper;

    private boolean sessionConnected;
    private boolean isCastDeviceAvailable;

    @Inject
    DefaultCastConnectionHelper(CastContextWrapper castContextWrapper) {
        this.castContextWrapper = castContextWrapper;
        this.mediaRouteMenuItems = new WeakHashMap<>();
        this.mediaRouteButtons = new HashSet<>(EXPECTED_MEDIA_BUTTON_CAPACITY);
        this.connectionChangeListeners = new HashSet<>();
    }

    @Override
    public void onPause(AppCompatActivity activity) {
        if (activity instanceof OnConnectionChangeListener) {
            removeOnConnectionChangeListener((OnConnectionChangeListener) activity);
        }
        super.onPause(activity);
    }

    @Override
    public void onResume(AppCompatActivity activity) {
        super.onResume(activity);
        if (activity instanceof OnConnectionChangeListener) {
            activity.invalidateOptionsMenu();
            addOnConnectionChangeListener((OnConnectionChangeListener) activity);
        }
    }

    @Override
    public void addOnConnectionChangeListener(OnConnectionChangeListener listener) {
        connectionChangeListeners.add(listener);
        notifyListeners();
    }

    @Override
    public void removeOnConnectionChangeListener(OnConnectionChangeListener listener) {
        connectionChangeListeners.remove(listener);
    }

    @Override
    public void notifyConnectionChange(boolean sessionConnected, boolean castAvailable) {
        this.sessionConnected = sessionConnected;
        this.isCastDeviceAvailable = castAvailable;
        updateMediaRouteButtonsVisibility();
        notifyListeners();
    }

    private void notifyListeners() {
        for (OnConnectionChangeListener listener : connectionChangeListeners) {
            if (isCastAvailable()) {
                listener.onCastAvailable();
            } else {
                listener.onCastUnavailable();
            }
        }
    }

    private void updateMediaRouteButtonsVisibility() {
        for (MediaRouteButton mediaRouteButton : mediaRouteButtons) {
            mediaRouteButton.setVisibility(isCastDeviceAvailable ? View.VISIBLE : View.GONE);
        }

        for (MenuItem mediaRouteButton : mediaRouteMenuItems.values()) {
            mediaRouteButton.setVisible(isCastDeviceAvailable);
        }
    }

    @Override
    public MenuItem addMediaRouterButton(Context context, Menu menu, int itemId) {
        try {
            final MenuItem menuItem = CastButtonFactory.setUpMediaRouteButton(context, menu, itemId);
            Log.d(TAG, "AddMediaRouterButton called for " + menuItem + " vis : " + isCastDeviceAvailable);

            mediaRouteMenuItems.put(context, menuItem);
            menuItem.setVisible(isCastDeviceAvailable);
            return menuItem;

        } catch (Exception ex) {
            ErrorUtils.handleSilentExceptionWithLog(ex, "Unable to set up media route item");
            return null;
        }
    }

    @Override
    public void removeMediaRouterButton(Context context, MenuItem castMenu) {
        mediaRouteMenuItems.remove(context);
    }

    @Override
    public void addMediaRouterButton(MediaRouteButton mediaRouteButton) {
        try {
            CastButtonFactory.setUpMediaRouteButton(mediaRouteButton.getContext(), mediaRouteButton);
            mediaRouteButtons.add(mediaRouteButton);
            mediaRouteButton.setVisibility(isCastDeviceAvailable ? View.VISIBLE : View.GONE);
            mediaRouteButton.onAttachedToWindow();
        } catch (Exception ex) {
            ErrorUtils.handleSilentExceptionWithLog(ex, "Unable to set up media route item " + mediaRouteButton);
        }
    }

    @Override
    public void removeMediaRouterButton(MediaRouteButton mediaRouteButton) {
        mediaRouteButtons.remove(mediaRouteButton);
        mediaRouteButton.onDetachedFromWindow();
    }

    @Override
    public boolean onDispatchVolumeEvent(KeyEvent event) {
        return castContextWrapper.onDispatchVolumeKeyEventBeforeJellyBean(event);
    }

    @Override
    public boolean isCasting() {
        return sessionConnected;
    }

    @Override
    public boolean isCastAvailable() {
        return isCastDeviceAvailable;
    }

    @Override
    public boolean onOptionsItemSelected(AppCompatActivity activity, MenuItem item) {
        return false;
    }

    @Override
    public String getDeviceName() {
        Optional<CastSession> castSession = castContextWrapper.getCurrentCastSession();
        if (castSession.isPresent() && castSession.get().getCastDevice() != null) {
            return Optional.fromNullable(castSession.get().getCastDevice().getFriendlyName()).or(StringUtils.EMPTY_STRING);
        } else {
            return StringUtils.EMPTY_STRING;
        }
    }
}

