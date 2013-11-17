package com.soundcloud.android.auth;


import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.screens.auth.SignUpScreen;
import com.soundcloud.android.tests.ActivityTestCase;
import sun.misc.Launcher;

public class AuthTestCase extends ActivityTestCase<LauncherActivity> {

    protected SignUpScreen signupScreen;

    public AuthTestCase() {
        super(LauncherActivity.class);
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
