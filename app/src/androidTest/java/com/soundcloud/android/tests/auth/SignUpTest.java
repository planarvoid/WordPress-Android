package com.soundcloud.android.tests.auth;

import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.screens.auth.SignUpBasicsScreen;
import com.soundcloud.android.screens.auth.SignUpMethodScreen;

public class SignUpTest extends AuthTest {
    protected HomeScreen homeScreen;
    protected SignUpMethodScreen signUpMethodScreen;
    protected SignUpBasicsScreen signUpBasicsScreen;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        homeScreen = new HomeScreen(solo);
    }
}
