package com.soundcloud.android.auth;

import com.soundcloud.android.screens.auth.SignUpScreen;

public class SignUpTestCase extends AuthTestCase {

    protected SignUpScreen signUpScreen;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        signUpScreen = new SignUpScreen(solo);
    }
}
