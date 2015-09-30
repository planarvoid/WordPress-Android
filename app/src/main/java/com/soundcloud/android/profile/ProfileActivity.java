package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.main.PlayerController;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;

import android.view.Menu;

import javax.inject.Inject;

public class ProfileActivity extends ScActivity {

    public static final String EXTRA_USER_URN = "userUrn";
    public static final String EXTRA_SEARCH_QUERY_SOURCE_INFO = "searchQuerySourceInfo";

    @Inject @LightCycle PlayerController playerController;
    @Inject @LightCycle ProfilePresenter profilePresenter;

    @Inject BaseLayoutHelper baseLayoutHelper;

    @Override
    protected void setActivityContentView() {
        super.setContentView(R.layout.new_profile);
        baseLayoutHelper.setToolBar(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_white, menu);
        return true;
    }
}
