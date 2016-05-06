package com.soundcloud.android.search.suggestions;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.RecordHolder;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.profile.WriteMixedRecordsCommand;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.matchers.ApiRequestTo;
import com.soundcloud.java.collections.PropertySet;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import android.support.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SearchSuggestionOperationsTest extends AndroidUnitTest{

    private static final String SEARCH_QUERY = "query";
    private static final int MAX_RESULTS_NUMBER = 5;

    @Mock private ApiClientRx apiClientRx;
    @Mock private WriteMixedRecordsCommand writeMixedRecordsCommand;
    @Mock private SearchSuggestionStorage suggestionStorage;
    @Captor private ArgumentCaptor<Iterable<RecordHolder>> recordIterableCaptor;

    private SearchSuggestionOperations operations;
    private TestSubscriber<ApiSearchSuggestions> subscriber;
    private TestSubscriber<SuggestionsResult> suggestionsResultSubscriber;
    private ApiTrack track;
    private ApiUser user;

    @Before
    public void setUp() throws Exception {
        operations = new SearchSuggestionOperations(apiClientRx, writeMixedRecordsCommand,
                Schedulers.immediate(), suggestionStorage);
        subscriber = new TestSubscriber<>();
        suggestionsResultSubscriber = new TestSubscriber<>();
        track = ModelFixtures.create(ApiTrack.class);
        user = ModelFixtures.create(ApiUser.class);
    }

    @Test
    public void returnsSuggestionsFromApi() {
        final ApiSearchSuggestions suggestions = setupSuggestionsFetch();

        operations.searchSuggestions(SEARCH_QUERY).subscribe(subscriber);

        subscriber.assertReceivedOnNext(Collections.singletonList(suggestions));
    }

    @Test
    public void cachesDependenciesFromApi() {
        setupSuggestionsFetch();

        operations.searchSuggestions(SEARCH_QUERY).subscribe(subscriber);

        verify(writeMixedRecordsCommand).call(recordIterableCaptor.capture());

        assertThat(recordIterableCaptor.getValue()).containsExactly(track, user);
    }

    @Test
    public void returnsLocalSuggestionsWhenEmptyRemote() {
        List<PropertySet> localSuggestions = getLocalSuggestions();
        when(suggestionStorage.getSuggestions(SEARCH_QUERY, MAX_RESULTS_NUMBER)).thenReturn(Observable.just(localSuggestions));

        final List<ApiSearchSuggestion> apiSearchSuggestions = Lists.newArrayList();
        setupRemoteSuggestions(apiSearchSuggestions);

        operations.suggestionsFor(SEARCH_QUERY).subscribe(suggestionsResultSubscriber);

        final SuggestionsResult localSuggestionsResult = SuggestionsResult.localFromPropertySets(localSuggestions);
        final SuggestionsResult remoteSuggestionsResult = SuggestionsResult.remoteFromPropertySetSource(apiSearchSuggestions);
        suggestionsResultSubscriber.assertReceivedOnNext(Arrays.asList(localSuggestionsResult, remoteSuggestionsResult));
    }

    @Test
    public void returnsRemoteSuggestionsWhenEmptyLocal() {
        List<PropertySet> localSuggestions = Lists.newArrayList();
        when(suggestionStorage.getSuggestions(SEARCH_QUERY, MAX_RESULTS_NUMBER)).thenReturn(Observable.just(localSuggestions));

        final List<ApiSearchSuggestion> apiSearchSuggestions = Collections.singletonList(getSuggestion(SEARCH_QUERY, track, null));
        setupRemoteSuggestions(apiSearchSuggestions);

        operations.suggestionsFor(SEARCH_QUERY).subscribe(suggestionsResultSubscriber);

        final SuggestionsResult localSuggestionsResult = SuggestionsResult.localFromPropertySets(localSuggestions);
        final SuggestionsResult remoteSuggestionsResult = SuggestionsResult.remoteFromPropertySetSource(apiSearchSuggestions);
        suggestionsResultSubscriber.assertReceivedOnNext(Arrays.asList(localSuggestionsResult, remoteSuggestionsResult));
    }

    @NonNull
    private List<PropertySet> getLocalSuggestions() {
        final PropertySet localSuggestion = PropertySet.create();
        final List<PropertySet> localSuggestions = Lists.newArrayList();
        localSuggestions.add(localSuggestion);
        return localSuggestions;
    }

    @NonNull
    private ApiSearchSuggestions setupRemoteSuggestions(final List<ApiSearchSuggestion> apiSearchSuggestions) {
        final ApiSearchSuggestions suggestions = getApiSearchSuggestions(apiSearchSuggestions);
        final ApiRequestTo requestMatcher = isApiRequestTo("GET", ApiEndpoints.SEARCH_SUGGESTIONS.path())
                .withQueryParam("q", SEARCH_QUERY)
                .withQueryParam("limit", String.valueOf(MAX_RESULTS_NUMBER));

        when(apiClientRx.mappedResponse(argThat(requestMatcher), same(ApiSearchSuggestions.class)))
                .thenReturn(Observable.just(suggestions));
        return suggestions;
    }

    @NonNull
    private ApiSearchSuggestions setupSuggestionsFetch() {
        final ApiSearchSuggestions suggestions = getMixedSuggestions();
        final ApiRequestTo requestMatcher = isApiRequestTo("GET", ApiEndpoints.SEARCH_SUGGESTIONS.path())
                .withQueryParam("q", SEARCH_QUERY)
                .withQueryParam("limit", String.valueOf(LegacySuggestionsAdapter.MAX_REMOTE));

        when(apiClientRx.mappedResponse(argThat(requestMatcher), same(ApiSearchSuggestions.class)))
                .thenReturn(Observable.just(suggestions));
        return suggestions;
    }

    @NonNull
    private ApiSearchSuggestions getMixedSuggestions() {
        return new ApiSearchSuggestions(
                    Arrays.asList(
                            getSuggestion("a", track, null),
                            getSuggestion("b", null, user)
                    ), Urn.forPlaylist(1)
            );
    }

    @NonNull
    private ApiSearchSuggestions getApiSearchSuggestions(final List<ApiSearchSuggestion> suggestions) {
        return new ApiSearchSuggestions(
                suggestions, Urn.forPlaylist(1)
        );
    }

    private ApiSearchSuggestion getSuggestion(String query, ApiTrack track, ApiUser user) {
        return ApiSearchSuggestion.create(
                query,
                Collections.<Map<String,Integer>>emptyList(),
                track,
                user);
    }
}
