package com.soundcloud.android.accounts;

import com.facebook.CallbackManager;
import com.facebook.login.LoginManager;
import dagger.Module;
import dagger.Provides;

@Module(complete = false, library = true)
public class FacebookModule {
    @Provides
    public CallbackManager providesCallbackManager() {
        return CallbackManager.Factory.create();
    }

    @Provides
    public LoginManager providesFacebookLoginManager() {
        return LoginManager.getInstance();
    }
}
