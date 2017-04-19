package com.soundcloud.android.di;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.tests.SoundCloudTestApplication;
import com.soundcloud.android.utils.ConnectionHelper;
import com.soundcloud.android.utils.GooglePlayServicesWrapper;
import com.soundcloud.android.utils.TestConnectionHelper;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.Context;
import android.net.ConnectivityManager;
import android.telephony.TelephonyManager;

public class TestApplicationModule extends ApplicationModule {

    public TestApplicationModule(SoundCloudTestApplication application) {
        super(application);
    }

    @Override
    public ConnectionHelper provideConnectionHelper(ConnectivityManager connectivityManager, TelephonyManager telephonyManager, EventBus eventBus) {
        return new TestConnectionHelper(eventBus);
    }

    @Override
    public GooglePlayServicesWrapper provideGooglePlayServicesWrapper() {
        final GooglePlayServicesWrapper googlePlayServicesWrapper = mock(GooglePlayServicesWrapper.class);
        when(googlePlayServicesWrapper.isPlayServiceAvailable(any(Context.class))).thenReturn(true);
        when(googlePlayServicesWrapper.isPlayServiceAvailable(any(Context.class), anyInt())).thenReturn(true);
        return googlePlayServicesWrapper;
    }
}
