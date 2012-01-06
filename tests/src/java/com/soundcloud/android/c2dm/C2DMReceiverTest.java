package com.soundcloud.android.c2dm;

import com.soundcloud.android.model.User;
import com.xtremelabs.robolectric.shadows.ShadowApplication;
import com.xtremelabs.robolectric.shadows.ShadowContext;
import com.xtremelabs.robolectric.shadows.ShadowContextThemeWrapper;
import com.xtremelabs.robolectric.shadows.ShadowPreferenceManager;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.apache.http.message.BasicHeader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.soundcloud.android.robolectric.DefaultTestRunner;

import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.newInstanceOf;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.preference.Preference;

@RunWith(DefaultTestRunner.class)
public class C2DMReceiverTest {
    @Before public void before() { RobolectricTestRunner.setStaticValue(Build.VERSION.class, "SDK_INT", 8); }
    @After public void after()   { expect(Robolectric.getFakeHttpLayer().hasPendingResponses()).toBeFalse(); }

    @Test
    public void registerShouldTriggerServiceStart() throws Exception {
        C2DMReceiver.register(DefaultTestRunner.application, new User());

        ShadowApplication ctxt = Robolectric.shadowOf(DefaultTestRunner.application);

        Intent svc = ctxt.getNextStartedService();
        expect(svc.getAction()).toEqual(C2DMReceiver.ACTION_REGISTER);
        expect(svc.getStringExtra("sender")).toEqual(C2DMReceiver.SENDER);
        expect(svc.getParcelableExtra("app")).not.toBeNull();
    }


    @Test
    public void registerWhenAlreadyRegisteredShouldNotTriggerServiceStart() throws Exception {
        C2DMReceiver.setRegistrationData(DefaultTestRunner.application, C2DMReceiver.PREF_REG_ID, "someid");
        C2DMReceiver.setRegistrationData(DefaultTestRunner.application, C2DMReceiver.PREF_DEVICE_URL, "http://foo.com");
        C2DMReceiver.register(DefaultTestRunner.application, new User());

        ShadowApplication ctxt = Robolectric.shadowOf(DefaultTestRunner.application);
        Intent svc = ctxt.getNextStartedService();
        expect(svc).toBeNull();
    }

    @Test
    public void registerShouldAlsoRegisterDevice() throws Exception {
        C2DMReceiver.setRegistrationData(DefaultTestRunner.application, C2DMReceiver.PREF_REG_ID, "someid");
        Robolectric.addPendingHttpResponse(201, "", new BasicHeader("Location", "http://foo.com"));

        C2DMReceiver.register(DefaultTestRunner.application, new User());

        expect(C2DMReceiver.getRegistrationData(DefaultTestRunner.application, C2DMReceiver.PREF_DEVICE_URL))
                .toEqual("http://foo.com");
    }

    @Test
    public void unregisterShouldTriggerServiceStart() throws Exception {
        C2DMReceiver.unregister(DefaultTestRunner.application);

        ShadowApplication ctxt = Robolectric.shadowOf(DefaultTestRunner.application);

        Intent svc = ctxt.getNextStartedService();
        expect(svc.getAction()).toEqual(C2DMReceiver.ACTION_UNREGISTER);
        expect(svc.getParcelableExtra("app")).not.toBeNull();
    }


    @Test
    public void testUnregisterWithRegisteredDevice() throws Exception {
        C2DMReceiver.setRegistrationData(DefaultTestRunner.application,
                C2DMReceiver.PREF_DEVICE_URL, "http://foo");


        C2DMReceiver.unregister(DefaultTestRunner.application);

        Robolectric.addHttpResponseRule("DELETE", "http://foo", new TestHttpResponse(200, ""));
        C2DMReceiver.processDeletionQueue(DefaultTestRunner.application, null);
    }

    @Test
    public void itShouldntDoAnyThingOnPreGingerbread() throws Exception {
        RobolectricTestRunner.setStaticValue(Build.VERSION.class, "SDK_INT", 5);
        C2DMReceiver.register(DefaultTestRunner.application, new User());
        C2DMReceiver.unregister(DefaultTestRunner.application);
    }
}
