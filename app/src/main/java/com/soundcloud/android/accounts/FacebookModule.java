package com.soundcloud.android.accounts;

import com.facebook.CallbackManager;
import com.facebook.login.LoginManager;
import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

@Module
public class FacebookModule {
    @Provides
    public CallbackManager providesCallbackManager() {
        return CallbackManager.Factory.create();
    }

    @Singleton
    @Provides
    public LoginManager providesFacebookLoginManager() {
        return LoginManager.getInstance();
    }
}
