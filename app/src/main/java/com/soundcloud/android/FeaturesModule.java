package com.soundcloud.android;

import com.soundcloud.android.accounts.AccountsModule;
import com.soundcloud.android.activities.ActivitiesModule;
import com.soundcloud.android.associations.LikesModule;
import com.soundcloud.android.comments.CommentsModule;
import com.soundcloud.android.explore.ExploreModule;
import com.soundcloud.android.main.MainModule;
import com.soundcloud.android.payments.PaymentModule;
import com.soundcloud.android.playback.PlayerModule;
import com.soundcloud.android.playlists.PlaylistsModule;
import com.soundcloud.android.profile.ProfileModule;
import com.soundcloud.android.search.SearchModule;
import com.soundcloud.android.stream.SoundStreamModule;
import com.soundcloud.android.tracks.TrackModule;
import dagger.Module;

@Module(includes = {
        AccountsModule.class,
        ActivitiesModule.class,
        SearchModule.class,
        ExploreModule.class,
        LikesModule.class,
        MainModule.class,
        PlayerModule.class,
        PlaylistsModule.class,
        ProfileModule.class,
        SoundStreamModule.class,
        TrackModule.class,
        CommentsModule.class,
        PaymentModule.class
})
public class FeaturesModule {}
