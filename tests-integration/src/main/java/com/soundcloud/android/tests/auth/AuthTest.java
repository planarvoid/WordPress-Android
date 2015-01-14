package com.soundcloud.android.tests.auth;


import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.auth.SignUpScreen;
import com.soundcloud.android.tests.ActivityTest;

public class AuthTest extends ActivityTest<LauncherActivity> {

    protected SignUpScreen signupScreen;

    public AuthTest() {
        super(LauncherActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        signupScreen = new SignUpScreen(solo);
    }
}
