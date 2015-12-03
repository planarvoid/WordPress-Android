package com.soundcloud.android.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.main.PlayerController;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;

import android.view.MenuItem;

import javax.inject.Inject;

public class SearchActivity extends ScActivity {

    @Inject @LightCycle SearchPresenter presenter;
    @Inject @LightCycle PlayerController playerController;

    @Inject BaseLayoutHelper layoutHelper;

    @Override
    protected void setActivityContentView() {
        layoutHelper.createActionBarLayout(this, R.layout.search);
    }

    @Override
    public void onBackPressed() {
        if (!playerController.handleBackPressed()) {
            presenter.dismiss(this);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            presenter.dismiss(this);
            return true;
        }
        return false;
    }
}
