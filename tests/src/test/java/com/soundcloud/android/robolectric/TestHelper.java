package com.soundcloud.android.robolectric;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;
import static com.xtremelabs.robolectric.Robolectric.newInstanceOf;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.soundcloud.android.provider.Content;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import com.xtremelabs.robolectric.shadows.ShadowContentResolver;
import com.xtremelabs.robolectric.shadows.ShadowEnvironment;
import com.xtremelabs.robolectric.tester.org.apache.http.FakeHttpLayer;
import com.xtremelabs.robolectric.tester.org.apache.http.TestHttpResponse;

import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class TestHelper {
    private TestHelper() {}

    // TODO: rename to addPendingHttpResponse
    public static void addCannedResponses(Class klazz, String... resources) throws IOException {
        for (String r : resources) {
            addPendingHttpResponse(200, resource(klazz, r));
        }
    }

    public static void addCannedResponse(Class klazz, String url, String resource) throws IOException {
        Robolectric.addHttpResponseRule(url, resource(klazz, resource));
    }

    public static void addResponseRule(String uri, int status) {
        Robolectric.addHttpResponseRule(uri, new TestHttpResponse(status, ""));
    }

    public static void addPendingIOException(String path) {
        if (path != null && path.startsWith("/")) {
            path = path.substring(1, path.length());
        }

        FakeHttpLayer.RequestMatcherBuilder builder = new FakeHttpLayer.RequestMatcherBuilder();
        if (path != null) {
            builder.path(path);
        }
        Robolectric.getFakeHttpLayer().addHttpResponseRule(
                new FakeHttpLayer.RequestMatcherResponseRule(builder, new IOException("boom")));
    }

    public static String resource(Class klazz, String res) throws IOException {
        StringBuilder sb = new StringBuilder(65536);
        int n;
        byte[] buffer = new byte[8192];
        InputStream is = klazz.getResourceAsStream(res);
        expect(is).not.toBeNull();
        while ((n = is.read(buffer)) != -1) sb.append(new String(buffer, 0, n));
        return sb.toString();
    }

    public static void assertFirstIdToBe(Content content, long id) {
        Cursor c = Robolectric.application.getContentResolver().query(content.uri, null, null, null, null);
        expect(c).not.toBeNull();
        c.moveToFirst();
        expect(c.getLong(c.getColumnIndex("_id"))).toEqual(id);
    }

    public static void assertResolverNotified(Uri... uris) {
        ShadowContentResolver res  =
                Robolectric.shadowOf_(Robolectric.application.getContentResolver());
        Set<Uri> _uris = new HashSet<Uri>();
        for (ShadowContentResolver.NotifiedUri u : res.getNotifiedUris()) {
            _uris.add(u.uri);
        }
        expect(_uris).toContain(uris);
    }

    public static void simulateOffline() {
        ConnectivityManager cm = (ConnectivityManager)
                Robolectric.application.getSystemService(Context.CONNECTIVITY_SERVICE);
        shadowOf(shadowOf(cm).getActiveNetworkInfo()).setConnectionStatus(false);
    }

    public static void connectedViaWifi(boolean enabled) {
        ConnectivityManager cm = (ConnectivityManager)
                Robolectric.application.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (enabled) {
            // pretend we're connected via wifi
            Robolectric.shadowOf(cm).setNetworkInfo(ConnectivityManager.TYPE_WIFI,
                    newInstanceOf(NetworkInfo.class));
        } else {
            // pretend we're connected only via mobile, no wifi
            Robolectric.shadowOf(cm).setNetworkInfo(ConnectivityManager.TYPE_MOBILE,
                    newInstanceOf(NetworkInfo.class));

            NetworkInfo info = newInstanceOf(NetworkInfo.class);
            Robolectric.shadowOf(info).setConnectionStatus(false);
            Robolectric.shadowOf(cm).setNetworkInfo(ConnectivityManager.TYPE_WIFI, info);
        }
    }


    public static void setBackgrounData(boolean enabled) {
        ConnectivityManager cm = (ConnectivityManager)
                Robolectric.application.getSystemService(Context.CONNECTIVITY_SERVICE);

        Robolectric.shadowOf(cm).setBackgroundDataSetting(enabled);
    }

    public static void enableFlightmode(boolean enabled) {
        Settings.System.putInt(Robolectric.application.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, enabled ? 1 : 0);
    }

    public static void addIdResponse(String url, int... ids) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ \"collection\": [");
        for (int i = 0; i < ids.length; i++) {
            sb.append(ids[i]);
            if (i < ids.length - 1) sb.append(", ");
        }
        sb.append("] }");
        Robolectric.addHttpResponseRule(url, new TestHttpResponse(200, sb.toString()));
    }

    public static void setSDCardMounted() {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);
    }

    public static void setSdkVersion(int version) {
        Robolectric.Reflection.setFinalStaticField(Build.VERSION.class, "SDK_INT", version);
    }
}
