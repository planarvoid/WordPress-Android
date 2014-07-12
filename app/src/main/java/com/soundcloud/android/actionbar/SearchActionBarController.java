package com.soundcloud.android.actionbar;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.search.suggestions.SuggestionsAdapter;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.ScTextUtils;
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

    private SuggestionsAdapter suggestionsAdapter;

    private SearchView searchView;

    private final PublicCloudAPI publicApi;
    private final SearchCallback searchCallback;

    private final SearchView.OnSuggestionListener mSuggestionListener = new SearchView.OnSuggestionListener() {
        @Override
        public boolean onSuggestionSelect(int position) {
            return false;
        }

        @Override
        public boolean onSuggestionClick(int position) {
            String query = searchView.getQuery().toString();
            searchView.clearFocus();

            if (suggestionsAdapter.isSearchItem(position)) {
                performSearch(query, true);
                searchView.setSuggestionsAdapter(null);
            } else {
                final Uri itemUri = suggestionsAdapter.getItemIntentData(position);

                final SearchEvent event = SearchEvent.searchSuggestion(
                        Content.match(itemUri), suggestionsAdapter.isLocalResult(position));
                eventBus.publish(EventQueue.SEARCH, event);

                final Intent intent = new Intent(Intent.ACTION_VIEW);
                Screen.SEARCH_SUGGESTIONS.addToIntent(intent);
                activity.startActivity(intent.setData(itemUri));
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
        publicApi = publicCloudAPI;
        this.searchCallback = searchCallback;
    }

    @Override
    public void onDestroy() {
        // Suggestions adapter has to stop handler thread
        if (suggestionsAdapter != null) suggestionsAdapter.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu) {
        ActionBar actionBar = owner.getActivity().getSupportActionBar();
        configureSearchState(menu, actionBar);
    }

    private void configureSearchState(Menu menu, ActionBar actionBar) {
        actionBar.setDisplayShowCustomEnabled(false);
        owner.getActivity().getMenuInflater().inflate(R.menu.search, menu);

        initSearchView(menu);
        searchView.setOnQueryTextListener(mQueryTextListener);
        searchView.setOnSuggestionListener(mSuggestionListener);
        styleSearchView(searchView);
    }

    private void initSearchView(Menu menu) {
        SearchManager searchManager = (SearchManager) activity.getSystemService(Context.SEARCH_SERVICE);
        final SearchableInfo searchableInfo = searchManager.getSearchableInfo(activity.getComponentName());

        SupportMenuItem searchItem = (SupportMenuItem) menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setSearchableInfo(searchableInfo);
        searchView.setIconifiedByDefault(false);
        searchView.setIconified(false);
        searchView.setQueryHint(owner.getActivity().getString(R.string.search_hint));
        searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH | EditorInfo.IME_FLAG_NO_EXTRACT_UI);

        suggestionsAdapter = new SuggestionsAdapter(activity, publicApi, activity.getContentResolver());
        searchView.setSuggestionsAdapter(suggestionsAdapter);
    }

    private final SearchView.OnQueryTextListener mQueryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            performSearch(query, false);
            clearFocus();
            searchView.setSuggestionsAdapter(null);
            return true;
        }

        @Override
        public boolean onQueryTextChange(String s) {
            if (searchView.getSuggestionsAdapter() == null) {
                // This is nulled on search as a workaround to unwanted focus on some devices
                searchView.setSuggestionsAdapter(suggestionsAdapter);
            }
            if (s.length() < 1) {
                searchCallback.exitSearchMode();
            }
            return false;
        }
    };

    @VisibleForTesting
    void performSearch(final String query, boolean viaShortcut) {
        String trimmedQuery = query.trim();
        boolean tagSearch = isTagSearch(trimmedQuery);

        eventBus.publish(EventQueue.SEARCH,
                SearchEvent.searchField(query, viaShortcut, tagSearch));

        if (tagSearch) {
            performTagSearch(trimmedQuery);
        } else {
            searchCallback.performTextSearch(trimmedQuery);
        }
    }

    private boolean isTagSearch(String trimmedQuery) {
        return trimmedQuery.startsWith("#");
    }

    private void performTagSearch(final String query) {
        String tag = query.replaceAll("^#+", ""); // Replaces the first occurrences of #
        if (!Strings.isNullOrEmpty(tag)) {
            searchCallback.performTagSearch(tag);
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

    private boolean isInitialised() {
        return searchView != null;
    }

    public String getQuery() {
        if (isInitialised()) {
            return searchView.getQuery().toString();
        } else {
            return ScTextUtils.EMPTY_STRING;
        }
    }

    public void setQuery(String query) {
        if (isInitialised()) {
            searchView.setQuery(query, false);
            searchView.setSuggestionsAdapter(null);
            clearFocus();
        }
    }

    public void clearQuery() {
        if (isInitialised()) {
            searchView.setQuery("", false);
        }
    }

    public void clearFocus() {
        if (isInitialised()) {
            searchView.clearFocus();
        }
    }

}
