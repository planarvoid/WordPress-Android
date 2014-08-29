package com.soundcloud.android.main;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.associations.AssociationsModule;
import com.soundcloud.android.associations.PlayableInteractionActivity;
import com.soundcloud.android.associations.PlaylistInteractionActivity;
import com.soundcloud.android.associations.TrackInteractionActivity;
import com.soundcloud.android.associations.WhoToFollowActivity;
import com.soundcloud.android.creators.record.RecordActivity;
import com.soundcloud.android.creators.upload.UploadActivity;
import com.soundcloud.android.onboarding.suggestions.SuggestedUsersActivity;
import com.soundcloud.android.onboarding.suggestions.SuggestedUsersCategoryActivity;
import com.soundcloud.android.onboarding.suggestions.SuggestedUsersSyncActivity;
import com.soundcloud.android.preferences.AccountSettingsActivity;
import com.soundcloud.android.preferences.NotificationSettingsActivity;
import com.soundcloud.android.preferences.ScSettingsActivity;
import com.soundcloud.android.preferences.SettingsActivity;
import com.soundcloud.android.profile.MeActivity;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.search.SearchActivity;
import dagger.Module;

@Module(addsTo = ApplicationModule.class,
        injects = {
                MainActivity.class,
                MeActivity.class,
                PlayableInteractionActivity.class,
                PlaylistInteractionActivity.class,
                RecordActivity.class,
                SuggestedUsersActivity.class,
                SuggestedUsersSyncActivity.class,
                TrackInteractionActivity.class,
                UploadActivity.class,
                NavigationFragment.class,
                NavigationDrawerFragment.class,
                EmailOptInDialogFragment.class,
                ScSettingsActivity.class,
                SettingsActivity.class,
                AccountSettingsActivity.class,
                NotificationSettingsActivity.class,
                WhoToFollowActivity.class,
                SearchActivity.class,
                SuggestedUsersCategoryActivity.class,
                ProfileActivity.class
        }, includes = AssociationsModule.class)
public class MainModule { }
