package com.soundcloud.android.receiver;

import static com.soundcloud.android.rx.observers.RxObserverHelper.fireAndForget;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.api.UnauthorisedRequestRegistry;
import com.soundcloud.android.dialog.TokenExpiredDialogFragment;
import com.soundcloud.android.rx.observers.DefaultObserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.FragmentManager;

public class UnauthorisedRequestReceiver extends BroadcastReceiver {

    private UnauthorisedRequestRegistry mRequestRegistry;
    private FragmentManager mFragmentManager;
    private TokenExpiredDialogFragment mTokenExpiredDialog;

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
        mRequestRegistry.timeSinceFirstUnauthorisedRequestIsBeyondLimit().subscribe(new DefaultObserver<Boolean>() {
            @Override
            public void onNext(Boolean expired) {
                if (expired) {
                    fireAndForget(mRequestRegistry.clearObservedUnauthorisedRequestTimestamp());
                    mTokenExpiredDialog.show(mFragmentManager, TokenExpiredDialogFragment.TAG);
                }
            }
        });

    }
}
