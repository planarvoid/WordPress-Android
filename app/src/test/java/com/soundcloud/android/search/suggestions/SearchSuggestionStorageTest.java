package com.soundcloud.android.search.suggestions;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;

import android.support.annotation.NonNull;

import java.util.Date;
import java.util.List;

public class SearchSuggestionStorageTest extends StorageIntegrationTest {

    private SearchSuggestionStorage suggestionStorage;
    private TestSubscriber<List<SearchSuggestion>> subscriber;

    private ApiTrack apiTrack;
    private SearchSuggestion trackSearchSuggestion;
    private ApiPlaylist apiPlaylist;
    private SearchSuggestion playlistSearchSuggestion;
    private ApiUser apiUser;
    private SearchSuggestion userSearchSuggestion;


    @Before
    public void setUp() throws Exception {
        suggestionStorage = new SearchSuggestionStorage(propeller());
        subscriber = new TestSubscriber<>();

        apiTrack = testFixtures().insertLikedTrack(new Date());
        trackSearchSuggestion = buildSearchSuggestionFromApiTrack(apiTrack);
        apiPlaylist = testFixtures().insertLikedPlaylist(new Date());
        playlistSearchSuggestion = buildSearchSuggestionFromApiPlaylist(apiPlaylist);
        apiUser = testFixtures().insertUser();
        userSearchSuggestion = buildSearchSuggestionFromApiUser(apiUser);
        testFixtures().insertFollowing(apiUser.getUrn());
    }

    @Test
    public void returnsTrackLikeFromStorageMatchedOnFirstWord() {
        final Observable<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(apiTrack.getTitle()
                                                                                                   .substring(0, 3), 1);
        suggestions.subscribe(subscriber);

        subscriber.assertValue(Lists.newArrayList(trackSearchSuggestion));
    }

    @Test
    public void returnsMatchedTrackLikeFromStorageMatchedOnSecondWord() {
        final String title = apiTrack.getTitle();
        final int startIndex = title.indexOf(" ");
        final Observable<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(title.substring(startIndex + 1,
                                                                                                           startIndex + 3),
                                                                                           1);
        suggestions.subscribe(subscriber);

        subscriber.assertValue(Lists.newArrayList(trackSearchSuggestion));
    }

    @Test
    public void returnsMatchedPlaylistLikeFromStorage() {
        final Observable<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(apiPlaylist.getTitle()
                                                                                                      .substring(0, 3),
                                                                                           1);
        suggestions.subscribe(subscriber);

        subscriber.assertValue(Lists.newArrayList(playlistSearchSuggestion));
    }

    @Test
    public void returnsMatchedFollowingFromStorage() {
        final Observable<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(apiUser.getUsername()
                                                                                                  .substring(0, 3), 1);
        suggestions.subscribe(subscriber);

        subscriber.assertValue(Lists.newArrayList(userSearchSuggestion));
    }

    @Test
    public void returnsAllTypesFromStorage() {
        final Observable<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions("", 3);
        suggestions.subscribe(subscriber);

        final List<SearchSuggestion> emmittedElements = subscriber.getOnNextEvents().get(0);
        final List<SearchSuggestion> expectedElements = Lists.newArrayList(userSearchSuggestion, trackSearchSuggestion, playlistSearchSuggestion);

        assertThat(emmittedElements).containsAll(expectedElements);
    }

    @Test
    public void returnsLimitedItemsFromStorage() {
        final Observable<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions("", 1);
        suggestions.subscribe(subscriber);

        subscriber.assertValue(Lists.newArrayList(userSearchSuggestion));
    }

    @Test
    public void returnsOnlyMatchedItemFromStorage() {
        final Observable<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(apiUser.getUsername(), 3);
        suggestions.subscribe(subscriber);

        subscriber.assertValue(Lists.newArrayList(userSearchSuggestion));
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
