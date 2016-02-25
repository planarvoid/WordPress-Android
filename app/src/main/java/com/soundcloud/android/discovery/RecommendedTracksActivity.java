package com.soundcloud.android.discovery;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.view.screen.BaseLayoutHelper;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;

public class RecommendedTracksActivity extends PlayerActivity {

    public static final String EXTRA_LOCAL_SEED_ID = "localSeedId";

    @Inject BaseLayoutHelper baseLayoutHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.activity_title_recommendations);

        if (savedInstanceState == null) {
            createFragmentForRecommendations();
        }
    }

    private void createFragmentForRecommendations() {
        final long localSeedId = getIntent().getLongExtra(EXTRA_LOCAL_SEED_ID, Consts.NOT_SET);
        if (localSeedId > 0) {
            Fragment fragment = RecommendedTracksFragment.create(localSeedId);
            getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
        } else {
            throw new IllegalStateException("Invalid recommendation local seed id");
        }
    }

    @Override
    public Screen getScreen() {
        return Screen.SEARCH_RECOMMENDED_TRACKS;
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setBaseLayout(this);
    }

}
