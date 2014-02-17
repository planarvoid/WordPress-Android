package com.soundcloud.android.search;

import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.actionbar.SearchActionBarController;
import com.soundcloud.android.main.ScActivity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;

public class CombinedSearchActivity extends ScActivity {

    private static final String STATE_QUERY = "query";

    private SearchActionBarController mActionBarController;

    private String mSavedQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.combined_search_activity);

        if (savedInstanceState == null) {
            replaceContent(new PlaylistTagsFragment(), PlaylistTagsFragment.TAG);
        } else {
            mSavedQuery = savedInstanceState.getString(STATE_QUERY);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (isConfigurationChange()) {
            mActionBarController.setQuery(mSavedQuery);
        } else {
            mActionBarController.requestSearchFieldFocus();
        }

        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_QUERY, mActionBarController.getQuery());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected ActionBarController createActionBarController() {
        mActionBarController = new SearchActionBarController(this, mPublicCloudAPI, searchCallback);
        return mActionBarController;
    }

    private void replaceContent(Fragment fragment, String tag) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment, tag)
                .commit();
    }

    @Override
    public int getMenuResourceId() {
        return R.menu.search;
    }

    private SearchActionBarController.SearchCallback searchCallback = new SearchActionBarController.SearchCallback() {
        @Override
        public void performSearch(String query) {
            replaceContent(SearchResultsFragment.newInstance(query), SearchResultsFragment.TAG);
        }

        @Override
        public void exitSearchMode() {
            replaceContent(new PlaylistTagsFragment(), PlaylistTagsFragment.TAG);
        }
    };

}
