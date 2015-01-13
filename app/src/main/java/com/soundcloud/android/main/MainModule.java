package com.soundcloud.android.main;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.analytics.AnalyticsModule;
import com.soundcloud.android.associations.AssociationsModule;
import com.soundcloud.android.associations.WhoToFollowActivity;
import com.soundcloud.android.comments.TrackCommentsActivity;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.creators.upload.UploadActivity;
import com.soundcloud.android.onboarding.suggestions.SuggestedUsersActivity;
import com.soundcloud.android.onboarding.suggestions.SuggestedUsersCategoryActivity;
import com.soundcloud.android.onboarding.suggestions.SuggestedUsersSyncActivity;
import com.soundcloud.android.profile.MeActivity;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.search.SearchActivity;
import com.soundcloud.android.settings.AccountSettingsActivity;
import com.soundcloud.android.settings.ConfigurationFeaturesActivity;
import com.soundcloud.android.settings.LegalActivity;
import com.soundcloud.android.settings.NotificationSettingsActivity;
import com.soundcloud.android.settings.ScSettingsActivity;
import com.soundcloud.android.settings.SettingsActivity;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                MainActivity.class,
                TrackedActivity.class,
                MeActivity.class,
                RecordActivity.class,
                SuggestedUsersActivity.class,
                SuggestedUsersSyncActivity.class,
                UploadActivity.class,
                NavigationFragment.class,
                NavigationDrawerFragment.class,
                EmailOptInDialogFragment.class,
                ScSettingsActivity.class,
                SettingsActivity.class,
                AccountSettingsActivity.class,
                ConfigurationFeaturesActivity.class,
                NotificationSettingsActivity.class,
                LegalActivity.class,
                WhoToFollowActivity.class,
                SearchActivity.class,
                SuggestedUsersCategoryActivity.class,
                TrackCommentsActivity.class,
                ProfileActivity.class
        }, includes = {AssociationsModule.class, AnalyticsModule.class})
public class MainModule { }
