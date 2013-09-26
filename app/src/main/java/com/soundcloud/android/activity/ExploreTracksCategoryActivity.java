package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.fragment.ExploreTracksFragment;
import com.soundcloud.android.model.ExploreTracksCategory;

import android.os.Bundle;

public class ExploreTracksCategoryActivity extends ScActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ExploreTracksCategory category = getIntent().getParcelableExtra(ExploreTracksCategory.EXTRA);
        setTitle(category.getTitle());

        if (savedInstanceState == null) {
            ExploreTracksFragment exploreTracksFragment = new ExploreTracksFragment();
            exploreTracksFragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(android.R.id.content, exploreTracksFragment)
                    .commit();
        }
    }

    @Override
    protected int getSelectedMenuId() {
        return R.id.nav_discover;
    }
}
