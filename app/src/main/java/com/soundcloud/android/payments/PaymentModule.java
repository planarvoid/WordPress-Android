package com.soundcloud.android.payments;

import com.soundcloud.android.ApplicationModule;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                NativeConversionActivity.class,
                WebConversionActivity.class,
                WebCheckoutActivity.class
        })
public class PaymentModule {
}
