package com.soundcloud.android.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarHelper;
import com.soundcloud.android.main.PlayerController;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.search.TabbedSearchFragment;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;

public class SearchResultsActivity extends ScActivity {

    public static final String EXTRA_SEARCH_QUERY = "searchQuery";

    @Inject @LightCycle PlayerController playerController;
    @Inject @LightCycle ActionBarHelper actionBarHelper;

    @Inject BaseLayoutHelper baseLayoutHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            createFragmentForSearchResults();
        }
    }

    private void createFragmentForSearchResults() {
        final String searchQuery = getIntent().getStringExtra(EXTRA_SEARCH_QUERY);
        if (Strings.isNullOrEmpty(searchQuery)) {
            throw new IllegalStateException("Invalid search query");
        } else {
            Fragment fragment = TabbedSearchFragment.newInstance(searchQuery);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment, TabbedSearchFragment.TAG)
                    .commit();
        }
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setBaseLayout(this);
    }

    @Override
    public void onBackPressed() {
        if (!playerController.handleBackPressed()) {
            super.onBackPressed();
        }
    }

}
