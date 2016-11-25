package com.soundcloud.android.screens.discovery;

import static org.hamcrest.MatcherAssert.assertThat;

import com.soundcloud.android.R;
import com.soundcloud.android.discovery.SearchActivity;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ImageViewElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SearchScreen extends Screen {

    private static final Class ACTIVITY = SearchActivity.class;

    public SearchScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    public SearchResultsScreen doSearch(String query) {
        return actionBar().doSearch(query);
    }

    public SearchScreen setSearchQuery(String query) {
        actionBar().setSearchQuery(query);
        return this;
    }

    public String getSearchQuery() {
        return actionBar().getSearchQuery();
    }

    public ProfileScreen clickOnUserSuggestion() {
        testDriver.findOnScreenElement(UserSuggestion()).click();
        return new ProfileScreen(testDriver);
    }

    public VisualPlayerElement clickOnTrackSuggestion() {
        testDriver.findOnScreenElement(TrackSuggestion()).click();
        return new VisualPlayerElement(testDriver);
    }

    public PlaylistDetailsScreen clickOnPlaylistSuggestion() {
        testDriver.findOnScreenElement(PlaylistSuggestion()).click();
        return new PlaylistDetailsScreen(testDriver);
    }

    public SearchResultsScreen clickOnAutocompleteSuggestion() {
        List<ViewElement> autocompleteResults = testDriver.findOnScreenElements(AutocompleteSuggestion());

        assertThat("Expected autocomplete results, have: " + autocompleteResults.size(),
                   autocompleteResults.size() > 1);

        autocompleteResults.get(1) // item 0 would be the "local" result we always show to execute the search
                           .click();
        return new SearchResultsScreen(testDriver);
    }

    public SearchScreen dismissSearch() {
        actionBar().dismissSearch();
        return this;
    }

    public boolean hasSearchResults() {
        return testDriver.findOnScreenElement(With.id(R.id.ak_recycler_view)).isOnScreen();
    }

    private static With UserSuggestion() {
        return Suggestion(R.drawable.ic_search_user, "user");
    }

    private static With TrackSuggestion() {
        return Suggestion(R.drawable.ic_search_sound, "sound");
    }

    private static With PlaylistSuggestion() {
        return Suggestion(R.drawable.ic_search_playlist, "playlist");
    }

    private static With AutocompleteSuggestion() {
        return With.id(R.id.search_icon);
    }

    private static With Suggestion(final int drawableId, final String description) {
        return new With() {
            @Override
            public String getSelector() {
                return "Search Suggestion for " + description;
            }

            @Override
            public boolean apply(@Nullable ViewElement input) {
                return input.getId() == R.id.iv_search_type && new ImageViewElement(input).hasDrawable(drawableId);
            }
        };
    }
}
