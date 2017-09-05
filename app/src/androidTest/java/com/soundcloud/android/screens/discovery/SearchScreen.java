package com.soundcloud.android.screens.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ImageViewElement;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.olddiscovery.SearchActivity;
import com.soundcloud.android.screens.PlaylistDetailsScreen;
import com.soundcloud.android.screens.ProfileScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import org.jetbrains.annotations.Nullable;

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
        testDriver.findOnScreenElement(userSuggestion()).click();
        return new ProfileScreen(testDriver);
    }

    public VisualPlayerElement clickOnTrackSuggestion() {
        testDriver.findOnScreenElement(trackSuggestion()).click();
        return new VisualPlayerElement(testDriver);
    }

    public PlaylistDetailsScreen clickOnPlaylistSuggestion() {
        testDriver.findOnScreenElement(playlistSuggestion()).click();
        return new PlaylistDetailsScreen(testDriver);
    }

    public String firstAutocompleteSuggestionText() {
        return new TextElement(testDriver.findOnScreenElement(autocompleteSuggestion())).getText();
    }

    public SearchResultsScreen clickOnFirstAutocompleteSuggestion() {
        testDriver.findOnScreenElement(autocompleteSuggestion())
                .click();

        return new SearchResultsScreen(testDriver);
    }

    public SearchScreen clickOnFirstAutocompleteSuggestionArrow() {
        testDriver.findOnScreenElement(autocompleteSuggestionArrow())
                  .click();

        return this;
    }

    public SearchScreen dismissSearch() {
        actionBar().dismissSearch();
        return this;
    }

    public boolean hasSearchResults() {
        return testDriver.findOnScreenElement(With.id(R.id.ak_recycler_view)).isOnScreen();
    }

    private static With userSuggestion() {
        return suggestion(R.drawable.ic_search_user, "user");
    }

    private static With trackSuggestion() {
        return suggestion(R.drawable.ic_search_sound, "sound");
    }

    private static With playlistSuggestion() {
        return suggestion(R.drawable.ic_search_playlist, "playlist");
    }

    private static With autocompleteSuggestion() {
        return With.id(R.id.search_title);
    }

    private static With autocompleteSuggestionArrow() {
        return With.id(R.id.arrow_icon);
    }

    private static With suggestion(final int drawableId, final String description) {
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
