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
    public void testHexString() throws Exception {
        expect(CloudUtils.hexString(new byte[] { 0, 12, 32, 0, 16})).toEqual("000c200010");
    }

    @Test
    public void shouldGetUniqueDeviceId() throws Exception {
        TelephonyManager tmr = (TelephonyManager)
                Robolectric.application.getSystemService(Context.TELEPHONY_SERVICE);

        expect(CloudUtils.getUniqueDeviceID(Robolectric.application)).toBeNull();
        Robolectric.shadowOf(tmr).setDeviceId("MYID");
        expect(CloudUtils.getUniqueDeviceID(Robolectric.application)).toEqual("04ddf8a23b64c654b938b95a50a486f0");
    }
}
