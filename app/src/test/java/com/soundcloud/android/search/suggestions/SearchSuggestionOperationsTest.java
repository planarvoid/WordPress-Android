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
import java.util.Map;

public class SearchSuggestionOperationsTest extends AndroidUnitTest{

    SearchSuggestionOperations operations;

    @Mock private ApiClientRx apiClientRx;
    @Mock private WriteMixedRecordsCommand writeMixedRecordsCommand;
    @Captor private ArgumentCaptor<Iterable<RecordHolder>> recordIterableCaptor;

    private TestSubscriber<ApiSearchSuggestions> subscriber;
    private ApiTrack track;
    private ApiUser user;

    @Before
    public void setUp() throws Exception {
        operations = new SearchSuggestionOperations(apiClientRx, writeMixedRecordsCommand, Schedulers.immediate());
        subscriber = new TestSubscriber<>();
        track = ModelFixtures.create(ApiTrack.class);
        user = ModelFixtures.create(ApiUser.class);
    }

    @Test
    public void returnsSuggestionsFromApi() {
        final ApiSearchSuggestions suggestions = setupSuggestionsFetch();

        operations.searchSuggestions("blah").subscribe(subscriber);

        subscriber.assertReceivedOnNext(Arrays.asList(suggestions));
    }

    @Test
    public void cachesDependenciesFromApi() {
        setupSuggestionsFetch();

        operations.searchSuggestions("blah").subscribe(subscriber);

        verify(writeMixedRecordsCommand).call(recordIterableCaptor.capture());

        assertThat(recordIterableCaptor.getValue()).containsExactly(track, user);
    }

    @NonNull
    private ApiSearchSuggestions setupSuggestionsFetch() {
        final ApiSearchSuggestions suggestions = getMixedSuggestions();
        final ApiRequestTo requestMatcher = isApiRequestTo("GET", ApiEndpoints.SEARCH_SUGGESTIONS.path())
                .withQueryParam("q", "blah")
                .withQueryParam("limit", String.valueOf(SuggestionsAdapter.MAX_REMOTE));

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

    private ApiSearchSuggestion getSuggestion(String query, ApiTrack track, ApiUser user) {
        return ApiSearchSuggestion.create(
                query,
                Collections.<Map<String,Integer>>emptyList(),
                track,
                user);
    }
}
