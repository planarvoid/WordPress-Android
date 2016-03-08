package com.soundcloud.android.testsupport;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.accounts.AccountOperations.AccountInfoKeys.USER_ID;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.testsupport.fixtures.JsonFixtures;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowAccountManager;
import com.xtremelabs.robolectric.shadows.ShadowEnvironment;
import com.xtremelabs.robolectric.shadows.ShadowNetworkInfo;
import com.xtremelabs.robolectric.tester.org.apache.http.FakeHttpLayer;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;

import android.accounts.Account;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;

import java.io.IOException;
import java.io.InputStream;

@Deprecated
public class TestHelper {
    private TestHelper() {
    }

    public static ObjectMapper getObjectMapper() {
        return PublicApi.buildObjectMapper();
    }

    public static <T> T readJson(Class<T> klazz, String path) throws IOException {
        InputStream is = TestHelper.class.getResourceAsStream(path);
        expect(is).not.toBeNull();
        return getObjectMapper().readValue(is, klazz);
    }

    public static void addPendingHttpResponse(Class klazz, String... resources) throws IOException {
        for (String r : resources) {
            Robolectric.getFakeHttpLayer().addPendingHttpResponse(
                    new TestHttpResponse(200, JsonFixtures.resourceAsBytes(klazz, r)));
        }
    }

    public static void connectedViaWifi(boolean enabled) {
        ConnectivityManager cm = (ConnectivityManager)
                Robolectric.application.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (enabled) {
            // pretend we're connected via wifi
            shadowOf(cm).setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null, ConnectivityManager.TYPE_WIFI,
                    0, true, true));
        } else {
            // pretend we're connected only via mobile, no wifi
            shadowOf(cm).setActiveNetworkInfo(ShadowNetworkInfo.newInstance(null, ConnectivityManager.TYPE_MOBILE,
                    0, true, true));

            NetworkInfo info = ShadowNetworkInfo.newInstance(null, ConnectivityManager.TYPE_WIFI, 0, true, false);
            Robolectric.shadowOf(cm).setNetworkInfo(ConnectivityManager.TYPE_WIFI, info);
        }
    }


    public static void setBackgrounData(boolean enabled) {
        ConnectivityManager cm = (ConnectivityManager)
                Robolectric.application.getSystemService(Context.CONNECTIVITY_SERVICE);

        Robolectric.shadowOf(cm).setBackgroundDataSetting(enabled);
    }

    public static FakeHttpLayer.UriRegexMatcher createRegexRequestMatcherForUriWithClientId(String method, String url) {
        return new FakeHttpLayer.UriRegexMatcher(method, url.replace("?", "\\?") + "(?:[&\\?]client_id=.+)?$");
    }

    public static void setSdkVersion(int version) {
        Robolectric.Reflection.setFinalStaticField(Build.VERSION.class, "SDK_INT", version);
        Robolectric.Reflection.setFinalStaticField(Build.VERSION.class, "SDK", String.valueOf(version));
    }

    public static void enableSDCard() {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);
    }

    public static void disableSDCard() {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_REMOVED);
    }

    public static void setUserId(long id) {
        ShadowAccountManager shadowAccountManager = shadowOf(ShadowAccountManager.get(DefaultTestRunner.application));
        AccountOperations accountOperations = DefaultTestRunner.application.getAccountOperations();

        if (accountOperations.getSoundCloudAccount() == null) {
            shadowAccountManager.addAccount(new Account("name", "com.soundcloud.android.account"));
        }

        accountOperations.setAccountData(USER_ID.getKey(), Long.toString(id));
    }

}
