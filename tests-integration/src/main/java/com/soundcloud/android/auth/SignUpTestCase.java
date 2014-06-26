package com.soundcloud.android.auth;

import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.screens.auth.SignUpScreen;
import com.soundcloud.android.screens.auth.SuggestedUsersCategoryScreen;
import com.soundcloud.android.screens.auth.SuggestedUsersScreen;
import com.soundcloud.android.tests.AccountAssistant;

public class SignUpTestCase extends AuthTestCase {
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
