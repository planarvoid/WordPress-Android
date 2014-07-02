package com.soundcloud.android.main;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.actionbar.NowPlayingProgressBar;
import com.soundcloud.android.activities.ActivitiesAdapter;
import com.soundcloud.android.associations.AssociationsModule;
import com.soundcloud.android.associations.TrackInteractionActivity;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.playback.views.WaveformControllerLayout;
import com.soundcloud.android.profile.MyTracksAdapter;
import com.soundcloud.android.view.adapters.PostsAdapter;
import com.soundcloud.android.view.adapters.SoundAdapter;
import com.soundcloud.android.view.adapters.UserAdapter;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                ScListFragment.class,
                UserAdapter.class,
                ActivitiesAdapter.class,
                SoundAdapter.class,
                PostsAdapter.class,
                TrackInteractionActivity.class,
                MyTracksAdapter.class,
                WaveformControllerLayout.class,
                NowPlayingProgressBar.class
        }, includes = AssociationsModule.class)

/**
 * Module for legacy classes that need direct injection but are not currently part of injected activities/fragments.
 * We should add to this only when absoultely necessary but should utlimately refactor these features to use injection
 */
@Deprecated
public class LegacyModule {
}
