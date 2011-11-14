package com.soundcloud.android.service.beta;

import static android.content.Context.WIFI_SERVICE;
import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.addPendingHttpResponse;
import static com.xtremelabs.robolectric.Robolectric.application;
import static com.xtremelabs.robolectric.Robolectric.newInstanceOf;

import com.soundcloud.android.Expect;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowConnectivityManager;
import com.xtremelabs.robolectric.shadows.ShadowEnvironment;
import com.xtremelabs.robolectric.shadows.ShadowService;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;

import java.io.IOException;
import java.io.InputStream;


@RunWith(DefaultTestRunner.class)
@Ignore
public class BetaServiceTest {
    BetaService bs;
/*
    @Before
    public void before() {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);
        ShadowConnectivityManager.setBackgroundDataSettingEnabled(true);
        ConnectivityManager cm = (ConnectivityManager)
                Robolectric.application.getSystemService(Context.CONNECTIVITY_SERVICE);
        Robolectric.shadowOf(cm).setNetworkInfo(ConnectivityManager.TYPE_WIFI,
                newInstanceOf(NetworkInfo.class));

        bs = new BetaService();
    }

    @After
    public void after() {
        expect(Robolectric.getFakeHttpLayer().hasPendingResponses()).toBeFalse();
    }

    @Test
    public void testOnStartCommand() throws Exception {
        addPendingHttpResponse(200, resource("bucket_contents.xml"));

        // HEAD http://soundcloud-android-beta.s3.amazonaws.com/com.soundcloud.android-28.apk

        addPendingHttpResponse(200, "", headers(
                "x-amz-meta-android-versioncode", "27",
                "x-amz-meta-android-versionname: 1.4.5-BETA1",
                "x-amz-meta-git-sha1",  "e800b4bedadc6308ebcf72c566bf95d7b9cee30f"));



        bs.onStartCommand(null, 0, 0);
        expect(Robolectric.shadowOf(bs).isStoppedBySelf()).toBeTrue();
    }

    protected String resource(String res) throws IOException {
        StringBuilder sb = new StringBuilder(65536);
        int n;
        byte[] buffer = new byte[8192];
        InputStream is = getClass().getResourceAsStream(res);
        while ((n = is.read(buffer)) != -1) sb.append(new String(buffer, 0, n));
        return sb.toString();
    }

    public static Header[] headers(String... keyValues) {
        Header[] headers = new Header[keyValues.length / 2];
        for (int i = 0; i < keyValues.length / 2; i++) {
            headers[i] = new BasicHeader(keyValues[i * 2], keyValues[i * 2 + 1]);
        }
        return headers;
    }
*/
}
