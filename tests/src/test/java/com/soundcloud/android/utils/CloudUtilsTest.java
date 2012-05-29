package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;
import static com.xtremelabs.robolectric.Robolectric.shadowOf;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;

@RunWith(DefaultTestRunner.class)
public class CloudUtilsTest {

    @Test
    public void testHexString() throws Exception {
        expect(CloudUtils.hexString(new byte[]{0, 12, 32, 0, 16})).toEqual("000c200010");
    }

    @Test
    public void shouldGetUniqueDeviceId() throws Exception {
        TelephonyManager tmr = (TelephonyManager)
                Robolectric.application.getSystemService(Context.TELEPHONY_SERVICE);

        expect(CloudUtils.getUniqueDeviceID(Robolectric.application)).toBeNull();
        shadowOf(tmr).setDeviceId("MYID");
        expect(CloudUtils.getUniqueDeviceID(Robolectric.application)).toEqual("04ddf8a23b64c654b938b95a50a486f0");
    }

    @Test
    public void shouldGetUniqueDeviceIdWithoutTelephonyManager() throws Exception {
        shadowOf(Robolectric.application).setSystemService(Context.TELEPHONY_SERVICE, null);
        expect(CloudUtils.getUniqueDeviceID(Robolectric.application)).toBeNull();

        Settings.Secure.putString(Robolectric.application.getContentResolver(), Settings.Secure.ANDROID_ID, "foobar");
        expect(CloudUtils.getUniqueDeviceID(Robolectric.application)).toEqual("3858f62230ac3c915f300c664312c63f");
    }

    @Test
    public void shouldFormatTimeString() throws Exception {
        expect(CloudUtils.formatTimestamp(5 * 1000)).toEqual("0.05");
        expect(CloudUtils.formatTimestamp(60 * 1000 * 5)).toEqual("5.00");
        expect(CloudUtils.formatTimestamp(60 * 1000 * 60 * 3)).toEqual("3.00.00");
    }

    @Test
    public void shouldGetTimeString() throws Exception {
        expectTime(1, "1 second");
        expectTime(20, "20 seconds");
        expectTime(60, "1 minute");
        expectTime(60 * 60 * 2.5, "2 hours");
        expectTime(60 * 60 * 24 * 2.5, "2 days");
        expectTime(60 * 60 * 24 * 31, "1 month");
        expectTime(60 * 60 * 24 * 31 * 12 * 2, "2 years");
    }

    @Test
    public void shouldGetElapsedTime() throws Exception {
        expect(CloudUtils.getTimeElapsed(
                Robolectric.application.getResources(),
                System.currentTimeMillis() - 1000 * 60)).toEqual("1 minute");
    }

    private void expectTime(double seconds, String text) {
        expect(CloudUtils.getTimeString(Robolectric.application.getResources(), seconds, false)).toEqual(text);
        expect(CloudUtils.getTimeString(Robolectric.application.getResources(), seconds, true)).toEqual(text + " ago");
    }

    @Test
    public void shouldCheckEmail() throws Exception {
        expect(CloudUtils.checkEmail("foo@bar.com")).toBeTrue();
        expect(CloudUtils.checkEmail("Foo+special@bar.com")).toBeTrue();
        expect(CloudUtils.checkEmail("foo@barcom")).toBeFalse();
        expect(CloudUtils.checkEmail("foobar.com")).toBeFalse();
    }
}