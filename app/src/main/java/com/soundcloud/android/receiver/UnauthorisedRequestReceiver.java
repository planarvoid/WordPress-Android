package com.soundcloud.android.receiver;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.api.UnauthorisedRequestRegistry;
import com.soundcloud.android.dialog.TokenExpiredDialogFragment;
import com.soundcloud.lightcycle.DefaultLightCycleActivity;
import com.soundcloud.android.utils.ErrorUtils;
import org.jetbrains.annotations.Nullable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;

import javax.inject.Inject;

public class UnauthorisedRequestReceiver extends BroadcastReceiver {

    private final UnauthorisedRequestRegistry requestRegistry;
    private final FragmentManager fragmentManager;
    private final TokenExpiredDialogFragment tokenExpiredDialog;

    public UnauthorisedRequestReceiver(Context context, FragmentManager fragmentManager) {
        this(UnauthorisedRequestRegistry.getInstance(context), fragmentManager, new TokenExpiredDialogFragment());
    }

    @VisibleForTesting
    protected UnauthorisedRequestReceiver(UnauthorisedRequestRegistry unauthorisedRequestRegistry,
                                          FragmentManager fragmentManager,
                                          TokenExpiredDialogFragment tokenExpiredDialog) {
        requestRegistry = unauthorisedRequestRegistry;
        this.fragmentManager = fragmentManager;
        this.tokenExpiredDialog = tokenExpiredDialog;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(requestRegistry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()){
            requestRegistry.clearObservedUnauthorisedRequestTimestamp();
            if (fragmentManager.findFragmentByTag(TokenExpiredDialogFragment.TAG) == null) {
                tokenExpiredDialog.show(fragmentManager, TokenExpiredDialogFragment.TAG);
            }
        }
    }

    public static class LightCycle extends DefaultLightCycleActivity<ActionBarActivity> {
        private UnauthorisedRequestReceiver unauthoriedRequestReceiver;

        @Inject
        public LightCycle() {
            // For dagger
        }

        @Override
        public void onCreate(ActionBarActivity activity, @Nullable Bundle bundle) {
            unauthoriedRequestReceiver = new UnauthorisedRequestReceiver(activity.getApplicationContext(), activity.getSupportFragmentManager());
        }

        @Override
        public void onResume(ActionBarActivity activity) {
            activity.registerReceiver(unauthoriedRequestReceiver, new IntentFilter(Consts.GeneralIntents.UNAUTHORIZED));
        }

        @Override
        public void onPause(ActionBarActivity activity) {
            safeUnregisterReceiver(activity, unauthoriedRequestReceiver);
        }


        private void safeUnregisterReceiver(ActionBarActivity activity, BroadcastReceiver receiver) {
            try {
                activity.unregisterReceiver(receiver);
            } catch (IllegalArgumentException e) {
                // This should not happen if the receiver is registered/unregistered in complementary methods and
                // the full lifecycle is respected, but it does.
                ErrorUtils.handleSilentException("Couldnt unregister receiver", e);
            }
        }
    }
}
