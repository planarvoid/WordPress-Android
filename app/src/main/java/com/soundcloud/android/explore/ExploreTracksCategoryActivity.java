package com.soundcloud.android.explore;

import com.soundcloud.android.main.ScActivity;
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
}
