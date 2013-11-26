package com.soundcloud.android.rx;

import dagger.Module;
import dagger.Provides;
import rx.Scheduler;
import rx.subscriptions.CompositeSubscription;

import javax.inject.Named;

@Module(library = true)
public class RxModule {

    @Provides
    public CompositeSubscription provideCompositeSubscription(){
        return new CompositeSubscription();
    }

    @Provides
    @Named("APIScheduler")
    public Scheduler provideApiScheduler(){
        return ScSchedulers.API_SCHEDULER;
    }

    @Provides
    @Named("StorageScheduler")
    public Scheduler provideStorageScheduler(){
        return ScSchedulers.STORAGE_SCHEDULER;
    }

}
