package com.soundcloud.android.main;

import com.soundcloud.android.activities.ActivitiesAdapter;
import com.soundcloud.android.associations.SoundAssociationAdapter;
import com.soundcloud.android.associations.TrackInteractionActivity;
import com.soundcloud.android.collections.DefaultPlayableAdapter;
import dagger.Module;

@Module(complete = false,
        injects = {
                ActivitiesAdapter.class,
                SoundAssociationAdapter.class,
                DefaultPlayableAdapter.class,
                TrackInteractionActivity.class
        })

/**
 * Module for legacy classes that need direct injection but are not currently part of injected activities/fragments.
 * We should add to this only when absoultely necessary but should utlimately refactor these features to use injection
 */
@Deprecated
public class LegacyModule {
}
