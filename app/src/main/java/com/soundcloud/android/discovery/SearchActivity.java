package com.soundcloud.android.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;

import android.view.MenuItem;

import javax.inject.Inject;

public class SearchActivity extends PlayerActivity {

    @Inject @LightCycle SearchPresenter presenter;

    @Inject BaseLayoutHelper layoutHelper;

    @Override
    public Screen getScreen() {
        // The Activity is not a screen. Fragments are screens.
        return Screen.UNKNOWN;
    }

    @Override
    protected void setActivityContentView() {
        layoutHelper.createActionBarLayout(this, R.layout.search);
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
