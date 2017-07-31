package com.soundcloud.android.collection;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.storage.SqlBriteDatabase;
import io.reactivex.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class CollectionDatabase extends SqlBriteDatabase {

    @Inject
    public CollectionDatabase(CollectionDatabaseOpenHelper databaseOpenHelper, @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler) {
        super(databaseOpenHelper, scheduler);
    }
}
