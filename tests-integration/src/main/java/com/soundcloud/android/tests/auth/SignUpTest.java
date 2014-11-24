package com.soundcloud.android.tests.auth;

import com.soundcloud.android.framework.screens.HomeScreen;
import com.soundcloud.android.framework.screens.auth.SignUpScreen;
import com.soundcloud.android.framework.screens.auth.SuggestedUsersCategoryScreen;
import com.soundcloud.android.framework.screens.auth.SuggestedUsersScreen;

public class SignUpTest extends AuthTest {
    protected HomeScreen homeScreen;
    protected SignUpScreen signUpScreen;
    protected SuggestedUsersScreen suggestedUsersScreen;
    protected SuggestedUsersCategoryScreen suggestedUsersCategoryScreen;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        homeScreen = new HomeScreen(solo);
    }
}
