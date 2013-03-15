package com.soundcloud.android.robolectric;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.*;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.SoundAssociationHolder;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.utils.IOUtils;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowContentResolver;
import com.xtremelabs.robolectric.shadows.ShadowEnvironment;
import com.xtremelabs.robolectric.shadows.ShadowNetworkInfo;
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

    public static ObjectMapper getObjectMapper() {
        return AndroidCloudAPI.Wrapper.buildObjectMapper();
    }

    public static <T> T readJson(Class<T> klazz, String path) throws IOException {
        InputStream is = TestHelper.class.getResourceAsStream(path);
        expect(is).not.toBeNull();
        return getObjectMapper().readValue(is, klazz);
    }

    @SuppressWarnings("unchecked")
    public static <T extends ScResource> T readResource(String path) throws IOException {
        InputStream is = TestHelper.class.getResourceAsStream(path);
        return (T) getObjectMapper().readValue(is, ScResource.class);
    }

    public static <T> T readJson(Class<T> modelClass, Class<?> lookupClass, String file) throws IOException {
        InputStream is = lookupClass.getResourceAsStream(file);
        expect(is).not.toBeNull();
        return getObjectMapper().readValue(is, modelClass);
    }

    public static void addPendingHttpResponse(Class klazz, String... resources) throws IOException {
        for (String r : resources) {
            Robolectric.getFakeHttpLayer().addPendingHttpResponse(
                    new TestHttpResponse(200, resourceAsBytes(klazz, r)));
        }
    }

    public static void addCannedResponse(Class klazz, String url, String resource) throws IOException {
        Robolectric.getFakeHttpLayer().addHttpResponseRule(url,
                new TestHttpResponse(200, resourceAsBytes(klazz, resource)));
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

    public static String resourceAsString(Class klazz, String res) throws IOException {
        InputStream is = klazz.getResourceAsStream(res);
        expect(is).not.toBeNull();
        return IOUtils.readInputStream(is);
    }

    public static byte[] resourceAsBytes(Class klazz, String res) throws IOException {
        InputStream is = klazz.getResourceAsStream(res);
        expect(is).not.toBeNull();
        return IOUtils.readInputStreamAsBytes(is);
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

    public static void simulateOnline() {
        ConnectivityManager cm = (ConnectivityManager)
                Robolectric.application.getSystemService(Context.CONNECTIVITY_SERVICE);
        shadowOf(shadowOf(cm).getActiveNetworkInfo()).setConnectionStatus(true);
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

    public static void setSdkVersion(int version) {
        Robolectric.Reflection.setFinalStaticField(Build.VERSION.class, "SDK_INT", version);
    }

    public static void enableSDCard(){
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);
    }

    public static void disableSDCard() {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_REMOVED);
    }
}
