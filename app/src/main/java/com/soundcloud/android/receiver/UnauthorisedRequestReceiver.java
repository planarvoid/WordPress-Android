package com.soundcloud.android.receiver;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.api.UnauthorisedRequestRegistry;
import com.soundcloud.android.dialog.TokenExpiredDialogFragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentManager;

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
}
