package com.soundcloud.android.playback;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.storage.SqlBriteDatabase;
import io.reactivex.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class PlayQueueDatabase extends SqlBriteDatabase {

    @Inject
    public PlayQueueDatabase(PlayQueueDatabaseOpenHelper databaseOpenHelper, @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler) {
        super(databaseOpenHelper, scheduler);
    }
}
