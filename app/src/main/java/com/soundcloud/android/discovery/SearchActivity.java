package com.soundcloud.android.discovery;

import android.view.MenuItem;
import com.soundcloud.android.R;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;
import javax.inject.Inject;

import static com.soundcloud.android.search.suggestions.SearchSuggestionsPresenter.SearchListener;

public class SearchActivity extends PlayerActivity implements SearchListener {

    @Inject @LightCycle FeaturedSearchPresenter presenter;

    @Inject BaseLayoutHelper layoutHelper;
    @Inject FeatureFlags featureFlags;

    @Override
    public Screen getScreen() {
        // The Activity is not a screen. Fragments are screens.
        return Screen.UNKNOWN;
    }

    @Override
    protected void setActivityContentView() {
        final int search_layout =
                featureFlags.isEnabled(Flag.NEW_SEARCH_SUGGESTIONS) ? R.layout.search : R.layout.search_legacy;
        layoutHelper.createActionBarLayout(this, search_layout);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            presenter.dismiss(this);
            return true;
        }
        return false;
    }

    @Override
    public void onSearchClicked(String searchQuery) {
        presenter.performSearch(searchQuery);
    }
}
