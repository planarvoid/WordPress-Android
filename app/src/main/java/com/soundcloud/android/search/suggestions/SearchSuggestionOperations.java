package com.soundcloud.android.search.suggestions;

import static com.soundcloud.java.collections.Iterables.concat;
import static com.soundcloud.java.collections.Iterables.transform;
import static com.soundcloud.java.collections.Lists.newArrayList;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.RecordHolder;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.profile.WriteMixedRecordsCommand;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class SearchSuggestionOperations {

    private static final int MAX_SUGGESTIONS_NUMBER = 5;

    private static final Func1<ApiSearchSuggestions, List<SuggestionItem>> REMOTE_TO_SUGGESTION_ITEM =
            new Func1<ApiSearchSuggestions, List<SuggestionItem>>() {
                @Override
                public List<SuggestionItem> call(ApiSearchSuggestions apiSearchSuggestions) {
                    final List<SuggestionItem> result = new ArrayList<>(apiSearchSuggestions.getCollection().size());
                    for (ApiSearchSuggestion input : apiSearchSuggestions.getCollection()) {
                        if (input.getTrack().isPresent()) {
                            result.add(SuggestionItem.forTrack(input.toPropertySet(), input.getQuery()));
                        } else if (input.getUser().isPresent()) {
                            result.add(SuggestionItem.forUser(input.toPropertySet(), input.getQuery()));
                        }
                    }
                    return result;
                }
            };


    private final ApiClientRx apiClientRx;
    private final WriteMixedRecordsCommand writeMixedRecordsCommand;
    private final Scheduler scheduler;

    private final SearchSuggestionStorage suggestionStorage;
    private final FeatureFlags featureFlags;

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

    private final TypeToken<ModelCollection<Autocompletion>> autocompletionTypeToken = new TypeToken<ModelCollection<Autocompletion>>() {
    };
    private final Func2<List<SuggestionItem>, List<SuggestionItem>, List<SuggestionItem>> ACCUMULATE_RESULTS = new Func2<List<SuggestionItem>, List<SuggestionItem>, List<SuggestionItem>>() {
        @Override
        public List<SuggestionItem> call(List<SuggestionItem> first, List<SuggestionItem> second) {
            return newArrayList(concat(first, second));
        }
    };

    @Inject
    SearchSuggestionOperations(ApiClientRx apiClientRx,
                               WriteMixedRecordsCommand writeMixedRecordsCommand,
                               @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler,
                               SearchSuggestionStorage suggestionStorage,
                               FeatureFlags featureFlags) {
        this.apiClientRx = apiClientRx;
        this.writeMixedRecordsCommand = writeMixedRecordsCommand;
        this.scheduler = scheduler;
        this.suggestionStorage = suggestionStorage;
        this.featureFlags = featureFlags;
    }

    Observable<List<SuggestionItem>> suggestionsFor(String query) {
        return Observable.concatEager(localSuggestions(query), remoteSuggestions(query))
                         .filter(RxUtils.IS_NOT_EMPTY_LIST)
                         .scan(ACCUMULATE_RESULTS);
    }

    private Observable<List<SuggestionItem>> localSuggestions(String query) {
        if (featureFlags.isEnabled(Flag.AUTOCOMPLETE)) {
            return localCollectionSuggestions(query).concatWith(defaultSearchItem(query));
        } else {
            return defaultSearchItem(query).concatWith(localCollectionSuggestions(query));
        }

    }

    private Observable<List<SuggestionItem>> defaultSearchItem(String query) {
        if (featureFlags.isEnabled(Flag.AUTOCOMPLETE)) {
            return Observable.just(Collections.singletonList(SuggestionItem.forSearch(query)));
        } else {
            return Observable.just(Collections.singletonList(SuggestionItem.forLegacySearch(query)));
        }
    }

    private Observable<List<SuggestionItem>> localCollectionSuggestions(String query) {
        return suggestionStorage.getSuggestions(query, MAX_SUGGESTIONS_NUMBER)
                                .map(localToSuggestionResult(query))
                                .onErrorResumeNext(Observable.<List<SuggestionItem>>empty())
                                .filter(RxUtils.IS_NOT_EMPTY_LIST)
                                .subscribeOn(scheduler);
    }

    private static Func1<? super List<PropertySet>, List<SuggestionItem>> localToSuggestionResult(final String searchQuery) {
        return new Func1<List<PropertySet>, List<SuggestionItem>>() {
            @Override
            public List<SuggestionItem> call(List<PropertySet> propertySets) {
                final List<SuggestionItem> result = new ArrayList<>(propertySets.size());
                for (PropertySet propertySet : propertySets) {
                    final Urn urn = propertySet.get(SearchSuggestionProperty.URN);
                    if (urn.isTrack()) {
                        result.add(SuggestionItem.forTrack(propertySet, searchQuery));
                    } else if (urn.isUser()) {
                        result.add(SuggestionItem.forUser(propertySet, searchQuery));
                    } else if (urn.isPlaylist()) {
                        result.add(SuggestionItem.forPlaylist(propertySet, searchQuery));
                    }
                }
                return result;
            }
        };
    }

    private Observable<List<SuggestionItem>> remoteSuggestions(String query) {
        if (featureFlags.isEnabled(Flag.AUTOCOMPLETE)) {
            return getAutocompletions(query);
        } else {
            return getLegacySuggestions(query);
        }
    }

    private Observable<List<SuggestionItem>> getLegacySuggestions(String query) {
        final ApiRequest request =
                ApiRequest.get(ApiEndpoints.SEARCH_SUGGESTIONS.path())
                          .addQueryParam("q", query)
                          .addQueryParam("limit", MAX_SUGGESTIONS_NUMBER)
                          .forPrivateApi()
                          .build();

        return apiClientRx.mappedResponse(request, ApiSearchSuggestions.class)
                          .doOnNext(writeDependencies)
                          .map(REMOTE_TO_SUGGESTION_ITEM)
                          .onErrorResumeNext(Observable.<List<SuggestionItem>>empty())
                          .filter(RxUtils.IS_NOT_EMPTY_LIST)
                          .subscribeOn(scheduler);
    }

    private Observable<List<SuggestionItem>> getAutocompletions(final String query) {
        final ApiRequest request =
                ApiRequest.get(ApiEndpoints.SEARCH_AUTOCOMPLETE.path())
                          .addQueryParam("query", query)
                          .addQueryParam("limit", MAX_SUGGESTIONS_NUMBER)
                          .forPrivateApi()
                          .build();

        return apiClientRx.mappedResponse(request, autocompletionTypeToken)
                          .map(collectionToSuggestionItem(query))
                          .onErrorResumeNext(Observable.<List<SuggestionItem>>empty())
                          .filter(RxUtils.IS_NOT_EMPTY_LIST)
                          .subscribeOn(scheduler);
    }

    private Func1<ModelCollection<Autocompletion>, List<SuggestionItem>> collectionToSuggestionItem(final String query) {
        return new Func1<ModelCollection<Autocompletion>, List<SuggestionItem>>() {
            @Override
            public List<SuggestionItem> call(ModelCollection<Autocompletion> autocompletions) {
                return newArrayList(transform(autocompletions.getCollection(),
                                              autocompletionToSuggestionItem(query, autocompletions.getQueryUrn())));
            }
        };
    }

    private static Function<? super Autocompletion, SuggestionItem> autocompletionToSuggestionItem(final String query, final Optional<Urn> queryUrn) {
        return new Function<Autocompletion, SuggestionItem>() {
            public SuggestionItem apply(Autocompletion input) {
                return SuggestionItem.forAutocompletion(input, query, queryUrn.get().toString());
            }
        };
    }
}
