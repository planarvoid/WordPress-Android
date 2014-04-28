package com.soundcloud.android.search;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.actionbar.SearchActionBarController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.PublicApi;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.ScTextUtils;

import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.Menu;

public class CombinedSearchActivity extends ScActivity implements PlaylistTagsFragment.TagEventsListener {

    private static final String ACTION_PLAY_FROM_SEARCH = "android.media.action.MEDIA_PLAY_FROM_SEARCH";
    private static final String INTENT_URL_HOST = "soundcloud.com";
    private static final String INTENT_URL_QUERY_PARAM = "q";
    private static final String INTENT_URI_SEARCH_PATH = "/search";

    private static final String STATE_QUERY = "query";

    private SearchActionBarController searchActionBarController;
    private String query;

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
            eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.SEARCH_MAIN.get());
            getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    };

    @SuppressWarnings("unused")
    public CombinedSearchActivity() {
    }

    @VisibleForTesting
    CombinedSearchActivity(SearchActionBarController searchActionBarController) {
        this.searchActionBarController = searchActionBarController;
    }

    @Override
    public void onBackPressed() {
        boolean isShowingResults = getSupportFragmentManager().getBackStackEntryCount() > 0;
        if (isShowingResults) {
            searchActionBarController.clearQuery();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.container_layout);

        if (savedInstanceState == null) {
            handleIntent();
        } else if (savedInstanceState.containsKey(STATE_QUERY)) {
            query = savedInstanceState.getString(STATE_QUERY);
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
        if (ScTextUtils.isNotBlank(query)) {
            searchActionBarController.setQuery(query);
        }
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(STATE_QUERY, searchActionBarController.getQuery());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected ActionBarController createActionBarController() {
        if (searchActionBarController == null) {
            searchActionBarController = new SearchActionBarController(this, new PublicApi(this), mSearchCallback, eventBus);
        }
        return searchActionBarController;
    }

    private void addPlaylistTagsFragment() {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.holder, new PlaylistTagsFragment(), PlaylistTagsFragment.TAG)
                .commit();
    }

    private void addContent(Fragment fragment, String tag) {
        FragmentManager manager = getSupportFragmentManager();

        // Clear the backstack before adding new content Fragment
        if (manager.getBackStackEntryCount() > 0) {
            manager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }

        manager.beginTransaction()
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
        searchActionBarController.setQuery("#" + tag);
        addContent(PlaylistResultsFragment.newInstance(tag), PlaylistResultsFragment.TAG);
    }

    @Override
    public void onTagsScrolled() {
        searchActionBarController.clearFocus();
    }

    private void handleIntent() {
        addPlaylistTagsFragment();
        final Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction()) || ACTION_PLAY_FROM_SEARCH.equals(intent.getAction())) {
            showResultsFromIntent(intent.getStringExtra(SearchManager.QUERY));
        } else if (isInterceptedSearchUrl(intent)) {
            showResultsFromIntent(intent.getData().getQueryParameter(INTENT_URL_QUERY_PARAM));
        } else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null
                && !intent.getData().getPath().equals(INTENT_URI_SEARCH_PATH)) {
            handleUri(intent);
        } else {
            eventBus.publish(EventQueue.SCREEN_ENTERED, Screen.SEARCH_MAIN.get());
        }
    }

    private boolean isInterceptedSearchUrl(Intent intent) {
        return intent.getData() != null
                && intent.getData().getHost().equals(INTENT_URL_HOST)
                && ScTextUtils.isNotBlank(intent.getData().getQueryParameter(INTENT_URL_QUERY_PARAM));
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
        this.query = query;
        addContent(TabbedSearchFragment.newInstance(query), TabbedSearchFragment.TAG);
    }

}
