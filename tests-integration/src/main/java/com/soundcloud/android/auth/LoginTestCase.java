package com.soundcloud.android.auth;

import com.soundcloud.android.screens.auth.LoginScreen;
import com.soundcloud.android.tests.IntegrationTestHelper;

public class LoginTestCase extends AuthTestCase {

    protected LoginScreen loginScreen;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        loginScreen = new LoginScreen(solo);
    }

    @Override
    public void tearDown() throws Exception {
        IntegrationTestHelper.logOut(getInstrumentation());
        super.tearDown();
    }
}
