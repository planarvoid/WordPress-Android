package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdPlayerController;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;
import android.view.Menu;

import javax.inject.Inject;

public class NewProfileActivity extends ScActivity {

    public static final String EXTRA_USER_URN = "userUrn";

    @Inject @LightCycle SlidingPlayerController playerController;
    @Inject @LightCycle AdPlayerController adPlayerController;
    @Inject @LightCycle ProfilePresenter profilePresenter;

    @Override
    protected void setContentView() {
        super.setContentView(R.layout.new_profile);
        presenter.setToolBar();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_white, menu);
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
