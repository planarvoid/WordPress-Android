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

    private ApiTrack likedTrack;
    private ApiTrack ownedTrack;
    private SearchSuggestion likedTrackSearchSuggestion;
    private SearchSuggestion ownedTrackSearchSuggestion;
    private ApiPlaylist likedPlaylist;
    private SearchSuggestion likedPlaylistSearchSuggestion;
    private ApiPlaylist ownedPlaylist;
    private SearchSuggestion ownedPlaylistSearchSuggestion;
    private ApiUser apiUser;
    private ApiUser loggedInUser;
    private SearchSuggestion userSearchSuggestion;
    private SearchSuggestion loggedInUserSearchSuggestion;

    @Before
    public void setUp() throws Exception {
        suggestionStorage = new SearchSuggestionStorage(propeller());

        apiUser = testFixtures().insertUser();
        loggedInUser = testFixtures().insertUser();

        likedTrack = testFixtures().insertLikedTrack(new Date());
        likedTrackSearchSuggestion = buildSearchSuggestionFromApiTrack(likedTrack);

        ownedTrack = testFixtures().insertTrackWithTitle("Awesome song that I created", loggedInUser);
        testFixtures().insertTrackPost(ownedTrack);
        ownedTrackSearchSuggestion = buildSearchSuggestionFromApiTrack(ownedTrack);

        likedPlaylist = testFixtures().insertPlaylistWithTitle("Liked playlist");
        testFixtures().insertLikedPlaylist(new Date(), likedPlaylist);
        likedPlaylistSearchSuggestion = buildSearchSuggestionFromApiPlaylist(likedPlaylist);

        ownedPlaylist = testFixtures().insertPlaylistWithTitle("Cool mix that I created", loggedInUser);
        testFixtures().insertPostedPlaylist(ownedPlaylist);
        ownedPlaylistSearchSuggestion = buildSearchSuggestionFromApiPlaylist(ownedPlaylist);

        userSearchSuggestion = buildSearchSuggestionFromApiUser(apiUser);
        loggedInUserSearchSuggestion = buildSearchSuggestionFromApiUser(loggedInUser);
        testFixtures().insertFollowing(apiUser.getUrn());
    }

    @Test
    public void returnsLikedTrackFromStorageMatchedOnFirstWord() {
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(likedTrack.getTitle().substring(0, 3), loggedInUser.getUrn(), 1);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(likedTrackSearchSuggestion));
    }

    @Test
    public void returnsLikedTrackFromStorageMatchedOnSecondWord() {
        final String title = likedTrack.getTitle();
        final int startIndex = title.indexOf(" ");
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(title.substring(startIndex + 1, startIndex + 3), loggedInUser.getUrn(), 1);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(likedTrackSearchSuggestion));
    }

    @Test
    public void returnsOwnedTrackFromStorageMatchedOnFirstWord() {
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(ownedTrack.getTitle().substring(0, 3), loggedInUser.getUrn(), 1);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(ownedTrackSearchSuggestion));
    }

    @Test
    public void returnsOwnedTrackLikeFromStorageMatchedOnSecondWord() {
        final String title = ownedTrack.getTitle();
        final int startIndex = title.indexOf(" ");
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(title.substring(startIndex + 1, startIndex + 3), loggedInUser.getUrn(), 1);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(ownedTrackSearchSuggestion));
    }

    @Test
    public void returnsLikedPlaylistFromStorageMatchedOnFirstWord() {
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(likedPlaylist.getTitle().substring(0, 3), loggedInUser.getUrn(), 1);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(likedPlaylistSearchSuggestion));
    }

    @Test
    public void returnsLikedPlaylistFromStorageMatchedOnSecondWord() {
        final String title = likedPlaylist.getTitle();
        final int startIndex = title.indexOf(" ");
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(title.substring(startIndex + 1, startIndex + 3), loggedInUser.getUrn(), 1);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(likedPlaylistSearchSuggestion));
    }

    @Test
    public void returnsOwnedPlaylistFromStorageMatchedOnFirstWord() {
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(ownedPlaylist.getTitle().substring(0, 3), loggedInUser.getUrn(), 1);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(ownedPlaylistSearchSuggestion));
    }

    @Test
    public void returnsOwnedPlaylistFromStorageMatchedOnSecondWord() {
        final String title = ownedPlaylist.getTitle();
        final int startIndex = title.indexOf(" ");
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(title.substring(startIndex + 1, startIndex + 3), loggedInUser.getUrn(), 1);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(ownedPlaylistSearchSuggestion));
    }

    @Test
    public void returnsMatchedFollowingFromStorage() {
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(apiUser.getUsername().substring(0, 3), loggedInUser.getUrn(), 1);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(userSearchSuggestion));
    }

    @Test
    public void returnsAllTypesFromStorage() {
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions("", loggedInUser.getUrn(), 6);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        final List<SearchSuggestion> emittedElements = subscriber.values().get(0);
        final List<SearchSuggestion> expectedElements = Lists.newArrayList(userSearchSuggestion,
                                                                           loggedInUserSearchSuggestion,
                                                                           likedTrackSearchSuggestion,
                                                                           likedPlaylistSearchSuggestion,
                                                                           ownedTrackSearchSuggestion,
                                                                           ownedPlaylistSearchSuggestion);

        assertThat(emittedElements).containsAll(expectedElements);
        assertThat(emittedElements.size()).isEqualTo(expectedElements.size());
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
