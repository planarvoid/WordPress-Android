package com.soundcloud.android.robolectric;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;

import com.soundcloud.android.provider.Content;
import com.xtremelabs.robolectric.Robolectric;

import android.database.Cursor;

import java.io.IOException;
import java.io.InputStream;

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

    public static String resource(Class klazz, String res) throws IOException {
        StringBuilder sb = new StringBuilder(65536);
        int n;
        byte[] buffer = new byte[8192];
        InputStream is = klazz.getResourceAsStream(res);
        while ((n = is.read(buffer)) != -1) sb.append(new String(buffer, 0, n));
        return sb.toString();
    }

    public static void assertContentUriCount(Content content, int count) {
        Cursor c = Robolectric.application.getContentResolver().query(content.uri, null, null, null, null);
        expect(c).not.toBeNull();
        expect(c.getCount()).toEqual(count);
    }
}
