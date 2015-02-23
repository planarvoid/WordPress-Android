package com.soundcloud.android.main;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.accounts.AccountPlaybackController;
import com.soundcloud.android.accounts.LogoutActivity;
import com.soundcloud.android.accounts.UserRemovedLightCycle;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.image.ImageOperationsController;
import com.soundcloud.android.lightcycle.LightCycleActionBarActivity;
import com.soundcloud.android.receiver.UnauthorisedRequestReceiver;
import com.soundcloud.android.rx.eventbus.EventBus;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.view.KeyEvent;
import android.view.Menu;

import javax.inject.Inject;

/**
 * Just the basics. Should arguably be extended by all activities that a logged in user would use
 */
public abstract class ScActivity extends LightCycleActionBarActivity {
    @Inject CastConnectionHelper castConnectionHelper;
    @Inject ActivityLifeCyclePublisher activityLifeCyclePublisher;
    @Inject NetworkConnectivityController networkConnectivityController;
    @Inject UnauthorisedRequestReceiver.LightCycle unauthorisedRequestLightCycle;
    @Inject UserRemovedLightCycle userRemovedLightCycle;
    @Inject ImageOperationsController imageOperationsController;
    @Inject AccountPlaybackController accountPlaybackController;
    @Inject ScreenStateLightCycle screenStateLightCycle;

    @Inject protected EventBus eventBus;
    @Inject protected AccountOperations accountOperations;

    private long currentUserId;

    public ScActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
        lightCycleDispatcher
                .attach(activityLifeCyclePublisher)
                .attach(networkConnectivityController)
                .attach(unauthorisedRequestLightCycle)
                .attach(userRemovedLightCycle)
                .attach(imageOperationsController)
                .attach(castConnectionHelper)
                .attach(accountPlaybackController)
                .attach(screenStateLightCycle);
    }

    // TODO: Ugly, but the support library (r19) does not update the AB title correctly via setTitle
    @Override
    public void setTitle(CharSequence title) {
        getSupportActionBar().setTitle(title);
    }

    @Override
    public void setTitle(int titleId) {
        getSupportActionBar().setTitle(titleId);
    }

    @Override
    public boolean onSearchRequested() {
        // just focus on the search tab, don't show default android search dialog
        startActivity(new Intent(Actions.SEARCH).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        return false;
    }

    public boolean isForeground() {
        return screenStateLightCycle.isForeground();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (menu.findItem(R.id.media_route_menu_item) != null){
            castConnectionHelper.addMediaRouterButton(menu, R.id.media_route_menu_item);
        }
        return true;
    }

    public long getCurrentUserId() {
        // TODO : inline ?
        if (currentUserId == 0) {
            currentUserId = accountOperations.getLoggedInUserId();
        }
        return currentUserId;
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
        setContentView(R.layout.container_layout);
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
        return !screenStateLightCycle.isConfigurationChange() || screenStateLightCycle.isReallyResuming();
    }

    @Override
    protected Dialog onCreateDialog(int which) {
        switch (which) {
            case Consts.Dialogs.DIALOG_LOGOUT:
                return new AlertDialog.Builder(this).setTitle(R.string.menu_clear_user_title)
                        .setMessage(R.string.menu_clear_user_desc)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                LogoutActivity.start(ScActivity.this);
                            }
                        }).create();
            default:
                return super.onCreateDialog(which);
        }
    }

}
