package com.soundcloud.android.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.main.PlayerController;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;

import javax.inject.Inject;

public class NewSearchActivity extends ScActivity {

    @Inject BaseLayoutHelper layoutHelper;

    @Inject @LightCycle PlayerController playerController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            createFragmentForSearch();
        }
    }

    private void createFragmentForSearch() {
        getSupportFragmentManager().beginTransaction().add(R.id.container, new SearchFragment()).commit();
    }

    @Override
    protected void setActivityContentView() {
        layoutHelper.setBaseLayout(this);
        getWindow().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(this, R.color.search_screen_background)));
    }
}
