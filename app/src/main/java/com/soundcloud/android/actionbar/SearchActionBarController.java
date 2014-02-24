package com.soundcloud.android.actionbar;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.search.suggestions.SuggestionsAdapter;
import com.soundcloud.android.utils.Log;
import org.jetbrains.annotations.NotNull;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.internal.view.SupportMenuItem;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.widget.ImageView;

import java.lang.reflect.Field;

public class SearchActionBarController extends ActionBarController {

    private SuggestionsAdapter mSuggestionsAdapter;

    private SearchView mSearchView;

    private final PublicCloudAPI mPublicCloudAPI;
    private final SearchCallback mSearchCallback;

    public interface SearchCallback {
        void performTextSearch(String query);
        void performTagSearch(String tag);
        void exitSearchMode();
    }

    public SearchActionBarController(@NotNull ActionBarOwner owner, PublicCloudAPI publicCloudAPI,
                                     SearchCallback searchCallback) {
        super(owner);
        mPublicCloudAPI = publicCloudAPI;
        mSearchCallback = searchCallback;
    }

    @Override
    public void onDestroy() {
        // suggestions adapter has to stop handler thread
        if (mSuggestionsAdapter != null) mSuggestionsAdapter.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu) {
        ActionBar actionBar = mOwner.getActivity().getSupportActionBar();
        configureSearchState(menu, actionBar);
    }

    private void configureSearchState(Menu menu, ActionBar actionBar) {
        actionBar.setDisplayShowCustomEnabled(false);
        mOwner.getActivity().getMenuInflater().inflate(R.menu.search, menu);

        initSearchView(menu);
        mSearchView.setOnQueryTextListener(mQueryTextListener);
        mSearchView.setOnSuggestionListener(mSuggestionListener);
        styleSearchView(mSearchView);
    }

    private void initSearchView(Menu menu) {
        SearchManager searchManager = (SearchManager) mActivity.getSystemService(Context.SEARCH_SERVICE);
        final SearchableInfo searchableInfo = searchManager.getSearchableInfo(mActivity.getComponentName());

        SupportMenuItem searchItem = (SupportMenuItem) menu.findItem(R.id.action_search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        mSearchView.setSearchableInfo(searchableInfo);
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setQueryHint(mOwner.getActivity().getString(R.string.search_hint));

        mSuggestionsAdapter = new SuggestionsAdapter(mActivity, mPublicCloudAPI);
        mSearchView.setSuggestionsAdapter(mSuggestionsAdapter);
    }

    private final SearchView.OnQueryTextListener mQueryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            performSearch(query);
            // clear focus as a workaround for https://code.google.com/p/android/issues/detail?id=24599
            mSearchView.clearFocus();
            return true;
        }

        @Override
        public boolean onQueryTextChange(String s) {
            if (s.length() < 1) {
                mSearchCallback.exitSearchMode();
            }
            return false;
        }
    };

    private final SearchView.OnSuggestionListener mSuggestionListener = new SearchView.OnSuggestionListener() {
        @Override
        public boolean onSuggestionSelect(int position) {
            return false;
        }

        @Override
        public boolean onSuggestionClick(int position) {
            String query = mSearchView.getQuery().toString();
            mSearchView.clearFocus();

            if (mSuggestionsAdapter.isSearchItem(position)) {
                performSearch(query);
            } else {
                final Uri itemUri = mSuggestionsAdapter.getItemIntentData(position);
                final Intent intent = new Intent(Intent.ACTION_VIEW);
                Screen.SEARCH_SUGGESTIONS.addToIntent(intent);
                mActivity.startActivity(intent.setData(itemUri));
            }
            return true;
        }
    };

    private void performSearch(final String query) {
        if (query.startsWith("#")) {
            mSearchCallback.performTagSearch(query.replaceFirst("#", ""));
        } else {
            mSearchCallback.performTextSearch(query);
        }
    }

    private void styleSearchView(SearchView searchView) {
        try {
            Field searchField = SearchView.class.getDeclaredField("mSearchButton");
            searchField.setAccessible(true);
            ImageView searchBtn = (ImageView) searchField.get(searchView);
            searchBtn.setBackgroundResource(R.drawable.item_background_dark);

            searchField = SearchView.class.getDeclaredField("mCloseButton");
            searchField.setAccessible(true);
            ImageView closeButton = (ImageView) searchField.get(searchView);
            closeButton.setBackgroundResource(R.drawable.item_background_dark);

        } catch (NoSuchFieldException e) {
            Log.e(getClass().getSimpleName(), e.getMessage(), e);
        } catch (IllegalAccessException e) {
            Log.e(getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    public void setQuery(String query) {
        mSearchView.setQuery(query, false);
        mSearchView.clearFocus();
    }

    public String getQuery() {
        return mSearchView.getQuery().toString();
    }

    public void requestSearchFieldFocus() {
        mSearchView.requestFocus();
    }
}
