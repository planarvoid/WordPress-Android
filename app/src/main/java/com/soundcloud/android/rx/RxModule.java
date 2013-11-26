package com.soundcloud.android.rx;

import dagger.Module;
import dagger.Provides;
import rx.subscriptions.CompositeSubscription;

@Module(library = true)
public class RxModule {

    @Provides
    public CompositeSubscription provideCompositeSubscription(){
        return new CompositeSubscription();
    }
}
