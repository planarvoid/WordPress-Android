package com.soundcloud.android.search.topresults;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRxV2;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.search.ApiUniversalSearchItem;
import com.soundcloud.android.search.CacheUniversalSearchCommand;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

public class TopResultsOperationsTest extends AndroidUnitTest {


    private static final String QUERY = "query";
    private static final Urn QUERY_URN = new Urn("soundcloud:query_urn:123");
    private static final SearchParams SEARCH_PARAMS = SearchParams.create(QUERY, QUERY, Optional.of(QUERY_URN), Optional.of(2));
    private static final Urn TRACKS_BUCKET_URN = new Urn("soundcloud:search-buckets:freetiertracks");
    private static final Urn UNKNOWN_BUCKET_URN = new Urn("soundcloud:search-buckets:unknown");
    private static final ApiUniversalSearchItem UNIVERSAL_TRACK_ITEM = new ApiUniversalSearchItem(null, null, ModelFixtures.create(ApiTrack.class));
    private static final TypeToken<ApiTopResults> TYPE_TOKEN = new TypeToken<ApiTopResults>() {
    };

    @Mock private CacheUniversalSearchCommand cacheUniversalSearchCommand;
    @Mock private ApiClientRxV2 apiClientRx;
    @Captor private ArgumentCaptor<ApiRequest> argumentCaptor;

    private TopResultsOperations topResultsOperations;
    private Scheduler scheduler = Schedulers.trampoline();

    @Before
    public void setUp() throws Exception {
        topResultsOperations = new TopResultsOperations(apiClientRx,
                                                        scheduler,
                                                        cacheUniversalSearchCommand);
    }

    @Test
    public void performSearchAndCacheResults() throws Exception {
        final ArrayList<ApiUniversalSearchItem> apiUniversalSearchItems = newArrayList(UNIVERSAL_TRACK_ITEM);
        final ApiTopResultsBucket apiTopResultsBucket = ApiTopResultsBucket.create(TRACKS_BUCKET_URN, 5, new ModelCollection<>(apiUniversalSearchItems, Collections.emptyMap(), QUERY_URN));
        final ModelCollection<ApiTopResultsBucket> buckets = new ModelCollection<>(newArrayList(apiTopResultsBucket), Collections.emptyMap(), QUERY_URN);
        final ApiTopResults apiTopResults = ApiTopResults.create(10, buckets);
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(TYPE_TOKEN))).thenReturn(Single.just(apiTopResults));

        final TestObserver<ApiTopResults> subscriber = topResultsOperations.apiSearch(SEARCH_PARAMS).test();

        verify(cacheUniversalSearchCommand).call(apiUniversalSearchItems);
        subscriber.assertValueCount(1)
                  .assertValue(apiTopResults)
                  .assertComplete();
    }

    @Test
    public void filtersOutUnknownBucket() throws Exception {
        final ArrayList<ApiUniversalSearchItem> apiUniversalSearchItems = newArrayList(UNIVERSAL_TRACK_ITEM);
        final ApiTopResultsBucket apiTopResultsBucket = ApiTopResultsBucket.create(UNKNOWN_BUCKET_URN, 5, new ModelCollection<>(apiUniversalSearchItems, Collections.emptyMap(), QUERY_URN));
        final ModelCollection<ApiTopResultsBucket> buckets = new ModelCollection<>(newArrayList(apiTopResultsBucket), Collections.emptyMap(), QUERY_URN);
        final ApiTopResults apiTopResults = ApiTopResults.create(10, buckets);
        when(apiClientRx.mappedResponse(any(ApiRequest.class), eq(TYPE_TOKEN))).thenReturn(Single.just(apiTopResults));

        final TestObserver<ApiTopResults> subscriber = topResultsOperations.apiSearch(SEARCH_PARAMS).test();

        verify(cacheUniversalSearchCommand).call(apiUniversalSearchItems);
        subscriber.assertValueCount(1)
                  .assertValue(apiTopResults)
                  .assertComplete();
    }

    @Test
    public void callsCorrectApiEndpoint() throws Exception {
        when(apiClientRx.mappedResponse(any(ApiRequest.class), any(TypeToken.class))).thenReturn(Single.error(new IOException()));

        topResultsOperations.apiSearch(SEARCH_PARAMS).test();

        verify(apiClientRx).mappedResponse(argumentCaptor.capture(), eq(TYPE_TOKEN));
        final ApiRequest apiRequest = argumentCaptor.getValue();
        assertThat(apiRequest.isPrivate()).isTrue();
        assertThat(apiRequest.getUri().toString()).isEqualTo(ApiEndpoints.SEARCH_TOP_RESULTS.path());
        assertThat(apiRequest.getMethod()).isEqualTo("GET");
        assertThat(apiRequest.getQueryParameters().get(TopResultsOperations.QUERY_PARAM).iterator().next()).isEqualTo(QUERY);
        assertThat(apiRequest.getQueryParameters().get(ApiRequest.Param.PAGE_SIZE.toString()).iterator().next()).isEqualTo("2");
        assertThat(apiRequest.getQueryParameters().get(TopResultsOperations.QUERY_URN_PARAM).iterator().next()).isEqualTo(QUERY_URN.toString());
    }

    @Test
    public void doesNotCacheWhenEmptyResponse() throws Exception {
        IOException exception = new IOException();
        when(apiClientRx.mappedResponse(any(ApiRequest.class), any(TypeToken.class))).thenReturn(Single.error(exception));

        final TestObserver<ApiTopResults> subscriber = topResultsOperations.apiSearch(SEARCH_PARAMS).test();

        verify(cacheUniversalSearchCommand, never()).call(anyIterable());
        subscriber.assertError(exception);
    }
}
