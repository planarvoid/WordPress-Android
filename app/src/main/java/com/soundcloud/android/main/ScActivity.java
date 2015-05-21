package com.soundcloud.android.main;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.accounts.AccountPlaybackController;
import com.soundcloud.android.accounts.UserRemovedController;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.image.ImageOperationsController;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleAppCompatActivity;
import com.soundcloud.android.offline.PolicyUpdateController;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.receiver.UnauthorisedRequestReceiver;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.view.screen.ScreenPresenter;

import android.app.Fragment;
import android.content.Intent;
import android.media.AudioManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import javax.inject.Inject;

/**
 * Just the basics. Should arguably be extended by all activities that a logged in user would use
 */
public abstract class ScActivity extends LightCycleAppCompatActivity {
    @Inject @LightCycle CastConnectionHelper castConnectionHelper;
    @Inject @LightCycle ActivityLifeCyclePublisher activityLifeCyclePublisher;
    @Inject @LightCycle NetworkConnectivityController networkConnectivityController;
    @Inject @LightCycle UnauthorisedRequestReceiver.LightCycle unauthorisedRequestLightCycle;
    @Inject @LightCycle UserRemovedController userRemovedController;
    @Inject @LightCycle ImageOperationsController imageOperationsController;
    @Inject @LightCycle AccountPlaybackController accountPlaybackController;
    @Inject @LightCycle ScreenStateProvider screenStateProvider;
    @Inject @LightCycle PolicyUpdateController policyUpdateController;
    @Inject ApplicationProperties applicationProperties;
    @Inject protected ScreenPresenter presenter;
    @Inject protected EventBus eventBus;
    @Inject protected AccountOperations accountOperations;

    public ScActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
        presenter.attach(this);
    }

    @Override
    public boolean onSearchRequested() {
        // just focus on the search tab, don't show default android search dialog
        startActivity(new Intent(Actions.SEARCH).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        return false;
    }

    public boolean isForeground() {
        return screenStateProvider.isForeground();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // This is a workaround. For some devices the back/up button does not work if we don't inflate "some" menu
        getMenuInflater().inflate(R.menu.empty, menu);
        return true;
    }

    protected void configureMainOptionMenuItems(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (menu.findItem(R.id.media_route_menu_item) != null) {
            castConnectionHelper.addMediaRouterButton(menu, R.id.media_route_menu_item);
        }
        final MenuItem feedbackItem = menu.findItem(R.id.action_feedback);
        if (feedbackItem != null) {
            feedbackItem.setVisible(applicationProperties.shouldAllowFeedback());
        }
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
        setContentView();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    // Override this in activities with custom content views
    protected void setContentView() {
        presenter.setContainerLayout();
    }

    protected void setContentFragment(final Fragment f) {
        getFragmentManager()
                .beginTransaction()
                .replace(getContentHolderViewId(), f)
                .commit();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (castConnectionHelper.onDispatchVolumeEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    protected boolean shouldTrackScreen() {
        // What does it mean ? Is there a bug here ? #2664
        return !screenStateProvider.isConfigurationChange() || screenStateProvider.isReallyResuming();
    }

}
