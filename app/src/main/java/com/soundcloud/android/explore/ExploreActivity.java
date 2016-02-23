package com.soundcloud.android.explore;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarHelper;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;

import javax.inject.Inject;

public class ExploreActivity extends PlayerActivity {

    @Inject @LightCycle ActionBarHelper actionBarHelper;
    @Inject @LightCycle ExplorePresenter explorePresenter;

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject Navigator navigator;

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.createActionBarLayout(this, R.layout.explore);
    }

    @Override
    public Screen getScreen() {
        return Screen.EXPLORE_TRENDING_MUSIC;
    }

    @Override
    public boolean onSupportNavigateUp() {
        navigator.openHome(this);
        finish();
        return true;
    }

}
