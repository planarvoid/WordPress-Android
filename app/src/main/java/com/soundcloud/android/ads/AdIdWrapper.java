package com.soundcloud.android.ads;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.GooglePlayServicesUtil;

import android.content.Context;
import android.support.annotation.Nullable;

import javax.inject.Inject;
import java.io.IOException;

class AdIdWrapper {

    private final Context context;

    @Inject
    public AdIdWrapper(Context context) {
        this.context = context;
    }

    public boolean isPlayServicesAvailable() {
        return GooglePlayServicesUtil.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS;
    }

    @Nullable public AdvertisingIdClient.Info getAdInfo() throws IOException, GooglePlayServicesNotAvailableException, GooglePlayServicesRepairableException {
        return AdvertisingIdClient.getAdvertisingIdInfo(context);
    }

}
