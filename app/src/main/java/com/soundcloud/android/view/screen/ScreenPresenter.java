package com.soundcloud.android.view.screen;

import com.soundcloud.android.R;
import com.soundcloud.android.properties.Feature;
import com.soundcloud.android.properties.FeatureFlags;

import android.support.v4.view.WindowCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class ScreenPresenter {

    private final FeatureFlags featureFlags;

    private ActionBarActivity activity;

    @Inject
    ScreenPresenter(FeatureFlags featureFlags) {
        this.featureFlags = featureFlags;
    }

    public void attach(ActionBarActivity activity) {
        this.activity = activity;
    }

    public View setBaseLayout() {
        return createLayout(getLayoutId());
    }

    public View setBaseLayoutWithContent(int contentId) {
        View layout = setBaseLayout();
        addContent(contentId, layout);
        return layout;
    }

    public View setBaseDrawerLayout() {
        return createLayout(getLayoutIdWithDrawer());
    }

    public View setBaseDrawerLayoutWithContent(int contentId) {
        View layout = setBaseDrawerLayout();
        addContent(contentId, layout);
        return layout;
    }

    private View createLayout(int baseLayoutId) {
        final View layout;
        if (featureFlags.isEnabled(Feature.VISUAL_PLAYER)) {
            activity.supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY);
        }

        layout = activity.getLayoutInflater().inflate(baseLayoutId, null);
        activity.setContentView(layout);
        return layout;
    }

    private void addContent(int contentId, View layout) {
        ViewGroup container = (ViewGroup) layout.findViewById(R.id.container);
        View content = activity.getLayoutInflater().inflate(contentId, null);
        container.addView(content);
    }

    private int getLayoutId() {
        if (featureFlags.isEnabled(Feature.VISUAL_PLAYER)) {
            return R.layout.base;
        } else {
            return R.layout.base_legacy;
        }
    }

    private int getLayoutIdWithDrawer() {
        if (featureFlags.isEnabled(Feature.VISUAL_PLAYER)) {
            return R.layout.base_with_drawer;
        } else {
            return R.layout.base_with_drawer_legacy;
        }
    }

}
