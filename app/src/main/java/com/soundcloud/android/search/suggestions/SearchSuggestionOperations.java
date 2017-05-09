package com.soundcloud.android.search.suggestions;

import static com.soundcloud.android.search.suggestions.SuggestionItem.fromSearchSuggestion;
import static com.soundcloud.java.collections.Iterables.concat;
import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.configuration.experiments.LocalizedAutocompletionsExperiment;
import com.soundcloud.java.reflect.TypeToken;
import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

class SearchSuggestionOperations {
    private static final int MAX_SUGGESTIONS_NUMBER = 9;

    private final ApiClientRx apiClientRx;
    private final Scheduler scheduler;
    private final SearchSuggestionStorage suggestionStorage;
    private final AccountOperations accountOperations;
    private final SearchSuggestionFiltering searchSuggestionFiltering;
    private final LocalizedAutocompletionsExperiment localizedAutocompletionsExperiment;
    private final TypeToken<ModelCollection<Autocompletion>> autocompletionTypeToken = new TypeToken<ModelCollection<Autocompletion>>() {
    };

    @Inject
    SearchSuggestionOperations(ApiClientRx apiClientRx,
                               @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                               SearchSuggestionStorage suggestionStorage,
                               AccountOperations accountOperations,
                               SearchSuggestionFiltering searchSuggestionFiltering, LocalizedAutocompletionsExperiment localizedAutocompletionsExperiment) {
        this.apiClientRx = apiClientRx;
        this.scheduler = scheduler;
        this.suggestionStorage = suggestionStorage;
        this.accountOperations = accountOperations;
        this.searchSuggestionFiltering = searchSuggestionFiltering;
        this.localizedAutocompletionsExperiment = localizedAutocompletionsExperiment;
    }

    Observable<List<SuggestionItem>> suggestionsFor(String query) {
        return Observable.concatEager(localCollectionSuggestions(query), getAutocompletions(query))
                         .scan((first, second) -> newArrayList(concat(first, second)));
    }

    private Observable<List<SuggestionItem>> localCollectionSuggestions(final String query) {
        return suggestionStorage.getSuggestions(query, accountOperations.getLoggedInUserUrn(), MAX_SUGGESTIONS_NUMBER)
                                .flatMap(Observable::from)
                                .map(propertySet -> fromSearchSuggestion(propertySet, query))
                                .toList()
                                .map(searchSuggestionFiltering::filtered)
                                .filter(list -> !list.isEmpty())
                                .onErrorResumeNext(Observable.empty())
                                .subscribeOn(scheduler);
    }

    private Observable<List<SuggestionItem>> getAutocompletions(String query) {
        final ApiRequest.Builder builder = ApiRequest.get(ApiEndpoints.SEARCH_AUTOCOMPLETE.path())
                                                     .addQueryParam("query", query)
                                                     .addQueryParam("limit", MAX_SUGGESTIONS_NUMBER)
                                                     .forPrivateApi();
        localizedAutocompletionsExperiment.variantName().ifPresent(variantName -> builder.addQueryParam("variant", variantName));
        final ApiRequest request = builder.build();

        return apiClientRx.mappedResponse(request, autocompletionTypeToken)
                          .flatMap(modelCollection -> Observable.from(modelCollection.getCollection())
                                                                .map(autocompletion -> SuggestionItem.forAutocompletion(
                                                                        autocompletion,
                                                                        query,
                                                                        modelCollection.getQueryUrn())))
                          .toList()
                          .filter(list -> !list.isEmpty())
                          .onErrorResumeNext(Observable.empty())
                          .subscribeOn(scheduler);
    }
}
