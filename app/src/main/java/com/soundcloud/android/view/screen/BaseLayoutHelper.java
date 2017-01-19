package com.soundcloud.android.view.screen;

import com.soundcloud.android.R;
import com.soundcloud.android.playlists.PlaylistDetailActivity;

import android.support.v4.view.WindowCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class BaseLayoutHelper {

    @Inject
    BaseLayoutHelper() {

    }

    public View setContainerLayout(AppCompatActivity activity) {
        return createActionBarLayout(activity, R.layout.container_layout);
    }

    public View setContainerLoadingLayout(AppCompatActivity activity) {
        return createActionBarLayout(activity, R.layout.container_loading_layout);
    }

    public View setBaseLayout(AppCompatActivity activity) {
        return createActionBarLayout(activity, R.layout.base);
    }

    public View setBaseNoToolbar(AppCompatActivity activity) {
        return createActionBarLayout(activity, R.layout.base_no_toolbar);
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

    private View createLayout(AppCompatActivity activity, int baseLayoutId) {
        activity.supportRequestWindowFeature(WindowCompat.FEATURE_ACTION_BAR_OVERLAY);
        final View layout = activity.getLayoutInflater().inflate(baseLayoutId, null);
        activity.setContentView(layout);
        return layout;
    }

    public static void addDevelopmentDrawer(AppCompatActivity activity) {
        final DrawerLayout drawerLayout = (DrawerLayout) activity.findViewById(R.id.drawer_layout);
        final View devDrawer = activity.findViewById(R.id.dev_drawer);
        if (drawerLayout != null && devDrawer == null) {
            View.inflate(activity, R.layout.dev_drawer, drawerLayout);
        }
    }

    public static void removeDevelopmentDrawer(AppCompatActivity activity) {
        final View devDrawer = activity.findViewById(R.id.dev_drawer);
        if (devDrawer != null) {
            ViewGroup viewGroup = (ViewGroup) devDrawer.getParent();
            viewGroup.removeView(devDrawer);
        }
    }

    public View createActionBarLayout(AppCompatActivity activity, int baseLayoutId) {
        final View layout = createLayout(activity, baseLayoutId);
        setupActionBar(activity);
        return layout;
    }

    public void setupActionBar(final AppCompatActivity activity) {
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
