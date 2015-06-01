package com.soundcloud.android;

import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

public class Navigator {

    private static final int NO_FLAGS = 0;

    private final FeatureFlags featureFlags;

    @Inject
    public Navigator(FeatureFlags featureFlags) {
        this.featureFlags = featureFlags;
    }

    public void openProfile(Context activityContext, Urn user) {
        activityContext.startActivity(createProfileIntent(activityContext, user));
    }

    public void openProfile(Context activityContext, Urn user, SearchQuerySourceInfo searchQuerySourceInfo) {
        activityContext.startActivity(createProfileIntent(activityContext, user)
                .putExtra(ProfileActivity.EXTRA_QUERY_SOURCE_INFO, searchQuerySourceInfo));
    }

    public PendingIntent openProfileFromNotification(Context context, Urn user) {
        return PendingIntent.getActivity(context,
                NO_FLAGS,
                createProfileIntent(context, user)
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NEW_TASK),
                NO_FLAGS);
    }

    public PendingIntent openProfileFromWidget(Context context, Urn user, int requestCode) {
        return PendingIntent.getActivity(context,
                requestCode,
                createProfileIntent(context, user),
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private Intent createProfileIntent(Context context, Urn user) {
        // TODO: replace with new profile activity from branch
        return new Intent(context, featureFlags.isEnabled(Flag.NEW_PROFILE) ? ProfileActivity.class : ProfileActivity.class)
                .putExtra(ProfileActivity.EXTRA_USER_URN, user);
    }

}
