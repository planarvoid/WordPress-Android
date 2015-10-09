package com.soundcloud.android.view.screen;

import com.soundcloud.android.R;
import com.soundcloud.android.properties.ApplicationProperties;

import android.support.v4.view.WindowCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class BaseLayoutHelper {

    private ApplicationProperties applicationProperties;

    @Inject
    BaseLayoutHelper(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public View setContainerLayout(AppCompatActivity activity) {
        return createActionBarLayout(activity, R.layout.container_layout);
    }

    public View setBaseLayout(AppCompatActivity activity) {
        return createActionBarLayout(activity, R.layout.base);
    }

    public View setBaseTabsLayout(AppCompatActivity activity) {
        return createLayout(activity, R.layout.base_with_tabs);
    }

    public View setBaseLayoutWithMargins(AppCompatActivity activity) {
        return createActionBarLayout(activity, R.layout.base_with_margins);
    }

    public View setBaseLayoutWithContent(AppCompatActivity activity, int contentId) {
        View layout = setBaseLayout(activity);
        addContent(activity, contentId, layout);
        return layout;
    }

    public View setBaseDrawerLayout(AppCompatActivity activity) {
        return createActionBarLayout(activity, R.layout.base_with_drawer);
    }

    public View setBaseDrawerLayoutWithContent(AppCompatActivity activity, int contentId) {
        View layout = setBaseDrawerLayout(activity);
        addContent(activity, contentId, layout);
        return layout;
    }

    private View createLayout(AppCompatActivity activity, int baseLayoutId) {
        activity.supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY);

        final View layout = activity.getLayoutInflater().inflate(baseLayoutId, null);
        activity.setContentView(layout);

        final DrawerLayout drawerLayout = (DrawerLayout) layout.findViewById(R.id.drawer_layout);
        if (drawerLayout != null && applicationProperties.isDebugBuild()) {
            View.inflate(layout.getContext(), R.layout.dev_drawer, drawerLayout);
        }
        return layout;
    }

    private View createActionBarLayout(AppCompatActivity activity, int baseLayoutId) {
        final View layout = createLayout(activity, baseLayoutId);
        setupActionBar(activity);
        return layout;
    }

    public void setupActionBar(AppCompatActivity activity) {
        final Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar_id);
        if (toolbar != null) {
            activity.setSupportActionBar(toolbar);

            final ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setDisplayShowTitleEnabled(true);
            }
        }
    }

    private void addContent(AppCompatActivity activity, int contentId, View layout) {
        ViewGroup container = (ViewGroup) layout.findViewById(R.id.container);
        View content = activity.getLayoutInflater().inflate(contentId, null);
        container.addView(content);
    }

}
