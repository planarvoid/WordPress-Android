package com.soundcloud.android.search.suggestions;

import static com.soundcloud.android.search.suggestions.SuggestionItem.fromPropertySet;
import static com.soundcloud.java.collections.Iterables.concat;
import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.configuration.experiments.AutocompleteConfig;
import com.soundcloud.android.model.RecordHolder;
import com.soundcloud.android.profile.WriteMixedRecordsCommand;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class SearchSuggestionOperations {
    private static final int MAX_SUGGESTIONS_NUMBER = 9;

    private final ApiClientRx apiClientRx;
    private final WriteMixedRecordsCommand writeMixedRecordsCommand;
    private final Scheduler scheduler;
    private final SearchSuggestionStorage suggestionStorage;
    private final AutocompleteConfig autocompleteConfig;
    private final SearchSuggestionFiltering searchSuggestionFiltering;
    private final TypeToken<ModelCollection<Autocompletion>> autocompletionTypeToken = new TypeToken<ModelCollection<Autocompletion>>() {
    };

    @Inject
    SearchSuggestionOperations(ApiClientRx apiClientRx,
                               WriteMixedRecordsCommand writeMixedRecordsCommand,
                               @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                               SearchSuggestionStorage suggestionStorage,
                               AutocompleteConfig autocompleteConfig,
                               SearchSuggestionFiltering searchSuggestionFiltering) {
        this.apiClientRx = apiClientRx;
        this.writeMixedRecordsCommand = writeMixedRecordsCommand;
        this.scheduler = scheduler;
        this.suggestionStorage = suggestionStorage;
        this.autocompleteConfig = autocompleteConfig;
        this.searchSuggestionFiltering = searchSuggestionFiltering;
    }

    Observable<List<SuggestionItem>> suggestionsFor(String query) {
        return Observable.concatEager(localSuggestions(query), remoteSuggestions(query))
                         .scan((first, second) -> newArrayList(concat(first, second)));
    }

    private Observable<List<SuggestionItem>> localSuggestions(String query) {
        if (autocompleteConfig.isEnabled()) {
            return localCollectionSuggestions(query);
        } else {
            return legacySearchItem(query).concatWith(localCollectionSuggestions(query));
        }

    }

    private Observable<List<SuggestionItem>> remoteSuggestions(String query) {
        if (autocompleteConfig.isEnabled()) {
            return getAutocompletions(query);
        } else {
            return getLegacySuggestions(query);
        }
    }

    private Observable<List<SuggestionItem>> legacySearchItem(String query) {
        return Observable.just(Collections.singletonList(SuggestionItem.forLegacySearch(query)));
    }

    private Observable<List<SuggestionItem>> localCollectionSuggestions(final String query) {
        return suggestionStorage.getSuggestions(query, MAX_SUGGESTIONS_NUMBER)
                                .flatMap(Observable::from)
                                .map(propertySet -> fromPropertySet(propertySet, query))
                                .toList()
                                .map(searchSuggestionFiltering::filtered)
                                .filter(list -> !list.isEmpty())
                                .onErrorResumeNext(Observable.empty())
                                .subscribeOn(scheduler);
    }

    private Observable<List<SuggestionItem>> getLegacySuggestions(String query) {
        final ApiRequest request =
                ApiRequest.get(ApiEndpoints.SEARCH_SUGGESTIONS.path())
                          .addQueryParam("q", query)
                          .addQueryParam("limit", MAX_SUGGESTIONS_NUMBER)
                          .forPrivateApi()
                          .build();

        return apiClientRx.mappedResponse(request, ApiSearchSuggestions.class)
                          .doOnNext(this::writeDependencies)
                          .flatMap(searchSuggestions -> Observable.from(searchSuggestions.getCollection()))
                          .map(searchSuggestion -> fromPropertySet(searchSuggestion.toPropertySet(), query))
                          .toList()
                          .filter(list -> !list.isEmpty())
                          .onErrorResumeNext(Observable.empty())
                          .subscribeOn(scheduler);
    }

    private Observable<List<SuggestionItem>> getAutocompletions(String query) {
        final ApiRequest request =
                ApiRequest.get(ApiEndpoints.SEARCH_AUTOCOMPLETE.path())
                          .addQueryParam("query", query)
                          .addQueryParam("limit", MAX_SUGGESTIONS_NUMBER)
                          .forPrivateApi()
                          .build();

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

    private void writeDependencies(ApiSearchSuggestions apiSearchSuggestions) {
        List<RecordHolder> dependencies = new ArrayList<>(apiSearchSuggestions.getCollection().size());
        for (ApiSearchSuggestion suggestion : apiSearchSuggestions.getCollection()) {
            Optional<? extends RecordHolder> recordHolder = suggestion.getRecordHolder();
            if (recordHolder.isPresent()) {
                dependencies.add(recordHolder.get());
            }
        }
        if (!dependencies.isEmpty()) {
            writeMixedRecordsCommand.call(dependencies);
        }
    }
}
