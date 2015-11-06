package com.soundcloud.android;

import com.soundcloud.android.discovery.SearchActivity;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.utils.TransitionUtils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.SharedElementCallback;
import android.content.Intent;
import android.transition.Transition;
import android.util.Pair;
import android.view.View;

import java.util.List;

@TargetApi(19)
@SuppressLint("NewApi")
public class SmoothNavigator extends Navigator {

    public SmoothNavigator(FeatureFlags featureFlags) {
        super(featureFlags);
    }

    @Override
    public void openSearch(Activity activity) {
        Transition autoTransition = TransitionUtils.createAutoTransition(activity);
        activity.getWindow().setSharedElementExitTransition(autoTransition);
        activity.getWindow().setSharedElementReturnTransition(autoTransition);
        activity.getWindow().setAllowReturnTransitionOverlap(true);

        final View searchIcon = activity.findViewById(R.id.search_icon);
        final View searchItem = activity.findViewById(R.id.search_item);
        final int mediumAnimTime = activity.getResources().getInteger(
                android.R.integer.config_mediumAnimTime);
        searchIcon.animate().alpha(0).setDuration(mediumAnimTime).start();
        searchItem.animate().alpha(0).setDuration(mediumAnimTime).start();

        activity.setExitSharedElementCallback(new SharedElementCallback() {
            @Override
            public void onSharedElementEnd(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {
                searchIcon.animate().alpha(1).setDuration(mediumAnimTime).start();
                searchItem.setAlpha(1);
            }
        });

        Pair[] sharedElements = new Pair[]{
                Pair.create(activity.findViewById(R.id.search_text), activity.getString(R.string.search_text_transition_name)),
                Pair.create(activity.findViewById(R.id.search_holder), activity.getString(R.string.search_text_holder_transition_name))
        };
        ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(activity, sharedElements);
        activity.startActivity(new Intent(activity, SearchActivity.class), options.toBundle());
    }
}
