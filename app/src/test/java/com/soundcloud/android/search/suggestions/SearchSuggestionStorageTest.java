package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.java.optional.Optional;
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
    private ApiUser followingUser;
    private ApiUser loggedInUser;
    private ApiUser creator;
    private SearchSuggestion followingUserSearchSuggestion;
    private SearchSuggestion loggedInUserSearchSuggestion;
    private SearchSuggestion likedTrackArtistUsernameSearchSuggestion;
    private SearchSuggestion likedPlaylistArtistUsernameSearchSuggestion;

    @Before
    public void setUp() throws Exception {
        suggestionStorage = new SearchSuggestionStorage(propeller());

        followingUser = testFixtures().insertUser("Random account");
        loggedInUser = testFixtures().insertUser("Myself");
        creator = testFixtures().insertUser("Prolific artist");

        likedTrack = testFixtures().insertTrackWithTitle("A tune I enjoy", creator);
        testFixtures().insertLikedTrack(likedTrack, 100L);
        likedTrackSearchSuggestion = buildSearchSuggestionFromApiTrack(likedTrack);
        likedTrackArtistUsernameSearchSuggestion = buildSearchSuggestionFromApiTrackArtistUsername(likedTrack);

        ownedTrack = testFixtures().insertTrackWithTitle("Awesome song that I created", loggedInUser);
        testFixtures().insertTrackPost(ownedTrack, 100L);
        ownedTrackSearchSuggestion = buildSearchSuggestionFromApiTrack(ownedTrack);

        likedPlaylist = testFixtures().insertPlaylistWithTitle("Liked playlist", creator);
        testFixtures().insertLikedPlaylist(new Date(0L), likedPlaylist);
        likedPlaylistSearchSuggestion = buildSearchSuggestionFromApiPlaylist(likedPlaylist);
        likedPlaylistArtistUsernameSearchSuggestion = buildSearchSuggestionFromApiPlaylistArtistUsername(likedPlaylist);

        ownedPlaylist = testFixtures().insertPlaylistWithTitle("Cool mix created by me", loggedInUser);
        testFixtures().insertPostedPlaylist(ownedPlaylist, 0L);
        ownedPlaylistSearchSuggestion = buildSearchSuggestionFromApiPlaylist(ownedPlaylist);

        followingUserSearchSuggestion = buildSearchSuggestionFromApiUser(followingUser);
        loggedInUserSearchSuggestion = buildSearchSuggestionFromApiUser(loggedInUser);
        testFixtures().insertFollowing(followingUser.getUrn(), 0L);
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
    public void returnsMultipleMatchesForLikedTrackByMostRecentlyLiked() {
        final ApiTrack secondLikedTrack = testFixtures().insertTrackWithTitle(likedTrack.getTitle(), creator, likedTrack.getCreatedAt().getTime() - 1);
        testFixtures().insertLikedTrack(secondLikedTrack, 500L);
        final SearchSuggestion secondLikedTrackSearchSuggestion = buildSearchSuggestionFromApiTrack(secondLikedTrack);

        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(likedTrack.getTitle().substring(0, 3), loggedInUser.getUrn(), 2);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(secondLikedTrackSearchSuggestion,
                                                  likedTrackSearchSuggestion));
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
    public void returnsMultipleMatchesForOwnedTrackByMostRecentlyPosted() {
        final ApiTrack secondOwnedTrack = testFixtures().insertTrackWithTitle(ownedTrack.getTitle(), loggedInUser, ownedTrack.getCreatedAt().getTime() - 1);
        testFixtures().insertTrackPost(secondOwnedTrack, 500L);
        final SearchSuggestion secondOwnedTrackSearchSuggestion = buildSearchSuggestionFromApiTrack(secondOwnedTrack);

        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(ownedTrack.getTitle().substring(0, 3), loggedInUser.getUrn(), 2);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(secondOwnedTrackSearchSuggestion,
                                                  ownedTrackSearchSuggestion));
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
    public void returnsLikedTrackAndPlaylistFromStorageMatchedOnFirstWordOfArtistUsername() {
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(creator.getUsername().substring(0, 3), loggedInUser.getUrn(), 2);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(likedTrackArtistUsernameSearchSuggestion,
                                                  likedPlaylistArtistUsernameSearchSuggestion));
    }

    @Test
    public void returnsLikedTrackAndPlaylistFromStorageMatchedOnSecondWordOfArtistUsername() {
        final String username = creator.getUsername();
        final int startIndex = username.indexOf(" ");
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(username.substring(startIndex + 1, startIndex + 3), loggedInUser.getUrn(), 2);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(likedTrackArtistUsernameSearchSuggestion,
                                                  likedPlaylistArtistUsernameSearchSuggestion));
    }

    @Test
    public void removesDuplicateWhenUserSearchesByArtistNameAndLikedTrackWithTitleContainingArtistName() {
        ApiTrack likedTrackWithArtistName = testFixtures().insertTrackWithTitle("Great song by " + creator.getUsername(), creator);
        testFixtures().insertLikedTrack(likedTrackWithArtistName, 500L);
        final SearchSuggestion likedTrackWithArtistNameSearchSuggestion = buildSearchSuggestionFromApiTrack(likedTrackWithArtistName);

        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(creator.getUsername().substring(0, 3), loggedInUser.getUrn(), 6);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(likedTrackWithArtistNameSearchSuggestion,
                                                  likedTrackArtistUsernameSearchSuggestion,
                                                  likedPlaylistArtistUsernameSearchSuggestion));
    }

    @Test
    public void returnsUserAndLikedTrackAndPlaylistInCorrectOrderWhenCreatorIsFollowed() {
        testFixtures().insertFollowing(creator.getUrn());
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(creator.getUsername().substring(0, 3), loggedInUser.getUrn(), 4);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(buildSearchSuggestionFromApiUser(creator),
                                                  likedTrackArtistUsernameSearchSuggestion,
                                                  likedPlaylistArtistUsernameSearchSuggestion));
    }

    @Test
    public void returnsFollowingUserFromStorageMatchedOnFirstWord() {
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(followingUser.getUsername().substring(0, 3), loggedInUser.getUrn(), 1);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(followingUserSearchSuggestion));
    }

    @Test
    public void returnsFollowingUserFromStorageMatchedOnSecondWord() {
        final String username = followingUser.getUsername();
        final int startIndex = username.indexOf(" ");
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(username.substring(startIndex + 1, startIndex + 3), loggedInUser.getUrn(), 2);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(followingUserSearchSuggestion));
    }

    @Test
    public void returnsMultipleMatchesForFollowingUserSortedByMostRecentlyFollowed() {
        final ApiUser secondFollowingUser = testFixtures().insertUser(followingUser.getUsername(), followingUser.getCreatedAt().get().getTime() - 1);
        testFixtures().insertFollowing(secondFollowingUser.getUrn(), 500L);
        final SearchSuggestion secondFollowingUserSearchSuggestion = buildSearchSuggestionFromApiUser(secondFollowingUser);

        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(followingUser.getUsername().substring(0, 3), loggedInUser.getUrn(), 2);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(secondFollowingUserSearchSuggestion,
                                                  followingUserSearchSuggestion));
    }

    @Test
    public void returnsAllTypesFromStorageInTheCorrectOrder() {
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions("", loggedInUser.getUrn(), 20);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(likedTrackSearchSuggestion,
                                                  likedPlaylistSearchSuggestion,
                                                  followingUserSearchSuggestion,
                                                  loggedInUserSearchSuggestion,
                                                  ownedTrackSearchSuggestion,
                                                  ownedPlaylistSearchSuggestion));
    }

    @Test
    public void returnsLimitedItemsFromStorage() {
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions("", loggedInUser.getUrn(), 1);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(likedTrackSearchSuggestion));
    }

    @Test
    public void returnsOnlyMatchedItemFromStorage() {
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(followingUser.getUsername(), loggedInUser.getUrn(), 3);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(followingUserSearchSuggestion));
    }

    @Test
    public void returnsLoggedInItemFromStorage() {
        final Single<List<SearchSuggestion>> suggestions = suggestionStorage.getSuggestions(loggedInUser.getUsername(), loggedInUser.getUrn(), 3);
        final TestObserver<List<SearchSuggestion>> subscriber = suggestions.test();

        subscriber.assertValue(Lists.newArrayList(loggedInUserSearchSuggestion));
    }

    @NonNull
    private SearchSuggestion buildSearchSuggestionFromApiTrack(final ApiTrack apiTrack) {
        return DatabaseSearchSuggestion.create(apiTrack.getUrn(), apiTrack.getTitle(), apiTrack.getImageUrlTemplate(), Optional.absent());
    }

    @NonNull
    private SearchSuggestion buildSearchSuggestionFromApiTrackArtistUsername(final ApiTrack apiTrack) {
        return DatabaseSearchSuggestion.create(apiTrack.getUrn(), apiTrack.getUserName() + " - " + apiTrack.getTitle(), apiTrack.getImageUrlTemplate(), Optional.of(apiTrack.getTitle()));
    }

    @NonNull
    private SearchSuggestion buildSearchSuggestionFromApiPlaylist(final ApiPlaylist apiPlaylist) {
        return DatabaseSearchSuggestion.create(apiPlaylist.getUrn(), apiPlaylist.getTitle(), apiPlaylist.getImageUrlTemplate(), Optional.absent());
    }

    @NonNull
    private SearchSuggestion buildSearchSuggestionFromApiPlaylistArtistUsername(final ApiPlaylist apiPlaylist) {
        return DatabaseSearchSuggestion.create(apiPlaylist.getUrn(), apiPlaylist.getUsername() + " - " + apiPlaylist.getTitle(), apiPlaylist.getImageUrlTemplate(), Optional.of(apiPlaylist.getTitle()));
    }

    @NonNull
    private SearchSuggestion buildSearchSuggestionFromApiUser(final ApiUser apiUser) {
        return DatabaseSearchSuggestion.create(apiUser.getUrn(), apiUser.getUsername(), apiUser.getImageUrlTemplate(), Optional.absent());
    }
}
