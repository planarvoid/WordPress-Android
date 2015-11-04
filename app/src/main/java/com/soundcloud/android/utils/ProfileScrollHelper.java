package com.soundcloud.android.utils;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.profile.ProfileScreen;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.content.res.Resources;
import android.support.design.widget.AppBarLayout;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class ProfileScrollHelper extends DefaultActivityLightCycle<AppCompatActivity>
        implements AppBarLayout.OnOffsetChangedListener {

    private static final int TOP = 0;

    private final Resources resources;

    private AppBarLayout appBarLayout;
    private ViewPager viewPager;
    private Toolbar toolbar;
    private View header;

    private List<ProfileScreen> profileScreens = new ArrayList<>();
    private float elevationTarget;
    private int lastOffset;

    @Inject
    public ProfileScrollHelper(Resources resources) {
        this.resources = resources;
    }

    @Override
    public void onStart(AppCompatActivity activity) {
        appBarLayout = ButterKnife.findById(activity, R.id.appbar);
        viewPager = ButterKnife.findById(activity, R.id.pager);
        toolbar = ButterKnife.findById(activity, R.id.toolbar_id);
        header = ButterKnife.findById(activity, R.id.profile_header);
        setupElevation();

        checkNotNull(appBarLayout, "Expected to find AppBarLayout with ID R.id.appbar");

        appBarLayout.addOnOffsetChangedListener(this);
    }

    private void setupElevation() {
        ViewCompat.setElevation(toolbar, 0);
        elevationTarget = resources.getDimension(R.dimen.toolbar_elevation);
    }

    @Override
    public void onStop(AppCompatActivity activity) {
        appBarLayout.removeOnOffsetChangedListener(this);
        appBarLayout = null;
        profileScreens.clear();
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
        lastOffset = offset;
        ViewCompat.setTranslationZ(toolbar, calculateElevation(offset));
        for (ProfileScreen profileScreen : profileScreens) {
            setScreenOffset(profileScreen);
        }
    }

    private float calculateElevation(int offset) {
        float headerHeight = header.getHeight();
        return Math.min(elevationTarget, (headerHeight / 2) - Math.abs((headerHeight / 2) + offset));
    }

    private void setScreenOffset(ProfileScreen profileScreen) {
        profileScreen.setEmptyViewHeight(calculateListHeight());
        profileScreen.setSwipeToRefreshEnabled(lastOffset >= TOP);
    }

    private int calculateListHeight() {
        return viewPager.getHeight() - appBarLayout.getTotalScrollRange() - lastOffset;
    }

    public void addProfileCollection(ProfileScreen profileScreen) {
        profileScreens.add(profileScreen);
        setScreenOffset(profileScreen);
    }

    public void removeProfileScreen(ProfileScreen profileScreen) {
        profileScreens.remove(profileScreen);
    }
}
