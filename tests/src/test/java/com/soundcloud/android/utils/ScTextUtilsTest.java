package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class ScTextUtilsTest {

    @Test
    public void testHexString() throws Exception {
        expect(ScTextUtils.hexString(new byte[]{0, 12, 32, 0, 16})).toEqual("000c200010");
    }

    @Test
    public void shouldFormatTimeString() throws Exception {
        expect(ScTextUtils.formatTimestamp(5 * 1000)).toEqual("0.05");
        expect(ScTextUtils.formatTimestamp(60 * 1000 * 5)).toEqual("5.00");
        expect(ScTextUtils.formatTimestamp(60 * 1000 * 60 * 3)).toEqual("3.00.00");
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
        expect(ScTextUtils.getTimeElapsed(
                Robolectric.application.getResources(),
                System.currentTimeMillis() - 1000 * 60)).toEqual("1 minute");
    }

    private void expectTime(double seconds, String text) {
        expect(ScTextUtils.getTimeString(Robolectric.application.getResources(), seconds, false)).toEqual(text);
        expect(ScTextUtils.getTimeString(Robolectric.application.getResources(), seconds, true)).toEqual(text + " ago");
    }

    @Test
    public void testIsEmail() throws Exception {
        expect(ScTextUtils.isEmail(null)).toBeFalse();
        expect(ScTextUtils.isEmail("foo@bar.com")).toBeTrue();
        expect(ScTextUtils.isEmail("Foo+special@bar.com")).toBeTrue();
        expect(ScTextUtils.isEmail("foo@barcom")).toBeFalse();
        expect(ScTextUtils.isEmail("foobar.com")).toBeFalse();
    }
}
