package com.soundcloud.android.payments.googleplay;

import com.android.vending.billing.IInAppBillingService;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;

import javax.inject.Inject;
import java.util.List;

class BillingServiceBinder {

    private static final int NO_FLAGS = 0;

    private final Context context;
    private final Intent serviceIntent;

    @Inject
    BillingServiceBinder(Context context) {
        this.context = context;
        serviceIntent = new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
    }

    public boolean canConnect() {
        List<ResolveInfo> resolveInfo = context.getPackageManager().queryIntentServices(
                serviceIntent, PackageManager.MATCH_DEFAULT_ONLY);
        return resolveInfo != null && !resolveInfo.isEmpty();
    }

    public void connect(Activity bindingActivity, ServiceConnection serviceConnection) {
        bindingActivity.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    public IInAppBillingService bind(IBinder iBinder) {
        return IInAppBillingService.Stub.asInterface(iBinder);
    }

}
