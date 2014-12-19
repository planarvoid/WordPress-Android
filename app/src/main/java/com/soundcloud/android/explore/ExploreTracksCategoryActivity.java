package com.soundcloud.android.explore;

import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdPlayerController;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.view.screen.ScreenPresenter;

import android.os.Bundle;

import javax.inject.Inject;

public class ExploreTracksCategoryActivity extends ScActivity {

    @Inject SlidingPlayerController playerController;
    @Inject AdPlayerController adPlayerController;
    @Inject ScreenPresenter presenter;

    public ExploreTracksCategoryActivity() {
        lightCycleDispatcher.add(playerController);
        lightCycleDispatcher.add(adPlayerController);
        presenter.attach(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final ExploreGenre category = getIntent().getParcelableExtra(ExploreGenre.EXPLORE_GENRE_EXTRA);
        setTitle(category.getTitle());

        if (savedInstanceState == null) {
            ExploreTracksFragment exploreTracksFragment = new ExploreTracksFragment();
            exploreTracksFragment.setRetainInstance(true);
            exploreTracksFragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, exploreTracksFragment)
                    .commit();
        }
    }

    @Override
    protected void setContentView() {
        presenter.setBaseLayout();
    }

    @Override
    public void onBackPressed() {
        if (!playerController.handleBackPressed()) {
            super.onBackPressed();
        }
    }
}
