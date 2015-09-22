package com.soundcloud.android.main;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.analytics.AnalyticsModule;
import com.soundcloud.android.associations.AssociationsModule;
import com.soundcloud.android.comments.TrackCommentsActivity;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.creators.record.UploadActivity;
import com.soundcloud.android.creators.upload.MetadataFragment;
import com.soundcloud.android.profile.LegacyProfileActivity;
import com.soundcloud.android.profile.MeActivity;
import com.soundcloud.android.profile.VerifyAgeActivity;
import com.soundcloud.android.search.SearchActivity;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                MainActivity.class,
                TrackedActivity.class,
                MeActivity.class,
                RecordActivity.class,
                UploadActivity.class,
                MetadataFragment.class,
                NavigationFragment.class,
                DevDrawerFragment.class,
                NavigationDrawerFragment.class,
                EmailOptInDialogFragment.class,
                SearchActivity.class,
                TrackCommentsActivity.class,
                LegacyProfileActivity.class,
                VerifyAgeActivity.class
        }, includes = {AssociationsModule.class, AnalyticsModule.class})
public class MainModule {
}
