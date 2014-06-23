package com.soundcloud.android.accounts;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.deeplinks.ResolveActivity;
import com.soundcloud.android.onboarding.auth.AuthenticatorService;
import dagger.Module;

@Module(addsTo = ApplicationModule.class, injects = {AuthenticatorService.class, LogoutFragment.class, ResolveActivity.class})
public class AccountsModule {
}
