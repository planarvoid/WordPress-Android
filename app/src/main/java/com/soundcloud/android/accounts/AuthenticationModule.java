package com.soundcloud.android.accounts;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.onboarding.FacebookSessionCallback;
import com.soundcloud.android.onboarding.OnboardActivity;
import com.soundcloud.android.onboarding.auth.AddUserInfoTaskFragment;
import com.soundcloud.android.onboarding.auth.AuthenticatorService;
import com.soundcloud.android.onboarding.auth.GooglePlusSignInTaskFragment;
import com.soundcloud.android.onboarding.auth.LoginTaskFragment;
import com.soundcloud.android.onboarding.auth.SignupTaskFragment;
import com.soundcloud.android.onboarding.auth.TokenInformationGenerator;
import com.soundcloud.android.onboarding.auth.tasks.SignupTask;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                AuthenticatorService.class,
                LogoutFragment.class,
                ResolveActivity.class,
                LoginTaskFragment.class,
                GooglePlusSignInTaskFragment.class,
                SignupTaskFragment.class,
                AddUserInfoTaskFragment.class,
                OnboardActivity.class,
                FacebookSessionCallback.class
        })
public class AuthenticationModule {
}
