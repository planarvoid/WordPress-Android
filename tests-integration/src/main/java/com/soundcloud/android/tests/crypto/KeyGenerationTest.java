package com.soundcloud.android.tests.crypto;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.tests.ActivityTest;
import com.soundcloud.android.framework.TestUser;

public class KeyGenerationTest extends ActivityTest<MainActivity> {

    public KeyGenerationTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        setDependsOn(Feature.APP_KEY_GENERATION);
        TestUser.streamUser.logIn(getInstrumentation().getTargetContext());
        super.setUp();
    }

    public void testApplicationKeyShouldBeGenerated() {
        ApplicationKey key = new ApplicationKey(getActivity());
        assertTrue(key.isValid());
    }
}
