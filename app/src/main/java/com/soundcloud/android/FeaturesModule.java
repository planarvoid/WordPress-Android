package com.soundcloud.android;

import com.soundcloud.android.accounts.AccountsModule;
import com.soundcloud.android.associations.LikesModule;
import com.soundcloud.android.explore.ExploreModule;
import com.soundcloud.android.main.MainModule;
import com.soundcloud.android.playback.PlayerModule;
import com.soundcloud.android.playlists.PlaylistsModule;
import com.soundcloud.android.search.SearchModule;
import com.soundcloud.android.stream.SoundStreamModule;
import dagger.Module;

@Module(includes = {
        AccountsModule.class,
        SearchModule.class,
        ExploreModule.class,
        LikesModule.class,
        MainModule.class,
        PlayerModule.class,
        PlaylistsModule.class,
        SoundStreamModule.class
})
public class FeaturesModule {
}
