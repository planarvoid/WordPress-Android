package com.soundcloud.android.view.screen;

import com.soundcloud.android.R;

import android.support.v4.view.WindowCompat;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class ScreenPresenter {

    private ActionBarActivity activity;

    @Inject
    ScreenPresenter() {
        // required for injection
    }

    public void attach(ActionBarActivity activity) {
        this.activity = activity;
    }

    public View setBaseLayout() {
        return createLayout(R.layout.base);
    }

    public View setBaseLayoutWithMargins() {
        return createLayout(R.layout.base_with_margins);
    }

    public View setBaseLayoutWithContent(int contentId) {
        View layout = setBaseLayout();
        addContent(contentId, layout);
        return layout;
    }

    public View setBaseDrawerLayout() {
        return createLayout(R.layout.base_with_drawer);
    }

    public View setBaseDrawerLayoutWithContent(int contentId) {
        View layout = setBaseDrawerLayout();
        addContent(contentId, layout);
        return layout;
    }

    private View createLayout(int baseLayoutId) {
        final View layout;
        activity.supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY);

        layout = activity.getLayoutInflater().inflate(baseLayoutId, null);
        activity.setContentView(layout);
        return layout;
    }

    private void addContent(int contentId, View layout) {
        ViewGroup container = (ViewGroup) layout.findViewById(R.id.container);
        View content = activity.getLayoutInflater().inflate(contentId, null);
        container.addView(content);
    }
}
