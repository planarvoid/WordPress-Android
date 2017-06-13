package com.soundcloud.android.search.suggestions;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;

import android.support.annotation.NonNull;

import java.util.Date;
import java.util.List;

public class SearchSuggestionStorageTest extends StorageIntegrationTest {

    private SearchSuggestionStorage suggestionStorage;

    private ApiTrack apiTrack;
    private SearchSuggestion trackSearchSuggestion;
    private ApiPlaylist apiPlaylist;
    private SearchSuggestion playlistSearchSuggestion;
    private ApiUser apiUser;
    private ApiUser loggedInUser;
    private SearchSuggestion userSearchSuggestion;
    private SearchSuggestion loggedInUserSearchSuggestion;

    @Before
    public void setUp() throws Exception {
        suggestionStorage = new SearchSuggestionStorage(propeller());

        apiTrack = testFixtures().insertLikedTrack(new Date());
        trackSearchSuggestion = buildSearchSuggestionFromApiTrack(apiTrack);
        apiPlaylist = testFixtures().insertLikedPlaylist(new Date());
        playlistSearchSuggestion = buildSearchSuggestionFromApiPlaylist(apiPlaylist);
        apiUser = testFixtures().insertUser();
        loggedInUser = testFixtures().insertUser();
        userSearchSuggestion = buildSearchSuggestionFromApiUser(apiUser);
        loggedInUserSearchSuggestion = buildSearchSuggestionFromApiUser(loggedInUser);
        testFixtures().insertFollowing(apiUser.getUrn());
    }

    @Test
    public void returnsTrackLikeFromStorageMatchedOnFirstWord() {
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(apiTrack.getTitle().substring(0, 3), loggedInUser.getUrn(), 1);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(trackSearchSuggestion));
    }

    @Test
    public void returnsMatchedTrackLikeFromStorageMatchedOnSecondWord() {
        final String title = apiTrack.getTitle();
        final int startIndex = title.indexOf(" ");
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(title.substring(startIndex + 1, startIndex + 3), loggedInUser.getUrn(), 1);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(trackSearchSuggestion));
    }

    @Test
    public void returnsMatchedPlaylistLikeFromStorage() {
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(apiPlaylist.getTitle().substring(0, 3), loggedInUser.getUrn(), 1);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(playlistSearchSuggestion));
    }

    @Test
    public void returnsMatchedFollowingFromStorage() {
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(apiUser.getUsername().substring(0, 3), loggedInUser.getUrn(), 1);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(userSearchSuggestion));
    }

    @Test
    public void returnsAllTypesFromStorage() {
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions("", loggedInUser.getUrn(), 4);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        final List<SearchSuggestion> emmittedElements = subscriber.values().get(0);
        final List<SearchSuggestion> expectedElements = Lists.newArrayList(userSearchSuggestion, trackSearchSuggestion, playlistSearchSuggestion);

        assertThat(emmittedElements).containsAll(expectedElements);
    }

    @Test
    public void returnsLimitedItemsFromStorage() {
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions("", loggedInUser.getUrn(), 1);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(userSearchSuggestion));
    }

    @Test
    public void returnsOnlyMatchedItemFromStorage() {
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(apiUser.getUsername(), loggedInUser.getUrn(), 3);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(userSearchSuggestion));
    }

    @Test
    public void returnsLoggedInItemFromStorage() {
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(loggedInUser.getUsername(), loggedInUser.getUrn(), 3);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(loggedInUserSearchSuggestion));
    }

    @NonNull
    private SearchSuggestion buildSearchSuggestionFromApiTrack(final ApiTrack apiTrack) {
        return DatabaseSearchSuggestion.create(apiTrack.getUrn(), apiTrack.getTitle(), apiTrack.getImageUrlTemplate());
    }

    @NonNull
    private SearchSuggestion buildSearchSuggestionFromApiPlaylist(final ApiPlaylist apiPlaylist) {
        return DatabaseSearchSuggestion.create(apiPlaylist.getUrn(), apiPlaylist.getTitle(), apiPlaylist.getImageUrlTemplate());
    }

    @NonNull
    private SearchSuggestion buildSearchSuggestionFromApiUser(final ApiUser apiUser) {
        return DatabaseSearchSuggestion.create(apiUser.getUrn(), apiUser.getUsername(), apiUser.getImageUrlTemplate());
    }
}
