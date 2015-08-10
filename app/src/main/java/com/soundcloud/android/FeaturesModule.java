package com.soundcloud.android;

import com.soundcloud.android.accounts.AuthenticationModule;
import com.soundcloud.android.activities.ActivitiesModule;
import com.soundcloud.android.comments.CommentsModule;
import com.soundcloud.android.creators.record.RecordModule;
import com.soundcloud.android.creators.upload.UploadModule;
import com.soundcloud.android.discovery.DiscoveryModule;
import com.soundcloud.android.explore.ExploreModule;
import com.soundcloud.android.likes.LikesModule;
import com.soundcloud.android.main.MainModule;
import com.soundcloud.android.offline.OfflineModule;
import com.soundcloud.android.payments.PaymentModule;
import com.soundcloud.android.playback.PlayerModule;
import com.soundcloud.android.playlists.PlaylistsModule;
import com.soundcloud.android.profile.ProfileModule;
import com.soundcloud.android.search.SearchModule;
import com.soundcloud.android.settings.SettingsModule;
import com.soundcloud.android.stations.StationsModule;
import com.soundcloud.android.stream.SoundStreamModule;
import com.soundcloud.android.tracks.TrackModule;
import dagger.Module;

@Module(includes = {
        AuthenticationModule.class,
        ActivitiesModule.class,
        SearchModule.class,
        ExploreModule.class,
        MainModule.class,
        PlayerModule.class,
        PlaylistsModule.class,
        ProfileModule.class,
        SoundStreamModule.class,
        TrackModule.class,
        CommentsModule.class,
        PaymentModule.class,
        OfflineModule.class,
        RecordModule.class,
        UploadModule.class,
        LikesModule.class,
        SettingsModule.class,
        StationsModule.class,
        DiscoveryModule.class
})
public class FeaturesModule {}
