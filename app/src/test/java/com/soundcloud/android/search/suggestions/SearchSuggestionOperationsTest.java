package com.soundcloud.android.search.suggestions;

import static com.soundcloud.android.search.suggestions.SuggestionItem.forAutocompletion;
import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiClientRxV2;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.configuration.experiments.LocalizedAutocompletionsExperiment;
import com.soundcloud.android.model.RecordHolder;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.matchers.ApiRequestTo;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.List;

public class SearchSuggestionOperationsTest extends AndroidUnitTest {

    private static final String SEARCH_QUERY = "query";
    private static final int MAX_RESULTS_NUMBER = 9;
    private static final Urn QUERY_URN = new Urn("soundcloud:autocomplete:123");
    private static final Urn USER_URN = Urn.forUser(123);

    @Mock private ApiClientRxV2 apiClientRx;
    @Mock private SearchSuggestionStorage suggestionStorage;
    @Mock private SearchSuggestionFiltering searchSuggestionFiltering;
    @Mock private AccountOperations accountOperations;
    @Mock private LocalizedAutocompletionsExperiment localizedAutocompletionsExperiment;
    @Captor private ArgumentCaptor<Iterable<RecordHolder>> recordIterableCaptor;
    @Captor private ArgumentCaptor<List<SuggestionItem>> suggestionItemsCaptor;

    private SearchSuggestionOperations operations;

    private final TypeToken<ModelCollection<Autocompletion>> autocompletionTypeToken = new TypeToken<ModelCollection<Autocompletion>>() {
    };

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(searchSuggestionFiltering.filtered(anyListOf(SuggestionItem.class))).thenAnswer(invocation -> invocation.getArguments()[0]);
        operations = new SearchSuggestionOperations(apiClientRx, Schedulers.trampoline(), suggestionStorage, accountOperations, searchSuggestionFiltering, localizedAutocompletionsExperiment);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(USER_URN);
    }

    @Test
    public void returnsSuggestionsAndSendVariantNameWithExperimentEnabled() {
        List<SearchSuggestion> localSuggestions = getLocalSuggestions();
        when(suggestionStorage.getSuggestions(SEARCH_QUERY, USER_URN, MAX_RESULTS_NUMBER)).thenReturn(Single.just(localSuggestions));
        final Optional<String> variant = Optional.of("variant123");
        when(localizedAutocompletionsExperiment.variantName()).thenReturn(variant);

        final Autocompletion autocompletion = Autocompletion.create("query", "output");
        final ApiRequestTo requestMatcher = setupAutocompletionRemoteSuggestions(autocompletion);
        requestMatcher.withQueryParam("variant", variant.get());

        final TestObserver<List<SuggestionItem>> suggestionsResultSubscriber = operations.suggestionsFor(SEARCH_QUERY).test();

        verify(apiClientRx).mappedResponse(argThat(requestMatcher), eq(autocompletionTypeToken));

        final SuggestionItem localItem = SuggestionItem.fromSearchSuggestion(localSuggestions.get(0), SEARCH_QUERY);
        final SuggestionItem autocompletionItem = forAutocompletion(autocompletion, SEARCH_QUERY, Optional.of(QUERY_URN));

        final List<SuggestionItem> firstItem = newArrayList(localItem);
        final List<SuggestionItem> allItems = newArrayList(localItem,
                                                           autocompletionItem);


        final List<List<SuggestionItem>> onNextEvents = suggestionsResultSubscriber.values();
        assertThat(onNextEvents.get(0)).isEqualTo(firstItem);
        assertThat(onNextEvents.get(1)).isEqualTo(allItems);
    }

    @Test
    public void doesNotSendAuthorizationHeaderWithAutocompleteRequest() {
        List<SearchSuggestion> localSuggestions = getLocalSuggestions();
        when(suggestionStorage.getSuggestions(SEARCH_QUERY, USER_URN, MAX_RESULTS_NUMBER)).thenReturn(Single.just(localSuggestions));
        final Optional<String> variant = Optional.of("variant123");
        when(localizedAutocompletionsExperiment.variantName()).thenReturn(variant);

        final Autocompletion autocompletion = Autocompletion.create("query", "output");
        final ApiRequestTo requestMatcher = setupAutocompletionRemoteSuggestions(autocompletion);
        requestMatcher.withAnonymity();

        operations.suggestionsFor(SEARCH_QUERY).test();

        verify(apiClientRx).mappedResponse(argThat(requestMatcher), eq(autocompletionTypeToken));
    }

    @Test
    public void doNotSendVariantWithExperimentDisabled() {
        List<SearchSuggestion> localSuggestions = getLocalSuggestions();
        when(suggestionStorage.getSuggestions(SEARCH_QUERY, USER_URN, MAX_RESULTS_NUMBER)).thenReturn(Single.just(localSuggestions));
        final Optional<String> variant = Optional.absent();
        when(localizedAutocompletionsExperiment.variantName()).thenReturn(variant);

        final Autocompletion autocompletion = Autocompletion.create("query", "output");
        final ApiRequestTo requestMatcher = setupAutocompletionRemoteSuggestions(autocompletion);

        operations.suggestionsFor(SEARCH_QUERY).test();

        verify(apiClientRx).mappedResponse(argThat(requestMatcher), eq(autocompletionTypeToken));
    }

    @NonNull
    private List<SearchSuggestion> getLocalSuggestions() {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        return newArrayList(DatabaseSearchSuggestion.create(apiTrack.getUrn(), SEARCH_QUERY, apiTrack.getImageUrlTemplate()));
    }

    @NonNull
    private ApiRequestTo setupAutocompletionRemoteSuggestions(Autocompletion autocompletion) {
        final ModelCollection<Autocompletion> autocompletions = new ModelCollection<>(newArrayList(autocompletion),
                                                                                      new HashMap<>(),
                                                                                      QUERY_URN.toString());
        final ApiRequestTo requestMatcher = isApiRequestTo("GET", ApiEndpoints.SEARCH_AUTOCOMPLETE.path())
                .withQueryParam("query", SEARCH_QUERY)
                .withQueryParam("limit", String.valueOf(MAX_RESULTS_NUMBER))
                .withAnonymity();

        when(apiClientRx.mappedResponse(argThat(requestMatcher), eq(autocompletionTypeToken)))
                .thenReturn(Single.just(autocompletions));

        return requestMatcher;
    }
}
