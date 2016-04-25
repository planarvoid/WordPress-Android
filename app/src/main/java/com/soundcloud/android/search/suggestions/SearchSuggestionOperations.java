package com.soundcloud.android.search.suggestions;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.RecordHolder;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.profile.WriteMixedRecordsCommand;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

class SearchSuggestionOperations {

    private final ApiClientRx apiClientRx;
    private final WriteMixedRecordsCommand writeMixedRecordsCommand;
    private final Scheduler scheduler;

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
                               @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.apiClientRx = apiClientRx;
        this.writeMixedRecordsCommand = writeMixedRecordsCommand;
        this.scheduler = scheduler;
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
        //TODO: return localSuggestions(query).concatWith(remoteSuggestions(query));
        final List<PropertySet> propertySets = new ArrayList<>();
        propertySets.add(PropertySet.create().put(EntityProperty.URN, Urn.forTrack(123L)));
        return Observable.just(SuggestionsResult.fromPropertySets(propertySets, Urn.forTrack(1234L)));
    }

    private Observable<SuggestionsResult> localSuggestions(String query) {
        //TODO: get local suggestions
        return Observable.empty();
    }

    private Observable<SuggestionsResult> remoteSuggestions(String query) {
        //TODO: get remote suggestions
        return Observable.empty();
    }
}
