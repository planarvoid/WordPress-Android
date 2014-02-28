package com.soundcloud.android.search;

import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.actionbar.SearchActionBarController;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.ScTextUtils;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Menu;

public class CombinedSearchActivity extends ScActivity implements PlaylistTagsFragment.TagClickListener {

    private static final String ACTION_PLAY_FROM_SEARCH = "android.media.action.MEDIA_PLAY_FROM_SEARCH";
    private static final String INTENT_URL_HOST = "soundcloud.com";
    private static final String INTENT_URL_QUERY_PARAM = "q";
    private static final String INTENT_URI_SEARCH_PATH = "/search";

    private static final String STATE_QUERY = "query";

    private SearchActionBarController mActionBarController;
    private String mQuery;

    private final SearchActionBarController.SearchCallback mSearchCallback = new SearchActionBarController.SearchCallback() {
        @Override
        public void performTextSearch(String query) {
            addContent(TabbedSearchFragment.newInstance(query), TabbedSearchFragment.TAG);
        }

        @Override
        public void performTagSearch(String tag) {
            addContent(PlaylistResultsFragment.newInstance(tag), PlaylistResultsFragment.TAG);
        }

        @Override
        public void exitSearchMode() {
            getSupportFragmentManager().popBackStack();
        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        mActionBarController.setQuery("");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.container_layout);

        if (savedInstanceState == null) {
            handleIntent();
        } else {
            mQuery = savedInstanceState.getString(STATE_QUERY);
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
        if (ScTextUtils.isNotBlank(mQuery)) {
            mActionBarController.setQuery(mQuery);
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
        mActionBarController = new SearchActionBarController(this, mPublicCloudAPI, mSearchCallback);
        return mActionBarController;
    }

    private void addPlaylistTagsFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.holder, new PlaylistTagsFragment(), PlaylistTagsFragment.TAG)
                .commit();
    }

    private void addContent(Fragment fragment, String tag) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.holder, fragment, tag)
                .addToBackStack(tag)
                .commit();
    }

    @Override
    public int getMenuResourceId() {
        return R.menu.search;
    }

    @Override
    public void onTagSelected(String tag) {
        mActionBarController.setQuery(tag);
        addContent(PlaylistResultsFragment.newInstance(tag), PlaylistResultsFragment.TAG);
    }

    private void handleIntent() {
        addPlaylistTagsFragment();
        final Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction()) || ACTION_PLAY_FROM_SEARCH.equals(intent.getAction())) {
            showResultsFromIntent(intent.getStringExtra(SearchManager.QUERY));
        } else if (isInterceptedSearchUrl(intent)) {
            handleDeeplink(intent);
        } else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null
                && !intent.getData().getPath().equals(INTENT_URI_SEARCH_PATH)) {
            handleUri(intent);
        }
    }

    private boolean isInterceptedSearchUrl(Intent intent) {
        return intent.getData() != null
                && intent.getData().getHost().equals(INTENT_URL_HOST)
                && ScTextUtils.isNotBlank(intent.getData().getQueryParameter(INTENT_URL_QUERY_PARAM));
    }

    private void handleDeeplink(final Intent intent) {
        showResultsFromIntent(intent.getData().getQueryParameter(INTENT_URL_QUERY_PARAM));
    }

    private void handleUri(final Intent intent) {
        final Content content = Content.match(intent.getData());
        if (content == Content.SEARCH_ITEM) {
            showResultsFromIntent(Uri.decode(intent.getData().getLastPathSegment()));
        } else if (content != Content.UNKNOWN) {
            // Quick search box - Resolve through normal system
            startActivity(new Intent(Intent.ACTION_VIEW).setData(intent.getData()));
            finish();
        }
    }

    private void showResultsFromIntent(String query) {
        mQuery = query;
        addContent(TabbedSearchFragment.newInstance(query), TabbedSearchFragment.TAG);
    }

}
