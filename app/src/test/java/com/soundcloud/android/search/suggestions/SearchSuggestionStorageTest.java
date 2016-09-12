package com.soundcloud.android.search.suggestions;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;

import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class SearchSuggestionStorageTest extends StorageIntegrationTest {

    private SearchSuggestionStorage suggestionStorage;
    private TestSubscriber<List<PropertySet>> subscriber;

    private ApiTrack apiTrack;
    private PropertySet trackPropertySet;
    private ApiPlaylist apiPlaylist;
    private PropertySet playlistPropertySet;
    private ApiUser apiUser;
    private PropertySet userPropertySet;


    @Before
    public void setUp() throws Exception {
        suggestionStorage = new SearchSuggestionStorage(propeller());
        subscriber = new TestSubscriber<>();

        apiTrack = testFixtures().insertLikedTrack(new Date());
        trackPropertySet = buildPropertySetFromApiTrack(apiTrack);
        apiPlaylist = testFixtures().insertLikedPlaylist(new Date());
        playlistPropertySet = buildPropertySetFromApiPlaylist(apiPlaylist);
        apiUser = testFixtures().insertUser();
        userPropertySet = buildPropertySetFromApiUser(apiUser);
        testFixtures().insertFollowing(apiUser.getUrn());
    }

    @Test
    public void returnsTrackLikeFromStorageMatchedOnFirstWord() {
        final Observable<List<PropertySet>> suggestions = suggestionStorage.getSuggestions(apiTrack.getTitle()
                                                                                                   .substring(0, 3), 1);
        suggestions.subscribe(subscriber);

        subscriber.assertValue(toList(trackPropertySet));
    }

    @Test
    public void returnsMatchedTrackLikeFromStorageMatchedOnSecondWord() {
        final String title = apiTrack.getTitle();
        final int startIndex = title.indexOf(" ");
        final Observable<List<PropertySet>> suggestions = suggestionStorage.getSuggestions(title.substring(startIndex + 1,
                                                                                                           startIndex + 3),
                                                                                           1);
        suggestions.subscribe(subscriber);

        subscriber.assertValue(toList(trackPropertySet));
    }

    @Test
    public void returnsMatchedPlaylistLikeFromStorage() {
        final Observable<List<PropertySet>> suggestions = suggestionStorage.getSuggestions(apiPlaylist.getTitle()
                                                                                                      .substring(0, 3),
                                                                                           1);
        suggestions.subscribe(subscriber);

        subscriber.assertValue(toList(playlistPropertySet));
    }

    @Test
    public void returnsMatchedFollowingFromStorage() {
        final Observable<List<PropertySet>> suggestions = suggestionStorage.getSuggestions(apiUser.getUsername()
                                                                                                  .substring(0, 3), 1);
        suggestions.subscribe(subscriber);

        subscriber.assertValue(toList(userPropertySet));
    }

    @Test
    public void returnsAllTypesFromStorage() {
        final Observable<List<PropertySet>> suggestions = suggestionStorage.getSuggestions("", 3);
        suggestions.subscribe(subscriber);

        final List<PropertySet> emmittedElements = subscriber.getOnNextEvents().get(0);
        final List<PropertySet> expectedElements = toList(userPropertySet, trackPropertySet, playlistPropertySet);

        assertThat(emmittedElements).containsAll(expectedElements);
    }

    @Test
    public void returnsLimitedItemsFromStorage() {
        final Observable<List<PropertySet>> suggestions = suggestionStorage.getSuggestions("", 1);
        suggestions.subscribe(subscriber);

        subscriber.assertValue(toList(userPropertySet));
    }

    @Test
    public void returnsOnlyMatchedItemFromStorage() {
        final Observable<List<PropertySet>> suggestions = suggestionStorage.getSuggestions(apiUser.getUsername(), 3);
        suggestions.subscribe(subscriber);

        subscriber.assertValue(toList(userPropertySet));
    }

    @NonNull
    private PropertySet buildPropertySetFromApiTrack(final ApiTrack apiTrack) {
        return initPropertySets(apiTrack.getUrn(), apiTrack.getTitle(), apiTrack.getImageUrlTemplate());
    }

    @NonNull
    private PropertySet buildPropertySetFromApiPlaylist(final ApiPlaylist apiPlaylist) {
        return initPropertySets(apiPlaylist.getUrn(), apiPlaylist.getTitle(), apiPlaylist.getImageUrlTemplate());
    }

    @NonNull
    private PropertySet buildPropertySetFromApiUser(final ApiUser apiUser) {
        return initPropertySets(apiUser.getUrn(), apiUser.getUsername(), apiUser.getImageUrlTemplate());
    }

    @NonNull
    private PropertySet initPropertySets(Urn urn, String title, Optional<String> imageUrl) {
        final PropertySet propertySet = PropertySet.create();
        propertySet.put(SearchSuggestionProperty.URN, urn);
        propertySet.put(SearchSuggestionProperty.DISPLAY_TEXT, title);
        propertySet.put(SearchSuggestionProperty.HIGHLIGHT, Optional.<SuggestionHighlight>absent());
        propertySet.put(EntityProperty.IMAGE_URL_TEMPLATE, imageUrl);
        return propertySet;
    }

    @NonNull
    private List<PropertySet> toList(PropertySet... items) {
        final List<PropertySet> propertySets = Lists.newArrayList();
        Collections.addAll(propertySets, items);
        return propertySets;
    }
}
