package com.soundcloud.android.search.suggestions;

import static com.soundcloud.android.search.suggestions.SuggestionItem.fromSearchSuggestion;
import static com.soundcloud.java.collections.Iterables.concat;
import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiClientRxV2;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.configuration.experiments.LocalizedAutocompletionsExperiment;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.reflect.TypeToken;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

class SearchSuggestionOperations {
    private static final int MAX_SUGGESTIONS_NUMBER = 9;
    static final int API_FETCH_DELAY_MS = 300;

    private final ApiClientRxV2 apiClientRx;
    private final Scheduler scheduler;
    private final SearchSuggestionStorage suggestionStorage;
    private final AccountOperations accountOperations;
    private final SearchSuggestionFiltering searchSuggestionFiltering;
    private final LocalizedAutocompletionsExperiment localizedAutocompletionsExperiment;
    private final TypeToken<ModelCollection<Autocompletion>> autocompletionTypeToken = new TypeToken<ModelCollection<Autocompletion>>() {
    };

    @Inject
    SearchSuggestionOperations(ApiClientRxV2 apiClientRx,
                               @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler,
                               SearchSuggestionStorage suggestionStorage,
                               AccountOperations accountOperations,
                               SearchSuggestionFiltering searchSuggestionFiltering,
                               LocalizedAutocompletionsExperiment localizedAutocompletionsExperiment) {
        this.apiClientRx = apiClientRx;
        this.scheduler = scheduler;
        this.suggestionStorage = suggestionStorage;
        this.accountOperations = accountOperations;
        this.searchSuggestionFiltering = searchSuggestionFiltering;
        this.localizedAutocompletionsExperiment = localizedAutocompletionsExperiment;
    }

    Observable<List<SuggestionItem>> suggestionsFor(String query) {
        return Observable.concatArrayEager(localCollectionSuggestions(query).toObservable(),
                                           Single.just(query).delay(API_FETCH_DELAY_MS, TimeUnit.MILLISECONDS, scheduler).flatMap(this::getAutocompletions).toObservable())
                         .scan((first, second) -> newArrayList(concat(first, second)));
    }

    private Single<List<SuggestionItem>> localCollectionSuggestions(final String query) {
        return suggestionStorage.getSuggestions(query, accountOperations.getLoggedInUserUrn(), MAX_SUGGESTIONS_NUMBER)
                                .map(suggestions -> Lists.transform(suggestions, item -> fromSearchSuggestion(item, query)))
                                .map(searchSuggestionFiltering::filtered)
                                .onErrorResumeNext(Single.just(Collections.emptyList()))
                                .subscribeOn(scheduler);
    }

    private Single<List<SuggestionItem>> getAutocompletions(String query) {
        final ApiRequest.Builder builder = ApiRequest.get(ApiEndpoints.SEARCH_AUTOCOMPLETE)
                                                     .addQueryParam("query", query)
                                                     .addQueryParam("limit", MAX_SUGGESTIONS_NUMBER)
                                                     .forPrivateApi();
        localizedAutocompletionsExperiment.variantName().ifPresent(variantName -> builder.addQueryParam("variant", variantName));
        final ApiRequest request = builder.build();

        return apiClientRx.mappedResponse(request, autocompletionTypeToken)
                          .map(modelCollection -> Lists.transform(modelCollection.getCollection(), autocompletion -> SuggestionItem.forAutocompletion(
                                  autocompletion,
                                  query,
                                  modelCollection.getQueryUrn())))
                          .onErrorResumeNext(Single.just(Collections.emptyList()))
                          .subscribeOn(scheduler);
    }
}
