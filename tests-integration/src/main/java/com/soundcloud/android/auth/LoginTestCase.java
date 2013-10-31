package com.soundcloud.android.auth;

import com.soundcloud.android.screens.auth.LoginScreen;
import com.soundcloud.android.tests.AccountAssistant;

public class LoginTestCase extends AuthTestCase {

    protected LoginScreen loginScreen;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        loginScreen = new LoginScreen(solo);
    }

    @Override
    public void tearDown() throws Exception {
        AccountAssistant.logOut(getInstrumentation());
        super.tearDown();
    }
}
