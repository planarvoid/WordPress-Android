package com.soundcloud.android.discovery;

import static com.soundcloud.android.search.suggestions.SearchSuggestionsPresenter.SuggestionListener;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.LightCycle;

import android.view.MenuItem;

import javax.inject.Inject;

public class SearchActivity extends PlayerActivity implements SuggestionListener {

    @Inject @LightCycle SearchPresenter presenter;

    @Inject BaseLayoutHelper layoutHelper;

    public SearchActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public Screen getScreen() {
        // The Activity is not a screen. Fragments are screens.
        return Screen.UNKNOWN;
    }

    @Override
    protected void setActivityContentView() {
        layoutHelper.createActionBarLayout(this, R.layout.search);
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
    public void onScrollChanged() {
        presenter.onScrollChanged();
    }

    @Override
    public void onSearchClicked(String apiQuery, String userQuery) {
        presenter.performSearch(apiQuery, userQuery);
    }

    @Override
    public void onSuggestionClicked() {
        presenter.onSuggestionClicked();
    }

    @Override
    public void onAutocompleteClicked(String searchQuery, String userQuery, String output, Optional<Urn> queryUrn, int position) {
        presenter.performSearch(searchQuery, userQuery, Optional.of(output), queryUrn, Optional.of(position));
    }
}
