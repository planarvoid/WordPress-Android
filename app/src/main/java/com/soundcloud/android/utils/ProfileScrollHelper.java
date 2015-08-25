package com.soundcloud.android.utils;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.R;
import com.soundcloud.android.profile.ProfileScreen;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;

import android.support.design.widget.AppBarLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class ProfileScrollHelper extends DefaultActivityLightCycle<AppCompatActivity>
        implements AppBarLayout.OnOffsetChangedListener {

    private static final int TOP = 0;
    private AppBarLayout appBarLayout;
    private List<ProfileScreen> profileScreens = new ArrayList<>();
    private ViewPager viewPager;
    private int lastOffset;

    @Inject
    public ProfileScrollHelper() {
    }

    @Override
    public void onStart(AppCompatActivity activity) {
        super.onStart(activity);

        appBarLayout = (AppBarLayout) activity.findViewById(R.id.appbar);
        viewPager = (ViewPager) activity.findViewById(R.id.pager);

        checkNotNull(appBarLayout, "Expected to find AppBarLayout with ID R.id.appbar");

        appBarLayout.addOnOffsetChangedListener(this);
    }

    @Override
    public void onStop(AppCompatActivity activity) {
        appBarLayout.removeOnOffsetChangedListener(this);
        appBarLayout = null;
        profileScreens.clear();

        super.onStop(activity);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int offset) {
        lastOffset = offset;
        for (ProfileScreen profileScreen : profileScreens) {
            setScreenOffset(profileScreen);
        }
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
