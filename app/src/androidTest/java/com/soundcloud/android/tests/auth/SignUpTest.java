package com.soundcloud.android.tests.auth;

import com.soundcloud.android.screens.HomeScreen;
import com.soundcloud.android.screens.auth.SignUpBasicsScreen;
import com.soundcloud.android.screens.auth.SignUpMethodScreen;
import com.soundcloud.android.screens.auth.SuggestedUsersCategoryScreen;
import com.soundcloud.android.screens.auth.SuggestedUsersScreen;

public class SignUpTest extends AuthTest {
    protected HomeScreen homeScreen;
    protected SignUpMethodScreen signUpMethodScreen;
    protected SuggestedUsersScreen suggestedUsersScreen;
    protected SuggestedUsersCategoryScreen suggestedUsersCategoryScreen;
    protected SignUpBasicsScreen signUpBasicsScreen;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        homeScreen = new HomeScreen(solo);
    }
}
