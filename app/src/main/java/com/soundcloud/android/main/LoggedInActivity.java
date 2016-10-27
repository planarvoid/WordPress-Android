package com.soundcloud.android.main;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.accounts.LoggedInController;
import com.soundcloud.android.accounts.UserRemovedController;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.policies.PolicyUpdateController;
import com.soundcloud.android.receiver.UnauthorisedRequestReceiver;
import com.soundcloud.android.stream.StreamRefreshController;
import com.soundcloud.lightcycle.LightCycle;

import android.app.Fragment;
import android.content.Intent;
import android.view.Menu;

import javax.inject.Inject;

/**
 * Just the basics. Should arguably be extended by all activities that a logged in user would use
 */
public abstract class LoggedInActivity extends RootActivity {

    @Inject @LightCycle CastConnectionHelper castConnectionHelper;
    @Inject @LightCycle UnauthorisedRequestReceiver.LightCycle unauthorisedRequestLightCycle;
    @Inject @LightCycle UserRemovedController userRemovedController;
    @Inject @LightCycle LoggedInController loggedInController;
    @Inject @LightCycle PolicyUpdateController policyUpdateController;
    @Inject @LightCycle StreamRefreshController streamRefreshController;
    @Inject AccountOperations accountOperations;

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
        // This is a workaround. For some devices the back/up button does not work if we don't inflate "some" menu
        getMenuInflater().inflate(R.menu.empty, menu);
        return true;
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

}
