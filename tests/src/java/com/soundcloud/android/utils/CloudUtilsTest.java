package com.soundcloud.android.utils;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.soundcloud.android.robolectric.DefaultTestRunner;

import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


@RunWith(DefaultTestRunner.class)
public class CloudUtilsTest {
    @Test
    public void testMD5() throws Exception {
        assertThat(CloudUtils.md5("foo"), equalTo("acbd18db4cc2f85cedef654fccc4a4d8"));
        assertThat(CloudUtils.md5("000012345"), equalTo("4748cdb4de48635e843db0670e1ad47a"));
        assertThat(CloudUtils.md5("00001234588888"), equalTo("1dff78cccd58a9a316d872a9d6d08db2"));
    }

    @Test
    public void testHexString() throws Exception {
        assertThat(CloudUtils.hexString(new byte[] { 0, 12, 32, 0, 16}), equalTo("000c200010"));
    }
}
