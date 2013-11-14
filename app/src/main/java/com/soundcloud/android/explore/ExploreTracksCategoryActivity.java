package com.soundcloud.android.explore;

import com.soundcloud.android.dagger.DaggerHelper;
import com.soundcloud.android.dagger.ObjectGraphProvider;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.model.ExploreTracksCategory;
import dagger.ObjectGraph;

import android.os.Bundle;

public class ExploreTracksCategoryActivity extends ScActivity implements ObjectGraphProvider {

    private ObjectGraph mObjectGraph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mObjectGraph = DaggerHelper.fromApplicationGraph(this, new ExploreModule());

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
    public ObjectGraph getObjectGraph() {
        return mObjectGraph;
    }

    @Override
    protected void setContentView() {
        // nop, don't allow margins to be set here
    }

    @Override
    protected void onDestroy() {
        mObjectGraph = null;
        super.onDestroy();
    }
}
