package com.soundcloud.android;

import com.soundcloud.android.accounts.AccountsModule;
import com.soundcloud.android.analytics.AnalyticsModule;
import com.soundcloud.android.api.ApiModule;
import com.soundcloud.android.associations.LikesModule;
import com.soundcloud.android.explore.ExploreModule;
import com.soundcloud.android.main.MainModule;
import com.soundcloud.android.playback.PlaybackModule;
import com.soundcloud.android.playlists.PlaylistsModule;
import com.soundcloud.android.search.SearchModule;
import com.soundcloud.android.storage.StorageModule;
import dagger.Module;

@Module(includes = {
        StorageModule.class,
        ApiModule.class,
        AnalyticsModule.class,
        AccountsModule.class,
        SearchModule.class,
        ExploreModule.class,
        LikesModule.class,
        MainModule.class,
        PlaybackModule.class,
        PlaylistsModule.class
}, addsTo = ApplicationModule.class)
public class SoundCloudModule {
}
