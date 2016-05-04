package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.model.RecordHolder;
import com.soundcloud.android.profile.WriteMixedRecordsCommand;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

class SearchSuggestionOperations {

    private static final int MAX_SUGGESTIONS_NUMBER = 5;

    private static final Observable<SuggestionsResult> ON_ERROR_EMPTY_LOCAL_RESULT =
            Observable.just(SuggestionsResult.emptyLocal());

    private static final Observable<SuggestionsResult> ON_ERROR_EMPTY_REMOTE_RESULT =
            Observable.just(SuggestionsResult.emptyRemote());

    private static final Func1<ApiSearchSuggestions, SuggestionsResult> REMOTE_TO_SUGGESTION_RESULT =
            new Func1<ApiSearchSuggestions, SuggestionsResult>() {
                @Override
                public SuggestionsResult call(ApiSearchSuggestions apiSearchSuggestions) {
                    return SuggestionsResult.remoteFromPropertySetSource(apiSearchSuggestions.getCollection());
                }
            };

    private static final Func1<List<PropertySet>, SuggestionsResult> LOCAL_TO_SUGGESTION_RESULT =
            new Func1<List<PropertySet>, SuggestionsResult>() {
                @Override
                public SuggestionsResult call(List<PropertySet> propertySets) {
                    return SuggestionsResult.localFromPropertySets(propertySets);
                }
            };

    private final ApiClientRx apiClientRx;
    private final WriteMixedRecordsCommand writeMixedRecordsCommand;
    private final Scheduler scheduler;

    private final SearchSuggestionStorage suggestionStorage;

    private final Action1<ApiSearchSuggestions> writeDependencies = new Action1<ApiSearchSuggestions>() {
        @Override
        public void call(ApiSearchSuggestions apiSearchSuggestions) {
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
    };

    @Inject
    SearchSuggestionOperations(ApiClientRx apiClientRx,
                               WriteMixedRecordsCommand writeMixedRecordsCommand,
                               @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                               SearchSuggestionStorage suggestionStorage) {
        this.apiClientRx = apiClientRx;
        this.writeMixedRecordsCommand = writeMixedRecordsCommand;
        this.scheduler = scheduler;
        this.suggestionStorage = suggestionStorage;
    }

    Observable<ApiSearchSuggestions> searchSuggestions(String query) {
        final ApiRequest request =
                ApiRequest.get(ApiEndpoints.SEARCH_SUGGESTIONS.path())
                        .addQueryParam("q", query)
                        .addQueryParam("limit", LegacySuggestionsAdapter.MAX_REMOTE)
                        .forPrivateApi()
                        .build();
        return apiClientRx.mappedResponse(request, ApiSearchSuggestions.class)
                .doOnNext(writeDependencies)
                .subscribeOn(scheduler);
    }

    Observable<SuggestionsResult> suggestionsFor(String query) {
        return localSuggestions(query).concatWith(remoteSuggestions(query));
    }

    private Observable<SuggestionsResult> localSuggestions(String query) {
        return suggestionStorage.getSuggestions(query, MAX_SUGGESTIONS_NUMBER)
                .map(LOCAL_TO_SUGGESTION_RESULT)
                .onErrorResumeNext(ON_ERROR_EMPTY_LOCAL_RESULT)
                .subscribeOn(scheduler);
    }

    private Observable<SuggestionsResult> remoteSuggestions(String query) {
        final ApiRequest request =
                ApiRequest.get(ApiEndpoints.SEARCH_SUGGESTIONS.path())
                        .addQueryParam("q", query)
                        .addQueryParam("limit", MAX_SUGGESTIONS_NUMBER)
                        .forPrivateApi()
                        .build();

        return apiClientRx.mappedResponse(request, ApiSearchSuggestions.class)
                .doOnNext(writeDependencies)
                .map(REMOTE_TO_SUGGESTION_RESULT)
                .onErrorResumeNext(ON_ERROR_EMPTY_REMOTE_RESULT)
                .subscribeOn(scheduler);
    }
}
