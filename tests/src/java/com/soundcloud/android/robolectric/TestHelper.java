package com.soundcloud.android.robolectric;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;

import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DelegatingContentResolver;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.tester.org.apache.http.FakeHttpLayer;

import android.database.Cursor;
import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

public class TestHelper {
    private TestHelper() {}

    public static void addCannedResponses(Class klazz, String... resources) throws IOException {
        for (String r : resources) {
            addPendingHttpResponse(200, resource(klazz, r));
        }
    }

    public static void addCannedResponse(Class klazz, String url, String resource) throws IOException {
        Robolectric.addHttpResponseRule(url, resource(klazz, resource));
    }

    public static void addPendingIOException(String path) {
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
        while ((n = is.read(buffer)) != -1) sb.append(new String(buffer, 0, n));
        return sb.toString();
    }

    public static void assertContentUriCount(Content content, int count) {
        assertContentUriCount(content.uri, count);
    }

    public static void assertContentUriCount(Uri uri, int count) {
        Cursor c = Robolectric.application.getContentResolver().query(uri, null, null, null, null);
        expect(c).not.toBeNull();
        expect(c.getCount()).toEqual(count);
    }

    public static void assertFirstIdToBe(Content content, long id) {
        Cursor c = Robolectric.application.getContentResolver().query(content.uri, null, null, null, null);
        expect(c).not.toBeNull();
        c.moveToFirst();
        expect(c.getLong(c.getColumnIndex("_id"))).toEqual(id);
    }

    public static void assertResolverNotified(Uri... uris) {
        DelegatingContentResolver res  =
                Robolectric.shadowOf_(Robolectric.application.getContentResolver());
        Set<Uri> _uris = new HashSet<Uri>();
        for (DelegatingContentResolver.NotifiedUri u : res.getNotifiedUris()) {
            _uris.add(u.uri);
        }
        expect(_uris).toContain(uris);
    }
}
