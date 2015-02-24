package com.soundcloud.android.payments.googleplay;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class BillingServiceBinderTest {

    private BillingServiceBinder binder;

    @Mock private PackageManager packageManager;
    @Mock private ServiceConnection serviceConnection;
    @Mock private Context context;
    @Mock private Activity activity;

    @Captor private ArgumentCaptor<Intent> intentCaptor;

    @Before
    public void setUp() throws Exception {
        binder = new BillingServiceBinder(context);
        when(context.getPackageManager()).thenReturn(packageManager);
    }

    @Test
    public void canBindIsTrueIfServiceIntentIsResolvable() {
        List<ResolveInfo> resolveInfo = Lists.newArrayList(new ResolveInfo());
        when(packageManager.queryIntentServices(any(Intent.class), anyInt())).thenReturn(resolveInfo);

        expect(binder.canConnect()).toBeTrue();
    }

    @Test
    public void canBindIsFalseIfServiceIntentIsNotResolvable() {
        when(packageManager.queryIntentServices(any(Intent.class), anyInt())).thenReturn(Collections.<ResolveInfo>emptyList());

        expect(binder.canConnect()).toBeFalse();
    }

    @Test
    public void connectIntentHasCorrectActionAndSetsTargetPackage() {
        binder.connect(activity, serviceConnection);
        verify(activity).bindService(intentCaptor.capture(), any(ServiceConnection.class), eq(Context.BIND_AUTO_CREATE));
        Intent intent = intentCaptor.getValue();

        expect(intent.getAction()).toEqual("com.android.vending.billing.InAppBillingService.BIND");
        expect(intent.getPackage()).toEqual("com.android.vending");
    }

}