package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.ScrollHelper;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

class ProfileScrollHelper
        extends DefaultActivityLightCycle<AppCompatActivity>
        implements ScrollHelper.ScrollScreen {

    private List<ProfileScreen> profileScreens = new ArrayList<>();
    private ScrollHelper scrollHelper;

    private AppBarLayout appBarLayout;
    private View headerView;
    private View contentView;
    private Toolbar toolbar;
    private float elevationTarget;

    @Inject
    public ProfileScrollHelper() { }

    @Override
    public void onCreate(final AppCompatActivity activity, Bundle bundle) {
        scrollHelper = new ScrollHelper(this);
        appBarLayout = (AppBarLayout) activity.findViewById(R.id.appbar);
        headerView = activity.findViewById(R.id.profile_header);
        contentView = activity.findViewById(R.id.pager);
        toolbar = (Toolbar) activity.findViewById(R.id.toolbar_id);
        elevationTarget = activity.getResources().getDimension(R.dimen.toolbar_elevation);
    }

    @Override
    public void onStart(final AppCompatActivity activity) {
        scrollHelper.attach();
    }

    @Override
    public void onStop(AppCompatActivity activity) {
        scrollHelper.detach();
        profileScreens.clear();
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        scrollHelper = null;
    }

    public void addProfileCollection(ProfileScreen profileScreen) {
        profileScreens.add(profileScreen);
    }

    public void removeProfileScreen(ProfileScreen profileScreen) {
        profileScreens.remove(profileScreen);
    }

    @Override
    public void setEmptyViewHeight(int height) {
        for (ProfileScreen profileScreen : profileScreens) {
            profileScreen.setEmptyViewHeight(height);
        }
    }

    @Override
    public void setSwipeToRefreshEnabled(boolean enabled) {
        for (ProfileScreen profileScreen : profileScreens) {
            profileScreen.setSwipeToRefreshEnabled(enabled);
        }
    }

    @Override
    public AppBarLayout getAppBarLayout() {
        return appBarLayout;
    }

    @Override
    public View getHeaderView() {
        return headerView;
    }

    @Override
    public View getContentView() {
        return contentView;
    }

    @Override
    public Toolbar getToolbar() {
        return toolbar;
    }

    @Override
    public float getElevationTarget() {
        return elevationTarget;
    }
}
