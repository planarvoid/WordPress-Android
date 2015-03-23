package com.soundcloud.android.tests.auth;


import com.soundcloud.android.main.LauncherActivity;
import com.soundcloud.android.screens.auth.SignUpMethodScreen;
import com.soundcloud.android.tests.ActivityTest;

public class AuthTest extends ActivityTest<LauncherActivity> {

    protected SignUpMethodScreen signupMethodScreen;

    public AuthTest() {
        super(LauncherActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        signupMethodScreen = new SignUpMethodScreen(solo);
    }
}
