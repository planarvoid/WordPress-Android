package com.soundcloud.android.actionbar;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.search.suggestions.SuggestionsAdapter;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.tracks.TrackUrn;
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

import javax.inject.Inject;
import javax.inject.Provider;
import java.lang.reflect.Field;

public class SearchActionBarController extends ActionBarController {

    private SuggestionsAdapter suggestionsAdapter;

    private SearchView searchView;

    private final PublicCloudAPI publicApi;
    private final PlaybackOperations playbackOperations;
    private final SearchCallback searchCallback;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;


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
                launchSuggestion(position);
            }
            return true;
        }
    };

    private void launchSuggestion(int position) {
        final Uri itemUri = suggestionsAdapter.getItemIntentData(position);

        final boolean localResult = suggestionsAdapter.isLocalResult(position);
        if (!localResult) {
            // cache dummy model in ScModelManager, as it will avert NotFoundException in Service class
            // this is very hacky, and should not exist in master... ever. Issue to fix:
            // https://soundcloud.atlassian.net/browse/DROID-358?jql=project%20%3D%20DROID
            cachePlaceholderModel(position, itemUri);

        }

        trackSuggestion(position, itemUri);

        final Urn urn = suggestionsAdapter.getUrn(position);
        if (urn.isTrack()) {
            playTrack(urn);
        } else {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            Screen.SEARCH_SUGGESTIONS.addToIntent(intent);
            activity.startActivity(intent.setData(itemUri));
        }
    }

    private void playTrack(Urn urn) {
        playbackOperations.startPlaybackWithRecommendations((TrackUrn) urn, Screen.SEARCH_SUGGESTIONS)
                .subscribe(expandPlayerSubscriberProvider.get());
        clearFocus();
        searchView.setSuggestionsAdapter(null);
    }

    private void trackSuggestion(int position, Uri itemUri) {
        final SearchEvent event = SearchEvent.searchSuggestion(
                Content.match(itemUri), suggestionsAdapter.isLocalResult(position));
        eventBus.publish(EventQueue.SEARCH, event);
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

    public interface SearchCallback {
        void performTextSearch(String query);

        void performTagSearch(String tag);

        void exitSearchMode();
    }

    SearchActionBarController(@NotNull ActionBarOwner owner, PublicCloudAPI publicCloudAPI,
                              SearchCallback searchCallback, PlaybackOperations playbackOperations,
                              EventBus eventBus, Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider) {
        super(owner, eventBus);
        this.publicApi = publicCloudAPI;
        this.searchCallback = searchCallback;
        this.playbackOperations = playbackOperations;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
    }

    public void onDestroy() {
        // Suggestions adapter has to stop handler thread
        if (suggestionsAdapter != null) {
            suggestionsAdapter.onDestroy();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu) {
        ActionBar actionBar = owner.getActivity().getSupportActionBar();
        configureSearchState(menu, actionBar);
        if (!actionBar.isShowing()) {
            searchView.clearFocus();
        }
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

    public static class Factory {

        private final EventBus eventBus;
        private final PublicCloudAPI publicCloudAPI;
        private final PlaybackOperations playbackOperations;
        private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;

        @Inject
        public Factory(EventBus eventBus, PublicCloudAPI publicCloudAPI, PlaybackOperations playbackOperations,
                       Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider) {
            this.eventBus = eventBus;
            this.publicCloudAPI = publicCloudAPI;
            this.playbackOperations = playbackOperations;
            this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        }

        public SearchActionBarController create(ActionBarOwner owner, SearchCallback searchCallback) {
            return new SearchActionBarController(owner, publicCloudAPI, searchCallback, playbackOperations,
                    eventBus, expandPlayerSubscriberProvider);

        }
    }

}
