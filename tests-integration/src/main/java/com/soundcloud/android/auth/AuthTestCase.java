package com.soundcloud.android.auth;

import com.soundcloud.android.activity.auth.Onboard;
import com.soundcloud.android.screens.auth.OnboardScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import com.soundcloud.android.tests.IntegrationTestHelper;

public class AuthTestCase extends ActivityTestCase<Onboard> {

    protected OnboardScreen onboardScreen;

    public AuthTestCase() {
        super(Onboard.class);
    }

    @Override
    public void setUp() throws Exception {
        IntegrationTestHelper.logOut(getInstrumentation());
        super.setUp();
        onboardScreen = new OnboardScreen(solo);
    }

    protected String generateEmail() {
        return "someemail-"+System.currentTimeMillis()+"@test.com";
    }

}
