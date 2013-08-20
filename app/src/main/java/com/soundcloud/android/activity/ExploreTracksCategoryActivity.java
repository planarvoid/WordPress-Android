package com.soundcloud.android.activity;

import com.soundcloud.android.R;
import com.soundcloud.android.fragment.ExploreTracksFragment;

import android.os.Bundle;

public class ExploreTracksCategoryActivity extends ScActivity {

    private ExploreTracksFragment mCategoryFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.frame_layout_holder);
        if (savedInstanceState == null) {
            mCategoryFragment = new ExploreTracksFragment();
            mCategoryFragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.holder, mCategoryFragment)
                    .commit();
        } else {
            mCategoryFragment = (ExploreTracksFragment) getSupportFragmentManager().findFragmentById(R.id.holder);
        }
    }

    @Override
    protected int getSelectedMenuId() {
        return R.id.nav_explore;
    }
}
