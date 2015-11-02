package com.soundcloud.android.main;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.analytics.AnalyticsModule;
import com.soundcloud.android.associations.AssociationsModule;
import com.soundcloud.android.comments.TrackCommentsActivity;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.creators.record.UploadActivity;
import com.soundcloud.android.creators.upload.MetadataFragment;
import com.soundcloud.android.profile.VerifyAgeActivity;
import com.soundcloud.android.search.SearchActivity;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                MainActivity.class,
                TrackedActivity.class,
                RecordActivity.class,
                UploadActivity.class,
                MetadataFragment.class,
                DevDrawerFragment.class,
                EmailOptInDialogFragment.class,
                SearchActivity.class,
                TrackCommentsActivity.class,
                VerifyAgeActivity.class
        }, includes = {AssociationsModule.class, AnalyticsModule.class})
public class MainModule {

}
