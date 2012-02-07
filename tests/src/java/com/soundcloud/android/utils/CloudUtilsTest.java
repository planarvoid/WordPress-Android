package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.telephony.TelephonyManager;


@RunWith(DefaultTestRunner.class)
public class CloudUtilsTest {
    @Test
    public void testMD5() throws Exception {
        expect(CloudUtils.md5("foo")).toEqual("acbd18db4cc2f85cedef654fccc4a4d8");
        expect(CloudUtils.md5("000012345")).toEqual("4748cdb4de48635e843db0670e1ad47a");
        expect(CloudUtils.md5("00001234588888")).toEqual("1dff78cccd58a9a316d872a9d6d08db2");
    }

    @Test
    public void testHexString() throws Exception {
        expect(CloudUtils.hexString(new byte[] { 0, 12, 32, 0, 16})).toEqual("000c200010");
    }

    @Test
    public void shouldGetDeviceId() throws Exception {
        TelephonyManager tmr = (TelephonyManager)
                Robolectric.application.getSystemService(Context.TELEPHONY_SERVICE);

        expect(CloudUtils.getDeviceID(Robolectric.application)).toBeNull();
        Robolectric.shadowOf(tmr).setDeviceId("MYID");
        expect(CloudUtils.getDeviceID(Robolectric.application)).toEqual("04ddf8a23b64c654b938b95a50a486f0");
    }
}
