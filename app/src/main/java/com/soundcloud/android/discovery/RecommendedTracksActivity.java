package com.soundcloud.android.discovery;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.main.PlayerController;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;

public class RecommendedTracksActivity extends ScActivity {

    public static final String EXTRA_LOCAL_SEED_ID = "localSeedId";

    @Inject @LightCycle PlayerController playerController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.activity_title_recommendations);

        if (savedInstanceState == null) {
            createFragmentForRecommendations();
        }
    }

    private void createFragmentForRecommendations() {
        long localSeedId = getIntent().getLongExtra(EXTRA_LOCAL_SEED_ID, Consts.NOT_SET);
        if (localSeedId > 0) {
            Fragment fragment = RecommendedTracksFragment.create(localSeedId);
            getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
        } else {
            throw new IllegalStateException("Invalid recommendation local seed id");
        }

    }

    @Override
    protected void setContentView() {
        presenter.setBaseLayout();
    }
}
