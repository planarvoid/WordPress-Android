package com.soundcloud.android.navigation;

import com.soundcloud.android.R;
import com.soundcloud.android.olddiscovery.SearchActivity;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.SharedElementCallback;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.util.Pair;
import android.view.View;

import java.util.List;

/**
 * {@inheritDoc}
 */
@TargetApi(21)
@SuppressLint("NewApi")
@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
public class SmoothNavigationExecutor extends NavigationExecutor {

    @Override
    public void openSearch(Activity activity) {
        final View searchIcon = activity.findViewById(R.id.search_icon);
        final View searchItem = activity.findViewById(R.id.search_item);

        // No guarantees the views are available here.
        // https://github.com/soundcloud/android-listeners/issues/4633
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
