package com.soundcloud.android.search.suggestions;

import static com.soundcloud.android.search.suggestions.SuggestionItem.forAutocompletion;
import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.Link;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.configuration.experiments.AutocompleteConfig;
import com.soundcloud.android.model.RecordHolder;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.profile.WriteMixedRecordsCommand;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.matchers.ApiRequestTo;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Maps;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SearchSuggestionOperationsTest extends AndroidUnitTest {

    private static final String SEARCH_QUERY = "query";
    private static final int MAX_RESULTS_NUMBER = 9;
    private static final Urn QUERY_URN = new Urn("soundcloud:autocomplete:123");

    @Mock private ApiClientRx apiClientRx;
    @Mock private WriteMixedRecordsCommand writeMixedRecordsCommand;
    @Mock private SearchSuggestionStorage suggestionStorage;
    @Mock private AutocompleteConfig autocompleteConfig;
    @Mock private SearchSuggestionFiltering searchSuggestionFiltering;
    @Captor private ArgumentCaptor<Iterable<RecordHolder>> recordIterableCaptor;
    @Captor private ArgumentCaptor<List<SuggestionItem>> suggestionItemsCaptor;

    private SearchSuggestionOperations operations;
    private TestSubscriber<List<SuggestionItem>> suggestionsResultSubscriber;
    private ApiTrack track;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(autocompleteConfig.isEnabled()).thenReturn(false);
        when(searchSuggestionFiltering.filtered(anyListOf(SuggestionItem.class))).thenAnswer(new Answer<List<SuggestionItem>>() {
            @Override
            public List<SuggestionItem> answer(InvocationOnMock invocation) throws Throwable {
                return (List<SuggestionItem>) invocation.getArguments()[0];
            }
        });

        operations = new SearchSuggestionOperations(apiClientRx, writeMixedRecordsCommand,
                                                    Schedulers.immediate(), suggestionStorage,
                                                    autocompleteConfig,
                                                    searchSuggestionFiltering);
        suggestionsResultSubscriber = new TestSubscriber<>();

        track = ModelFixtures.create(ApiTrack.class);
    }

    @Test
    public void returnsLocalSuggestionsWhenEmptyRemote() {
        List<PropertySet> localSuggestions = getLocalSuggestions();
        when(suggestionStorage.getSuggestions(SEARCH_QUERY, MAX_RESULTS_NUMBER)).thenReturn(Observable.just(
                localSuggestions));

        final List<ApiSearchSuggestion> apiSearchSuggestions = newArrayList();
        setupLegacyRemoteSuggestions(apiSearchSuggestions);

        operations.suggestionsFor(SEARCH_QUERY).subscribe(suggestionsResultSubscriber);

        final SuggestionItem suggestionItem = SuggestionItem.forLegacySearch(SEARCH_QUERY);
        final List<SuggestionItem> searchQueryItem = newArrayList(suggestionItem);
        final List<SuggestionItem> localSuggestionItems = newArrayList(suggestionItem,
                                                                       SuggestionItem.forTrack(localSuggestions.get(0),
                                                                                               SEARCH_QUERY));


        final List<List<SuggestionItem>> onNextEvents = suggestionsResultSubscriber.getOnNextEvents();
        assertThat(onNextEvents.get(0)).isEqualTo(searchQueryItem);
        assertThat(onNextEvents.get(1)).isEqualTo(localSuggestionItems);
    }

    @Test
    public void returnsRemoteSuggestionsWhenEmptyLocal() {
        final List<ApiSearchSuggestion> apiSearchSuggestions = Collections.singletonList(getSuggestion(SEARCH_QUERY,
                                                                                                       track,
                                                                                                       null));
        setupLegacyRemoteSuggestions(apiSearchSuggestions);

        when(suggestionStorage.getSuggestions(SEARCH_QUERY,
                                              MAX_RESULTS_NUMBER)).thenReturn(Observable.<List<PropertySet>>empty());

        operations.suggestionsFor(SEARCH_QUERY).subscribe(suggestionsResultSubscriber);

        final SuggestionItem suggestionItem = SuggestionItem.forLegacySearch(SEARCH_QUERY);
        final List<SuggestionItem> searchQueryItem = newArrayList(suggestionItem);
        final List<SuggestionItem> remoteSuggestionItems = newArrayList(suggestionItem,
                                                                        SuggestionItem.forTrack(
                                                                                apiSearchSuggestions.get(0)
                                                                                                    .toPropertySet(),
                                                                                SEARCH_QUERY));


        final List<List<SuggestionItem>> onNextEvents = suggestionsResultSubscriber.getOnNextEvents();
        assertThat(onNextEvents.get(0)).isEqualTo(searchQueryItem);
        assertThat(onNextEvents.get(1)).isEqualTo(remoteSuggestionItems);
    }

    @Test
    public void returnsCorrectOrderWithAutocompleteEnabled() {
        List<PropertySet> localSuggestions = getLocalSuggestions();
        when(suggestionStorage.getSuggestions(SEARCH_QUERY, MAX_RESULTS_NUMBER)).thenReturn(Observable.just(
                localSuggestions));
        when(autocompleteConfig.isEnabled()).thenReturn(true);

        final Autocompletion autocompletion = setupAutocompletionRemoteSuggestions();

        operations.suggestionsFor(SEARCH_QUERY).subscribe(suggestionsResultSubscriber);

        final SuggestionItem localItem = SuggestionItem.forTrack(localSuggestions.get(0), SEARCH_QUERY);
        final SuggestionItem autocompletionItem = forAutocompletion(autocompletion, SEARCH_QUERY, Optional.of(QUERY_URN));

        final List<SuggestionItem> firstItem = newArrayList(localItem);
        final List<SuggestionItem> allItems = newArrayList(localItem,
                                                           autocompletionItem);


        final List<List<SuggestionItem>> onNextEvents = suggestionsResultSubscriber.getOnNextEvents();
        assertThat(onNextEvents.get(0)).isEqualTo(firstItem);
        assertThat(onNextEvents.get(1)).isEqualTo(allItems);
    }

    @NonNull
    private List<PropertySet> getLocalSuggestions() {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        final PropertySet localSuggestion = apiTrack.toPropertySet();
        localSuggestion.put(SearchSuggestionProperty.URN, apiTrack.getUrn());
        final List<PropertySet> localSuggestions = newArrayList();
        localSuggestions.add(localSuggestion);
        return localSuggestions;
    }

    @NonNull
    private ApiSearchSuggestions setupLegacyRemoteSuggestions(final List<ApiSearchSuggestion> apiSearchSuggestions) {
        final ApiSearchSuggestions suggestions = getApiSearchSuggestions(apiSearchSuggestions);
        final ApiRequestTo requestMatcher = isApiRequestTo("GET", ApiEndpoints.SEARCH_SUGGESTIONS.path())
                .withQueryParam("q", SEARCH_QUERY)
                .withQueryParam("limit", String.valueOf(MAX_RESULTS_NUMBER));

        when(apiClientRx.mappedResponse(argThat(requestMatcher), same(ApiSearchSuggestions.class)))
                .thenReturn(Observable.just(suggestions));
        return suggestions;
    }

    @NonNull
    private Autocompletion setupAutocompletionRemoteSuggestions() {
        final Autocompletion autocompletion = Autocompletion.create("query", "output");
        final ModelCollection<Autocompletion> autocompletions = new ModelCollection<>(Lists.newArrayList(autocompletion),
                                                                                      Maps.<String, Link>newHashMap(),
                                                                                      QUERY_URN.toString());
        final ApiRequestTo requestMatcher = isApiRequestTo("GET", ApiEndpoints.SEARCH_AUTOCOMPLETE.path())
                .withQueryParam("query", SEARCH_QUERY)
                .withQueryParam("limit", String.valueOf(MAX_RESULTS_NUMBER));

        when(apiClientRx.mappedResponse(argThat(requestMatcher), Matchers.<TypeToken<ModelCollection<Autocompletion>>>any()))
                .thenReturn(Observable.just(autocompletions));


        return autocompletion;
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
                Collections.<Map<String, Integer>>emptyList(),
                track,
                user);
    }
}
