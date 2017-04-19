package com.soundcloud.android.di.testimplementations;

import static org.mockito.Mockito.spy;

import com.facebook.login.LoginManager;
import com.soundcloud.android.accounts.FacebookModule;

public class TestFacebookModule extends FacebookModule {

    @Override
    public LoginManager providesFacebookLoginManager() {
        return spy(super.providesFacebookLoginManager());
    }
}
