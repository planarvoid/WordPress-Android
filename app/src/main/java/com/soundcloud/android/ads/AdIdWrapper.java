package com.soundcloud.android.ads;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.soundcloud.android.utils.GooglePlayServicesWrapper;

import android.content.Context;
import android.support.annotation.Nullable;

import javax.inject.Inject;
import java.io.IOException;

class AdIdWrapper {

    private final Context context;
    private final GooglePlayServicesWrapper googlePlayServicesWrapper;

    @Inject
    public AdIdWrapper(Context context, GooglePlayServicesWrapper googlePlayServicesWrapper) {
        this.context = context;
        this.googlePlayServicesWrapper = googlePlayServicesWrapper;
    }

    public boolean isPlayServicesAvailable() {
        return googlePlayServicesWrapper.isPlayServiceAvailable(context);
    }

    @Nullable
    public AdvertisingIdClient.Info getAdInfo() throws IOException, GooglePlayServicesNotAvailableException, GooglePlayServicesRepairableException {
        return AdvertisingIdClient.getAdvertisingIdInfo(context);
    }

}
