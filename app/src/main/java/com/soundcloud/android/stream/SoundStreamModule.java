package com.soundcloud.android.stream;

import com.soundcloud.android.Consts;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.rx.DatabaseScheduler;
import dagger.Module;
import dagger.Provides;

import android.content.Context;
import android.support.v4.app.NotificationCompat;

import javax.inject.Provider;

@Module(complete = false, library = true, injects = {SoundStreamFragment.class})
public class SoundStreamModule {

    @Provides
    public ISoundStreamStorage provideSoundStreamStorage(FeatureFlags featureFlags, DatabaseScheduler databaseScheduler, PropellerDatabase database){
        if (featureFlags.isEnabled(Flag.API_MOBILE_STREAM)){
            return new SoundStreamStorage(databaseScheduler, database);
        } else {
            return new LegacySoundStreamStorage(databaseScheduler, database);
        }
    }

    @Provides
    public StreamNotificationBuilder provideStreamNotificationBuilder(Context context, ImageOperations imageOperations, Provider<NotificationCompat.Builder> builderProvider){
        if (Consts.SdkSwitches.USE_RICH_NOTIFICATIONS ) {
            return new RichStreamNotificationBuilder(context, imageOperations, builderProvider);
        } else {
            return new StreamNotificationBuilder(context, builderProvider);
        }
    }

}
