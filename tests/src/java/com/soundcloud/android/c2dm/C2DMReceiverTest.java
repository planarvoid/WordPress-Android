package com.soundcloud.android.c2dm;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.service.sync.SyncAdapterService;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import com.xtremelabs.robolectric.shadows.ShadowApplication;
import com.xtremelabs.robolectric.shadows.ShadowContentResolver;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.apache.http.message.BasicHeader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.Build;

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

    @Test
    public void shouldForceSyncOnPushNotification() throws Exception {
        Account account = new Account("test", "type");
        DefaultTestRunner.application.useAccount(account);

        C2DMReceiver receiver = new C2DMReceiver();

        Intent intent = new Intent(C2DMReceiver.ACTION_RECEIVE)
                .putExtra(SyncAdapterService.EXTRA_PUSH_EVENT, "follower")
                .putExtra(SyncAdapterService.EXTRA_PUSH_EVENT_URI, "soundcloud:people:1234");

        receiver.onReceive(DefaultTestRunner.application, intent);

        ShadowContentResolver.Status status =
                ShadowContentResolver.getStatus(account, ScContentProvider.AUTHORITY);

        expect(status).not.toBeNull();
        expect(status.syncRequests).toEqual(1);
        expect(status.syncExtras.getBoolean(ContentResolver.SYNC_EXTRAS_IGNORE_SETTINGS, false)).toBeTrue();
        expect(status.syncExtras.getBoolean(ContentResolver.SYNC_EXTRAS_IGNORE_BACKOFF, false)).toBeTrue();
    }
}
