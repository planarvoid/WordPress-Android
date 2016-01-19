package com.soundcloud.android.payments;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                UpgradeActivity.class,
                UpgradeProgressActivity.class,
        })
public class PaymentModule {
}
