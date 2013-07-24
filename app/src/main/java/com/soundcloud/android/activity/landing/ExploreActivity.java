package com.soundcloud.android.activity.landing;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.fragment.SuggestedTracksFragment;

import android.os.Bundle;

public class ExploreActivity extends ScActivity implements ScLandingPage
{
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.frame_layout_holder);
        if (state == null) {
            final SuggestedTracksFragment suggestedTracksFragment = new SuggestedTracksFragment();
            suggestedTracksFragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.holder, suggestedTracksFragment)
                    .commit();
        }
    }

    @Override
    protected int getSelectedMenuId() {
        return R.id.nav_explore;
    }
}
