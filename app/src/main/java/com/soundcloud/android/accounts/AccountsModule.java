package com.soundcloud.android.accounts;

import com.soundcloud.android.onboarding.auth.AuthenticatorService;
import dagger.Module;

@Module(complete = false, injects = {AuthenticatorService.class, LogoutFragment.class})
public class AccountsModule {
}
