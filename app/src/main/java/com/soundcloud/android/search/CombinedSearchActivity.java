package com.soundcloud.android.search;

import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.actionbar.SearchActionBarController;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.storage.provider.Content;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;

public class CombinedSearchActivity extends ScActivity {

    private static final String PLAY_FROM_SEARCH_ACTION = "android.media.action.MEDIA_PLAY_FROM_SEARCH";

    private static final String STATE_QUERY = "query";

    private SearchActionBarController mActionBarController;

    private String mSavedQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.container_layout);

        if (savedInstanceState == null) {
            handleIntent();
            replaceContent(new PlaylistTagsFragment(), PlaylistTagsFragment.TAG);
        } else {
            mSavedQuery = savedInstanceState.getString(STATE_QUERY);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent();
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
                .replace(R.id.holder, fragment, tag)
                .commit();
    }

    @Override
    public int getMenuResourceId() {
        return R.menu.search;
    }

    private SearchActionBarController.SearchCallback searchCallback = new SearchActionBarController.SearchCallback() {
        @Override
        public void performSearch(String query) {
            replaceContent(TabbedSearchFragment.newInstance(query), TabbedSearchFragment.TAG);
        }

        @Override
        public void exitSearchMode() {
            replaceContent(new PlaylistTagsFragment(), PlaylistTagsFragment.TAG);
        }
    };

    private void handleIntent() {
        final Intent intent = getIntent();
        if (intent.getAction().equals(Intent.ACTION_SEARCH) || intent.getAction().equals(PLAY_FROM_SEARCH_ACTION)) {
            handleSearchActions(intent);
        } else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null
                && !intent.getData().getPath().equals("/search")) {
            handleSearchDeeplink(intent);
        }
    }

    private void handleSearchActions(final Intent intent) {
        String query = intent.getStringExtra(SearchManager.QUERY);
        replaceContent(TabbedSearchFragment.newInstance(query), TabbedSearchFragment.TAG);
    }

    private void handleSearchDeeplink(final Intent intent) {
        final Content content = Content.match(intent.getData());
        if (content == Content.SEARCH_ITEM) {
            final String query = Uri.decode(intent.getData().getLastPathSegment());
            replaceContent(TabbedSearchFragment.newInstance(query), TabbedSearchFragment.TAG);
        } else if (content != Content.UNKNOWN) {
            // Quick search box - Resolve through normal system
            startActivity(new Intent(Intent.ACTION_VIEW).setData(intent.getData()));
        }
    }

}
