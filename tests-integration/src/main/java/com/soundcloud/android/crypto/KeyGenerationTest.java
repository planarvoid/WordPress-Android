package com.soundcloud.android.crypto;

import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.TestUser;

public class KeyGenerationTest extends ActivityTestCase<MainActivity> {

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
