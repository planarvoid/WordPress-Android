package com.soundcloud.android.main;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.activities.ActivitiesAdapter;
import com.soundcloud.android.associations.AssociationsModule;
import com.soundcloud.android.collections.ScListFragment;
import com.soundcloud.android.creators.upload.UploadService;
import com.soundcloud.android.sync.ApiSyncer;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                ScListFragment.class,
                ActivitiesAdapter.class,
                ApiSyncer.class,
                UploadService.class
        }, includes = AssociationsModule.class)

/**
 * Module for legacy classes that need direct injection but are not currently part of injected activities/fragments.
 * We should add to this only when absolutely necessary but should ultimately refactor these features to use injection
 */
@Deprecated
public class LegacyModule {
}
