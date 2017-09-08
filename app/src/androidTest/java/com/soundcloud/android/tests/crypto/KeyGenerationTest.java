package com.soundcloud.android.tests.crypto;

import static com.soundcloud.android.framework.TestUser.streamUser;
import static junit.framework.Assert.assertTrue;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.tests.ActivityTest;
import org.junit.Test;

public class KeyGenerationTest extends ActivityTest<MainActivity> {

    public KeyGenerationTest() {
        super(MainActivity.class);
    }

    @Override
    protected TestUser getUserForLogin() {
        return streamUser;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Test
    public void testApplicationKeyShouldBeGenerated() throws Exception {
        DeviceKey key = new DeviceKey(activityTestRule.getActivity());
        assertTrue(key.isValid());
    }
}
