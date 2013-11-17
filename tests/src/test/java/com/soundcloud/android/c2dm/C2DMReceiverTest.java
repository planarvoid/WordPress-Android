package com.soundcloud.android.c2dm;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.robolectric.TestHelper.createRegexRequestMatcherForUriWithClientId;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.soundcloud.android.Consts;
import com.soundcloud.android.storage.provider.ScContentProvider;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.sync.SyncAdapterService;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowAccountManager;
import com.xtremelabs.robolectric.shadows.ShadowApplication;
import com.xtremelabs.robolectric.shadows.ShadowContentResolver;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.message.BasicHeader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Intent;

@RunWith(DefaultTestRunner.class)
public class C2DMReceiverTest {
    @Before public void before() { TestHelper.setSdkVersion(8); }
    @After public void after()   { expect(Robolectric.getFakeHttpLayer().hasPendingResponses()).toBeFalse(); }

    @Test
    public void registerShouldTriggerServiceStart() throws Exception {
        C2DMReceiver.register(DefaultTestRunner.application);

        ShadowApplication ctxt = shadowOf(DefaultTestRunner.application);

        Intent svc = ctxt.getNextStartedService();
        expect(svc.getAction()).toEqual(C2DMReceiver.ACTION_REGISTER);
        expect(svc.getStringExtra("sender")).toEqual(C2DMReceiver.SENDER);
        expect(svc.getParcelableExtra("app")).not.toBeNull();
    }


    @Test
    public void registerWhenAlreadyRegisteredShouldNotTriggerServiceStart() throws Exception {
        C2DMReceiver.setRegistrationData(DefaultTestRunner.application, C2DMReceiver.PREF_REG_ID, "someid");
        C2DMReceiver.setRegistrationData(DefaultTestRunner.application, Consts.PrefKeys.C2DM_DEVICE_URL, "http://foo.com");
        C2DMReceiver.register(DefaultTestRunner.application);

        ShadowApplication ctxt = shadowOf(DefaultTestRunner.application);
        Intent svc = ctxt.getNextStartedService();
        expect(svc).toBeNull();
    }

    @Test
    public void registerShouldAlsoRegisterDevice() throws Exception {
        C2DMReceiver.setRegistrationData(DefaultTestRunner.application, C2DMReceiver.PREF_REG_ID, "someid");
        Robolectric.addPendingHttpResponse(201, "", new BasicHeader("Location", "http://foo.com"));

        C2DMReceiver.register(DefaultTestRunner.application);

        expect(C2DMReceiver.getRegistrationData(DefaultTestRunner.application, Consts.PrefKeys.C2DM_DEVICE_URL))
                .toEqual("http://foo.com");
    }

    @Test
    public void unregisterShouldTriggerServiceStart() throws Exception {
        new C2DMReceiver().unregister(DefaultTestRunner.application);

        ShadowApplication ctxt = shadowOf(DefaultTestRunner.application);

        Intent svc = ctxt.getNextStartedService();
        expect(svc.getAction()).toEqual(C2DMReceiver.ACTION_UNREGISTER);
        expect(svc.getParcelableExtra("app")).not.toBeNull();
    }


    @Test
    public void testUnregisterWithRegisteredDevice() throws Exception {
        C2DMReceiver.setRegistrationData(DefaultTestRunner.application,
                Consts.PrefKeys.C2DM_DEVICE_URL, "http://foo");


        new C2DMReceiver().unregister(DefaultTestRunner.application);

        Robolectric.addHttpResponseRule(createRegexRequestMatcherForUriWithClientId(HttpDelete.METHOD_NAME, "http://foo"), new TestHttpResponse(200, ""));
        C2DMReceiver.processDeletionQueue(DefaultTestRunner.application, null);
    }

    @Test
    public void itShouldntDoAnyThingOnPreGingerbread() throws Exception {
        TestHelper.setSdkVersion(5);
        C2DMReceiver.register(DefaultTestRunner.application);
        new C2DMReceiver().unregister(DefaultTestRunner.application);
    }

    @Test
    public void shouldForceSyncOnPushNotification() throws Exception {
        Account account = new Account("test", "com.soundcloud.android.account");
        shadowOf(ShadowAccountManager.get(DefaultTestRunner.application)).addAccount(account);

        C2DMReceiver receiver = new C2DMReceiver();

        Intent intent = new Intent(C2DMReceiver.ACTION_RECEIVE)
                .putExtra(SyncAdapterService.EXTRA_C2DM_EVENT, "follower")
                .putExtra(SyncAdapterService.EXTRA_C2DM_EVENT_URI, "soundcloud:people:1234");

        receiver.onReceive(DefaultTestRunner.application, intent);

        ShadowContentResolver.Status status =
                ShadowContentResolver.getStatus(account, ScContentProvider.AUTHORITY);

        expect(status).not.toBeNull();
        expect(status.syncRequests).toEqual(1);
        expect(status.syncExtras.getBoolean(ContentResolver.SYNC_EXTRAS_IGNORE_SETTINGS, false)).toBeTrue();
        expect(status.syncExtras.getBoolean(ContentResolver.SYNC_EXTRAS_IGNORE_BACKOFF, false)).toBeTrue();
    }
}
