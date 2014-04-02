package com.soundcloud.android.actionbar;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.search.suggestions.SuggestionsAdapter;
import com.soundcloud.android.storage.provider.Content;
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
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;

import java.lang.reflect.Field;

public class SearchActionBarController extends ActionBarController {

    private SuggestionsAdapter mSuggestionsAdapter;

    private SearchView mSearchView;

    private final PublicCloudAPI mPublicCloudAPI;
    private final SearchCallback mSearchCallback;

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
                performSearch(query, true);
                mSearchView.setSuggestionsAdapter(null);
            } else {
                final Uri itemUri = mSuggestionsAdapter.getItemIntentData(position);

                final SearchEvent event = SearchEvent.searchSuggestion(
                        Content.match(itemUri), mSuggestionsAdapter.isLocalResult(position));
                mEventBus.publish(EventQueue.SEARCH, event);

                final Intent intent = new Intent(Intent.ACTION_VIEW);
                Screen.SEARCH_SUGGESTIONS.addToIntent(intent);
                mActivity.startActivity(intent.setData(itemUri));
            }

            return true;
        }
    };

    public interface SearchCallback {
        void performTextSearch(String query);
        void performTagSearch(String tag);
        void exitSearchMode();
    }

    public SearchActionBarController(@NotNull ActionBarOwner owner, PublicCloudAPI publicCloudAPI,
                                     SearchCallback searchCallback, EventBus eventBus) {
        super(owner, eventBus);
        mPublicCloudAPI = publicCloudAPI;
        mSearchCallback = searchCallback;
    }

    @Override
    public void onDestroy() {
        // Suggestions adapter has to stop handler thread
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
        mSearchView.setIconified(false);
        mSearchView.setQueryHint(mOwner.getActivity().getString(R.string.search_hint));
        mSearchView.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);

        mSuggestionsAdapter = new SuggestionsAdapter(mActivity, mPublicCloudAPI, mActivity.getContentResolver());
        mSearchView.setSuggestionsAdapter(mSuggestionsAdapter);
    }

    private final SearchView.OnQueryTextListener mQueryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            performSearch(query, false);
            clearFocus();
            mSearchView.setSuggestionsAdapter(null);
            return true;
        }

        @Override
        public boolean onQueryTextChange(String s) {
            if (mSearchView.getSuggestionsAdapter() == null) {
                // This is nulled on search as a workaround to unwanted focus on some devices
                mSearchView.setSuggestionsAdapter(mSuggestionsAdapter);
            }
            if (s.length() < 1) {
                mSearchCallback.exitSearchMode();
            }
            return false;
        }
    };

    @VisibleForTesting
    void performSearch(final String query, boolean viaShortcut) {
        String trimmedQuery = query.trim();
        boolean tagSearch = isTagSearch(trimmedQuery);

        mEventBus.publish(EventQueue.SEARCH,
                SearchEvent.searchField(query, viaShortcut, tagSearch));

        if (tagSearch) {
            performTagSearch(trimmedQuery);
        } else {
            mSearchCallback.performTextSearch(trimmedQuery);
        }
    }

    private boolean isTagSearch(String trimmedQuery) {
        return trimmedQuery.startsWith("#");
    }

    private void performTagSearch(final String query) {
        String tag = query.replaceAll("^#+", ""); // Replaces the first occurrences of #
        if (!Strings.isNullOrEmpty(tag)) {
            mSearchCallback.performTagSearch(tag);
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

    public boolean isInitialised() {
        return mSearchView != null;
    }

    public void setQuery(String query) {
        mSearchView.setQuery(query, false);
        clearFocus();
    }

    public void clearQuery() {
        mSearchView.setQuery("", false);
    }

    public String getQuery() {
        return mSearchView.getQuery().toString();
    }

    public void clearFocus() {
        mSearchView.clearFocus();
    }
}
