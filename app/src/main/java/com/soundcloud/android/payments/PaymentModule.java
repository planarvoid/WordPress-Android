package com.soundcloud.android.payments;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
            SubscribeActivity.class,
            SubscribeSuccessActivity.class
        })
public class PaymentModule {}
