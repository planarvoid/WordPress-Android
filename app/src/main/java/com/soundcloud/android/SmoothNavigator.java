package com.soundcloud.android;

import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.discovery.SearchActivity;
import com.soundcloud.android.properties.FeatureFlags;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.SharedElementCallback;
import android.content.Intent;
import android.util.Pair;
import android.view.View;

import java.util.List;

@TargetApi(21)
@SuppressLint("NewApi")
class SmoothNavigator extends Navigator {

    SmoothNavigator(EventTracker eventTracker, FeatureFlags featureFlags) {
        super(eventTracker, featureFlags);
    }

    @Override
    public void openSearch(Activity activity) {
        final View searchIcon = activity.findViewById(R.id.search_icon);
        final View searchItem = activity.findViewById(R.id.search_item);

        // No guarantees the views are available here.
        // https://github.com/soundcloud/SoundCloud-Android/issues/4633
        if (searchItem != null && searchIcon != null) {
            final int mediumAnimTime = activity.getResources().getInteger(android.R.integer.config_mediumAnimTime);
            searchIcon.animate().alpha(0).setDuration(mediumAnimTime).start();
            searchItem.animate().alpha(0).setDuration(mediumAnimTime).start();

            activity.setExitSharedElementCallback(new SharedElementCallback() {
                @Override
                public void onSharedElementEnd(List<String> sharedElementNames,
                                               List<View> sharedElements,
                                               List<View> sharedElementSnapshots) {
                    searchIcon.animate().alpha(1).setDuration(mediumAnimTime).start();
                    searchItem.setAlpha(1);
                }
            });

            Pair[] sharedElements = new Pair[]{
                    Pair.create(activity.findViewById(R.id.search_text),
                                activity.getString(R.string.search_text_transition_name)),
                    Pair.create(activity.findViewById(R.id.search_holder),
                                activity.getString(R.string.search_text_holder_transition_name))
            };
            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(activity, sharedElements);
            activity.startActivity(new Intent(activity, SearchActivity.class), options.toBundle());
        } else {
            // Fallback to parent class and open the activity without animation.
            super.openSearch(activity);
        }
    }

}
