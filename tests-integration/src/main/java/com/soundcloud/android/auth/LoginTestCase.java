package com.soundcloud.android.auth;

import com.soundcloud.android.screens.auth.LoginScreen;

public class LoginTestCase extends AuthTestCase {

    protected LoginScreen loginScreen;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        loginScreen = new LoginScreen(solo);
    }
}
