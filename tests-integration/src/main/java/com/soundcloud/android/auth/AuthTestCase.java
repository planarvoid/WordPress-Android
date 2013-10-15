package com.soundcloud.android.auth;

import com.soundcloud.android.activity.auth.Onboard;
import com.soundcloud.android.screens.auth.SignUpScreen;
import com.soundcloud.android.tests.ActivityTestCase;

public class AuthTestCase extends ActivityTestCase<Onboard> {

    protected SignUpScreen signupScreen;

    public AuthTestCase() {
        super(Onboard.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        signupScreen = new SignUpScreen(solo);
    }

    protected String generateEmail() {
        return "someemail-"+System.currentTimeMillis()+"@test.com";
    }

}
