package com.soundcloud.android.tests.crypto;

import com.soundcloud.android.framework.TestUser;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.tests.ActivityTest;

public class KeyGenerationTest extends ActivityTest<MainActivity> {

    public KeyGenerationTest() {
        super(MainActivity.class);
    }

    @Override
    protected void logInHelper() {
        TestUser.streamUser.logIn(getInstrumentation().getTargetContext());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    public void testApplicationKeyShouldBeGenerated() {
        DeviceKey key = new DeviceKey(getActivity());
        assertTrue(key.isValid());
    }
}
