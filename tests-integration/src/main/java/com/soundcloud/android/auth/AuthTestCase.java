package com.soundcloud.android.auth;

import com.soundcloud.android.activity.auth.OnboardActivity;
import com.soundcloud.android.screens.auth.SignUpScreen;
import com.soundcloud.android.tests.ActivityTestCase;

public class AuthTestCase extends ActivityTestCase<OnboardActivity> {

    protected SignUpScreen signupScreen;

    public AuthTestCase() {
        super(OnboardActivity.class);
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
