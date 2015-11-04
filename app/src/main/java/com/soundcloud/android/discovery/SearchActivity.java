package com.soundcloud.android.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.main.PlayerController;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;

import android.graphics.drawable.ColorDrawable;
import android.support.v4.content.ContextCompat;

import javax.inject.Inject;

public class SearchActivity extends ScActivity {

    @Inject BaseLayoutHelper layoutHelper;

    @Inject @LightCycle SearchPresenter presenter;
    @Inject @LightCycle PlayerController playerController;

    @Override
    protected void setActivityContentView() {
        layoutHelper.createActionBarLayout(this, R.layout.search);
        getWindow().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.search_screen_background)));
    }

    @Override
    public void onBackPressed() {
        if (!playerController.handleBackPressed()) {
            super.onBackPressed();
        }
    }
}
