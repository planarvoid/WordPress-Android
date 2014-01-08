package com.soundcloud.android.receiver;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.api.UnauthorisedRequestRegistry;
import com.soundcloud.android.dialog.TokenExpiredDialogFragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentManager;

public class UnauthorisedRequestReceiver extends BroadcastReceiver {

    private final UnauthorisedRequestRegistry mRequestRegistry;
    private final FragmentManager mFragmentManager;
    private final TokenExpiredDialogFragment mTokenExpiredDialog;

    public UnauthorisedRequestReceiver(Context context, FragmentManager fragmentManager) {
        this(UnauthorisedRequestRegistry.getInstance(context), fragmentManager, new TokenExpiredDialogFragment());
    }

    @VisibleForTesting
    protected UnauthorisedRequestReceiver(UnauthorisedRequestRegistry unauthorisedRequestRegistry,
                                          FragmentManager fragmentManager,
                                          TokenExpiredDialogFragment tokenExpiredDialog) {
        mRequestRegistry = unauthorisedRequestRegistry;
        mFragmentManager = fragmentManager;
        mTokenExpiredDialog = tokenExpiredDialog;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(mRequestRegistry.timeSinceFirstUnauthorisedRequestIsBeyondLimit()){
            mRequestRegistry.clearObservedUnauthorisedRequestTimestamp();
            if (mFragmentManager.findFragmentByTag(TokenExpiredDialogFragment.TAG) == null) {
                mTokenExpiredDialog.show(mFragmentManager, TokenExpiredDialogFragment.TAG);
            }
        }
    }
}
