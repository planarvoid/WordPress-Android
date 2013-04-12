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

import java.util.Arrays;
import java.util.HashMap;

@RunWith(DefaultTestRunner.class)
public class AndroidUtilsTest {
    @Test
    public void shouldGetUniqueDeviceId() throws Exception {
        TelephonyManager tmr = (TelephonyManager)
                Robolectric.application.getSystemService(Context.TELEPHONY_SERVICE);

        expect(AndroidUtils.getUniqueDeviceID(Robolectric.application)).toBeNull();
        shadowOf(tmr).setDeviceId("MYID");
        expect(AndroidUtils.getUniqueDeviceID(Robolectric.application)).toEqual("04ddf8a23b64c654b938b95a50a486f0");
    }

    @Test
    public void shouldGetUniqueDeviceIdWithoutTelephonyManager() throws Exception {
        shadowOf(Robolectric.application).setSystemService(Context.TELEPHONY_SERVICE, null);
        expect(AndroidUtils.getUniqueDeviceID(Robolectric.application)).toBeNull();

        Settings.Secure.putString(Robolectric.application.getContentResolver(), Settings.Secure.ANDROID_ID, "foobar");
        expect(AndroidUtils.getUniqueDeviceID(Robolectric.application)).toEqual("3858f62230ac3c915f300c664312c63f");
    }

    @Test
    public void shouldReturnKeysSortedByValue() throws Exception {
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        map.put("d",1);
        map.put("b",3);
        map.put("a",4);
        map.put("c",2);

        final String[] actual = AndroidUtils.returnKeysSortedByValue(map);
        final String[] expected = {"a", "b", "c", "d"};

        expect(Arrays.equals(actual, expected)).toBeTrue();
    }
}