package com.soundcloud.android.tests.auth;

import com.soundcloud.android.screens.auth.LoginScreen;

public class LoginTest extends AuthTest {

    protected LoginScreen loginScreen;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        loginScreen = new LoginScreen(solo);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }
}
