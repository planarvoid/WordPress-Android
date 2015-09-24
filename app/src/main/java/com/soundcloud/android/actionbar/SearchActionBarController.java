package com.soundcloud.android.actionbar;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.search.suggestions.SuggestionsAdapter;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;

import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.v4.internal.view.SupportMenuItem;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageView;

import javax.inject.Inject;
import javax.inject.Provider;
import java.lang.reflect.Field;

public class SearchActionBarController extends DefaultActivityLightCycle<AppCompatActivity> {
    private static final String STATE_QUERY = "query";
    private final PublicApi publicApi;
    private final PlaybackInitiator playbackInitiator;
    private final EventBus eventBus;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    private final SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
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

    private SuggestionsAdapter suggestionsAdapter;
    private SearchCallback searchCallback;
    private SearchView searchView;
    private String query;

    @Inject
    SearchActionBarController(PublicApi publicCloudAPI,
                              PlaybackInitiator playbackInitiator,
                              EventBus eventBus,
                              Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider) {
        this.publicApi = publicCloudAPI;
        this.playbackInitiator = playbackInitiator;
        this.eventBus = eventBus;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
    }

    public void setSearchCallback(SearchCallback searchCallback) {
        this.searchCallback = searchCallback;
    }

    @Override
    public void onCreate(AppCompatActivity activity, @Nullable Bundle savedInstanceState) {
        activity.getSupportActionBar().setDisplayShowTitleEnabled(false);
        if (savedInstanceState != null && savedInstanceState.containsKey(STATE_QUERY)) {
            query = savedInstanceState.getString(STATE_QUERY);
        }
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        // Suggestions adapter has to stop handler thread
        if (suggestionsAdapter != null) {
            suggestionsAdapter.onDestroy();
        }
    }

    public String getQuery() {
        if (isInitialised()) {
            return searchView.getQuery().toString();
        } else {
            return ScTextUtils.EMPTY_STRING;
        }
    }

    public void setQuery(String query) {
        this.query = query;
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

    public void setFocusable(boolean focusable) {
        if (isInitialised()) {
            searchView.setFocusable(focusable);
        }
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater, final AppCompatActivity activity) {
        inflater.inflate(R.menu.search, menu);
        initSearchView(activity, menu);

        if (!activity.getSupportActionBar().isShowing()) {
            clearFocus();
        }

        if (Strings.isNotBlank(query)) {
            setQuery(query);
        }
    }

    @NonNull
    private SearchView.OnSuggestionListener getSuggestionListener(final AppCompatActivity activity) {
        return new SearchView.OnSuggestionListener() {
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
                    launchSuggestion(activity, position);
                }
                return true;
            }
        };
    }

    private void launchSuggestion(AppCompatActivity activity, int position) {
        final Uri itemUri = suggestionsAdapter.getItemIntentData(position);

        final boolean localResult = suggestionsAdapter.isLocalResult(position);
        if (!localResult) {
            // cache dummy model in ScModelManager, as it will avert NotFoundException in Service class
            // this is very hacky, and should not exist in master... ever. Issue to fix:
            // https://soundcloud.atlassian.net/browse/DROID-358?jql=project%20%3D%20DROID
            cachePlaceholderModel(position, itemUri);

        }

        SearchQuerySourceInfo searchQuerySourceInfo = getQuerySourceInfo(position);
        trackSuggestion(position, itemUri, searchQuerySourceInfo);

        final Urn urn = suggestionsAdapter.getUrn(position);
        if (urn.isTrack()) {
            playTrack(urn, searchQuerySourceInfo);
        } else {
            final Intent intent = new Intent(Intent.ACTION_VIEW);

            if (urn.isUser()) {
                intent.putExtra(ProfileActivity.EXTRA_SEARCH_QUERY_SOURCE_INFO, searchQuerySourceInfo);
            }

            Screen.SEARCH_SUGGESTIONS.addToIntent(intent);
            activity.startActivity(intent.setData(itemUri));
        }
    }

    private void playTrack(Urn urn, SearchQuerySourceInfo searchQuerySourceInfo) {
        playbackInitiator.startPlaybackWithRecommendations(urn, Screen.SEARCH_SUGGESTIONS, searchQuerySourceInfo)
                .subscribe(expandPlayerSubscriberProvider.get());
        clearFocus();
        searchView.setSuggestionsAdapter(null);
    }

    private void trackSuggestion(int position, Uri itemUri, SearchQuerySourceInfo searchQuerySourceInfo) {
        final SearchEvent event = SearchEvent.searchSuggestion(
                Content.match(itemUri), suggestionsAdapter.isLocalResult(position), searchQuerySourceInfo);
        eventBus.publish(EventQueue.TRACKING, event);
    }

    private void cachePlaceholderModel(int position, Uri itemUri) {
        final long modelId = suggestionsAdapter.getModelId(position);
        switch (Content.match(itemUri)) {
            case TRACK:
                SoundCloudApplication.sModelManager.cache(new PublicApiTrack(modelId));
            case USER:
                SoundCloudApplication.sModelManager.cache(new PublicApiUser(modelId));
            default:
                // should never happen. I am not failing fast on hotfix code though
                break;
        }
    }

    private void initSearchView(AppCompatActivity activity, Menu menu) {
        SearchManager searchManager = (SearchManager) activity.getSystemService(Context.SEARCH_SERVICE);
        final SearchableInfo searchableInfo = searchManager.getSearchableInfo(activity.getComponentName());

        SupportMenuItem searchItem = (SupportMenuItem) menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        searchView.setSearchableInfo(searchableInfo);
        searchView.setIconifiedByDefault(false);
        searchView.setIconified(false);
        searchView.setQueryHint(activity.getString(R.string.search_hint));
        searchView.setImeOptions(EditorInfo.IME_ACTION_SEARCH | EditorInfo.IME_FLAG_NO_EXTRACT_UI);

        suggestionsAdapter = new SuggestionsAdapter(activity, publicApi, activity.getContentResolver());
        searchView.setSuggestionsAdapter(suggestionsAdapter);

        searchView.setOnQueryTextListener(queryTextListener);
        searchView.setOnSuggestionListener(getSuggestionListener(activity));
        styleSearchView(searchView);
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

    @VisibleForTesting
    void performSearch(final String query, boolean viaShortcut) {
        String trimmedQuery = query.trim();
        boolean tagSearch = isTagSearch(trimmedQuery);

        eventBus.publish(EventQueue.TRACKING,
                SearchEvent.searchField(query, viaShortcut, tagSearch));

        if (tagSearch) {
            performTagSearch(trimmedQuery);
        } else {
            searchCallback.performTextSearch(trimmedQuery);
        }
    }

    private SearchQuerySourceInfo getQuerySourceInfo(int position) {
        SearchQuerySourceInfo searchQuerySourceInfo = null;
        Urn queryUrn = suggestionsAdapter.getQueryUrn(position);

        if (!queryUrn.equals(Urn.NOT_SET)) {
            searchQuerySourceInfo = new SearchQuerySourceInfo(queryUrn,
                    suggestionsAdapter.getQueryPosition(position),
                    suggestionsAdapter.getUrn(position));
        }

        return searchQuerySourceInfo;
    }

    public interface SearchCallback {
        void performTextSearch(String query);

        void performTagSearch(String tag);

        void exitSearchMode();
    }
}
