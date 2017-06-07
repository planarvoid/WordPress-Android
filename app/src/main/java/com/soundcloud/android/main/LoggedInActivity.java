package com.soundcloud.android.main;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.LoggedInController;
import com.soundcloud.android.accounts.UserRemovedController;
import com.soundcloud.android.ads.AdsStorage;
import com.soundcloud.android.cast.CastButtonInstaller;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.cast.CastIntroductoryOverlayPresenter;
import com.soundcloud.android.policies.PolicyUpdateController;
import com.soundcloud.android.receiver.UnauthorisedRequestReceiver;
import com.soundcloud.android.stream.StreamRefreshController;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.LightCycle;

import android.app.Fragment;
import android.content.Intent;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import javax.inject.Inject;

/**
 * Just the basics. Should arguably be extended by all activities that a logged in user would use
 */
public abstract class LoggedInActivity extends RootActivity implements CastConnectionHelper.OnConnectionChangeListener {

    @Inject @LightCycle CastConnectionHelper castConnectionHelper;
    @Inject @LightCycle UnauthorisedRequestReceiver.LightCycle unauthorisedRequestLightCycle;
    @Inject @LightCycle UserRemovedController userRemovedController;
    @Inject @LightCycle LoggedInController loggedInController;
    @Inject @LightCycle PolicyUpdateController policyUpdateController;
    @Inject @LightCycle StreamRefreshController streamRefreshController;
    @Inject @LightCycle CastIntroductoryOverlayPresenter castIntroductoryOverlayPresenter;
    @Inject CastButtonInstaller castButtonInstaller;
    @Inject AdsStorage adsStorage;

    private Optional<MenuItem> castMenu = Optional.absent();

    public LoggedInActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public boolean onSearchRequested() {
        // just focus on the search tab, don't show default android search dialog
        startActivity(new Intent(Actions.SEARCH).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        castMenu = castButtonInstaller.addMediaRouteButton(this, menu, R.id.media_route_menu_item);
        updateCastMenuItemVisibility();
        return true;
    }

    private void updateCastMenuItemVisibility() {
        castMenu.ifPresent(menuItem -> {
            final boolean castButtonVisible = castConnectionHelper.isCastAvailable();
            menuItem.setVisible(castButtonVisible);
            if (castButtonVisible) {
                castIntroductoryOverlayPresenter.showIntroductoryOverlayForCastIfNeeded();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        adsStorage.preventPrestitialFetchForTimeInterval();
        castConnectionHelper.addOnConnectionChangeListener(this);
    }

    @Override
    protected void onPause() {
        castMenu = Optional.absent();
        castConnectionHelper.removeOnConnectionChangeListener(this);
        super.onPause();
    }

    /**
     * For the search UI, we need to block out the UI completely. this might change as requirements to
     * To be implemented
     */
    @Override
    public boolean onSupportNavigateUp() {
        if (isTaskRoot()) {
            // empty backstack and not a landing page, might be from a notification or deeplink
            // just go to the home activity
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
        return true;
    }

    /**
     * Convenience method to get the content id for usage in one-off fragments
     */
    public static int getContentHolderViewId() {
        return R.id.container;
    }

    @Override
    protected void setActivityContentView() {
        // Override with specific base layout in Activity. See also: BaseLayoutHelper
    }

    protected void setContentFragment(final Fragment f) {
        getFragmentManager()
                .beginTransaction()
                .replace(getContentHolderViewId(), f)
                .commit();
    }

    protected void clearContentFragment() {
        Fragment fragment = getFragmentManager()
                .findFragmentById(getContentHolderViewId());

        if (fragment != null) {
            getFragmentManager()
                    .beginTransaction()
                    .remove(fragment)
                    .commit();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (castConnectionHelper.onDispatchVolumeEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onCastAvailable() {
        updateCastMenuItemVisibility();
    }

    @Override
    public void onCastUnavailable() {
        updateCastMenuItemVisibility();
    }
}
