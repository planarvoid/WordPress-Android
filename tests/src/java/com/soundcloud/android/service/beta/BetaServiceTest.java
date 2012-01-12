package com.soundcloud.android.service.beta;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.*;

import com.soundcloud.android.Consts;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowEnvironment;
import com.xtremelabs.robolectric.shadows.ShadowNotificationManager;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;


@RunWith(DefaultTestRunner.class)
public class BetaServiceTest {
    BetaService bs;
    @Before
    public void before() {
        ShadowEnvironment.setExternalStorageState(Environment.MEDIA_MOUNTED);
        ConnectivityManager cm = (ConnectivityManager)
                Robolectric.application.getSystemService(Context.CONNECTIVITY_SERVICE);

        Robolectric.shadowOf(cm).setBackgroundDataSetting(true);
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
        TestHelper.addCannedResponses(getClass(), "bucket_contents.xml");

        // HEAD http://soundcloud-android-beta.s3.amazonaws.com/com.soundcloud.android-28.apk
        addPendingHttpResponse(200, "", headers(
                "x-amz-meta-android-versioncode", "27",
                "x-amz-meta-android-versionname: 1.4.5-BETA1",
                "x-amz-meta-git-sha1",  "e800b4bedadc6308ebcf72c566bf95d7b9cee30f"));


        // GET http://soundcloud-android-beta.s3.amazonaws.com/com.soundcloud.android-28.apk
        addPendingHttpResponse(200, "foo-baz-bar");

        bs.onStartCommand(null, 0, 0);
        expect(Robolectric.shadowOf(bs).isStoppedBySelf()).toBeTrue();

        ShadowNotificationManager m = shadowOf((NotificationManager)
            Robolectric.getShadowApplication().getSystemService(Context.NOTIFICATION_SERVICE));

        Notification n = m.getNotification(Consts.Notifications.BETA_NOTIFY_ID);

        expect(n).not.toBeNull();
        expect(n.tickerText).toEqual("Beta update");
        expect(shadowOf(n).getLatestEventInfo().getContentTitle()).toEqual("New beta version downloaded");
    }


    public static Header[] headers(String... keyValues) {
        Header[] headers = new Header[keyValues.length / 2];
        for (int i = 0; i < keyValues.length / 2; i++) {
            headers[i] = new BasicHeader(keyValues[i * 2], keyValues[i * 2 + 1]);
        }
        return headers;
    }
}
