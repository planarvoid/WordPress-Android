package com.soundcloud.android.auth;

import com.soundcloud.android.screens.auth.SignUpScreen;
import com.soundcloud.android.screens.auth.SuggestedUsersCategoryScreen;
import com.soundcloud.android.screens.auth.SuggestedUsersScreen;

public class SignUpTestCase extends AuthTestCase {

    protected SignUpScreen signUpScreen;
    protected SuggestedUsersScreen suggestedUsersScreen;
    protected SuggestedUsersCategoryScreen suggestedUsersCategoryScreen;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        signUpScreen = new SignUpScreen(solo);
        suggestedUsersScreen = new SuggestedUsersScreen(solo);
        suggestedUsersCategoryScreen = new SuggestedUsersCategoryScreen(solo);
    }
}
