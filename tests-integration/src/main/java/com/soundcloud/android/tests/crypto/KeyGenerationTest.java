package com.soundcloud.android.tests.crypto;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.framework.TestUser;

public class KeyGenerationTest extends ActivityTest<MainActivity> {

    public KeyGenerationTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        TestUser.streamUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();
    }

    public void testApplicationKeyShouldBeGenerated() {
        DeviceKey key = new DeviceKey(getActivity());
        assertTrue(key.isValid());
    }
}
