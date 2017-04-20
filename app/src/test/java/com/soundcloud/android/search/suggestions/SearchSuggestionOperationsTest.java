package com.soundcloud.android.search.suggestions;

import static com.soundcloud.android.search.suggestions.SuggestionItem.forAutocompletion;
import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.RecordHolder;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.matchers.ApiRequestTo;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
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

import java.util.HashMap;
import java.util.List;

public class SearchSuggestionOperationsTest extends AndroidUnitTest {

    private static final String SEARCH_QUERY = "query";
    private static final int MAX_RESULTS_NUMBER = 9;
    private static final Urn QUERY_URN = new Urn("soundcloud:autocomplete:123");
    private static final Urn USER_URN = Urn.forUser(123);

    @Mock private ApiClientRx apiClientRx;
    @Mock private SearchSuggestionStorage suggestionStorage;
    @Mock private SearchSuggestionFiltering searchSuggestionFiltering;
    @Mock private AccountOperations accountOperations;
    @Captor private ArgumentCaptor<Iterable<RecordHolder>> recordIterableCaptor;
    @Captor private ArgumentCaptor<List<SuggestionItem>> suggestionItemsCaptor;

    private SearchSuggestionOperations operations;
    private TestSubscriber<List<SuggestionItem>> suggestionsResultSubscriber;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        when(searchSuggestionFiltering.filtered(anyListOf(SuggestionItem.class))).thenAnswer(invocation -> invocation.getArguments()[0]);
        operations = new SearchSuggestionOperations(apiClientRx, Schedulers.immediate(), suggestionStorage, accountOperations, searchSuggestionFiltering);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(USER_URN);
        suggestionsResultSubscriber = new TestSubscriber<>();
    }

    @Test
    public void returnsCorrectOrderWithAutocompleteEnabled() {
        List<SearchSuggestion> localSuggestions = getLocalSuggestions();
        when(suggestionStorage.getSuggestions(SEARCH_QUERY, USER_URN, MAX_RESULTS_NUMBER)).thenReturn(Observable.just(localSuggestions));

        final Autocompletion autocompletion = setupAutocompletionRemoteSuggestions();

        operations.suggestionsFor(SEARCH_QUERY).subscribe(suggestionsResultSubscriber);

        final SuggestionItem localItem = SuggestionItem.fromSearchSuggestion(localSuggestions.get(0), SEARCH_QUERY);
        final SuggestionItem autocompletionItem = forAutocompletion(autocompletion, SEARCH_QUERY, Optional.of(QUERY_URN));

        final List<SuggestionItem> firstItem = newArrayList(localItem);
        final List<SuggestionItem> allItems = newArrayList(localItem,
                                                           autocompletionItem);


        final List<List<SuggestionItem>> onNextEvents = suggestionsResultSubscriber.getOnNextEvents();
        assertThat(onNextEvents.get(0)).isEqualTo(firstItem);
        assertThat(onNextEvents.get(1)).isEqualTo(allItems);
    }

    @NonNull
    private List<SearchSuggestion> getLocalSuggestions() {
        final ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        return newArrayList(DatabaseSearchSuggestion.create(apiTrack.getUrn(), SEARCH_QUERY, apiTrack.getImageUrlTemplate()));
    }

    @NonNull
    private Autocompletion setupAutocompletionRemoteSuggestions() {
        final Autocompletion autocompletion = Autocompletion.create("query", "output");
        final ModelCollection<Autocompletion> autocompletions = new ModelCollection<>(newArrayList(autocompletion),
                                                                                      new HashMap<>(),
                                                                                      QUERY_URN.toString());
        final ApiRequestTo requestMatcher = isApiRequestTo("GET", ApiEndpoints.SEARCH_AUTOCOMPLETE.path())
                .withQueryParam("query", SEARCH_QUERY)
                .withQueryParam("limit", String.valueOf(MAX_RESULTS_NUMBER));

        when(apiClientRx.mappedResponse(argThat(requestMatcher), Matchers.<TypeToken<ModelCollection<Autocompletion>>>any()))
                .thenReturn(Observable.just(autocompletions));


        return autocompletion;
    }
}
